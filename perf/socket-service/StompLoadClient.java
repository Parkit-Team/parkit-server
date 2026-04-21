import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class StompLoadClient {
	public static void main(String[] args) throws Exception {
		String endpoint = env("STOMP_ENDPOINT", "ws://127.0.0.1:8082/ws/parkit");
		String destination = env("STOMP_DESTINATION", "/topic/coaching-mock");
		int clientCount = Integer.parseInt(env("CLIENT_COUNT", "100"));
		int durationSeconds = Integer.parseInt(env("DURATION_SECONDS", "30"));
		int connectTimeoutSeconds = Integer.parseInt(env("CONNECT_TIMEOUT_SECONDS", "30"));

		List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
		SockJsClient sockJsClient = new SockJsClient(transports);
		WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
		stompClient.setMessageConverter(new MappingJackson2MessageConverter());

		List<StompSession> sessions = new ArrayList<>();
		List<Long> latencies = new CopyOnWriteArrayList<>();
		AtomicInteger messageCount = new AtomicInteger();
		AtomicInteger connectFailures = new AtomicInteger();
		CountDownLatch connected = new CountDownLatch(clientCount);
		List<CompletableFuture<StompSession>> futures = new ArrayList<>();

		for (int i = 0; i < clientCount; i++) {
			int clientIndex = i;
			CompletableFuture<StompSession> future = stompClient.connectAsync(endpoint, new StompSessionHandlerAdapter() {
				@Override
				public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
					session.subscribe(destination, new StompSessionHandlerAdapter() {
						@Override
						public Type getPayloadType(StompHeaders headers) {
							return Map.class;
						}

						@Override
						public void handleFrame(StompHeaders headers, Object payload) {
							if (!(payload instanceof Map<?, ?> body)) {
								return;
							}

							Long messageTimestamp = readTimestamp(body);
							if (messageTimestamp != null) {
								long latency = System.currentTimeMillis() - messageTimestamp;
								latencies.add(latency);
							}
							messageCount.incrementAndGet();
						}
					});
					connected.countDown();
				}

				@Override
				public void handleTransportError(StompSession session, Throwable exception) {
					connectFailures.incrementAndGet();
					connected.countDown();
				}

				@Override
				public void handleFrame(StompHeaders headers, Object payload) {
					// no-op
				}
			});
			futures.add(future.exceptionally(ex -> {
				System.err.printf("client-%d failed to connect: %s%n", clientIndex, ex.getMessage());
				connectFailures.incrementAndGet();
				connected.countDown();
				return null;
			}));
		}

		connected.await(connectTimeoutSeconds, TimeUnit.SECONDS);
		for (CompletableFuture<StompSession> future : futures) {
			StompSession session = future.getNow(null);
			if (session != null && session.isConnected()) {
				sessions.add(session);
			}
		}

		Thread.sleep(durationSeconds * 1000L);

		for (StompSession session : sessions) {
			try {
				session.disconnect();
			} catch (Exception ignored) {
			}
		}
		stompClient.stop();

		List<Long> sorted = new ArrayList<>(latencies);
		Collections.sort(sorted);

		System.out.printf("clients=%d connected=%d failures=%d messages=%d%n",
			clientCount, sessions.size(), connectFailures.get(), messageCount.get());
		if (sorted.isEmpty()) {
			System.out.println("No messages received.");
			return;
		}

		System.out.printf("avg_ms=%.2f p50_ms=%d p95_ms=%d p99_ms=%d max_ms=%d%n",
			sorted.stream().mapToLong(Long::longValue).average().orElse(0),
			percentile(sorted, 0.50),
			percentile(sorted, 0.95),
			percentile(sorted, 0.99),
			sorted.get(sorted.size() - 1));
	}

	private static long percentile(List<Long> values, double ratio) {
		int index = (int) Math.ceil(values.size() * ratio) - 1;
		index = Math.min(Math.max(index, 0), values.size() - 1);
		return values.get(index);
	}

	private static Long readTimestamp(Map<?, ?> body) {
		Object value = body.get("timestamp");
		if (!(value instanceof String timestamp) || timestamp.isBlank()) {
			return null;
		}
		try {
			return LocalDateTime.parse(timestamp)
				.atZone(ZoneId.systemDefault())
				.toInstant()
				.toEpochMilli();
		} catch (Exception ignored) {
			return null;
		}
	}

	private static String env(String key, String defaultValue) {
		String value = System.getenv(key);
		return value == null || value.isBlank() ? defaultValue : value;
	}
}
