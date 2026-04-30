import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

public class KafkaSensorProducer {
	public static void main(String[] args) throws Exception {
		String bootstrapServers = env("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
		String topic = env("KAFKA_TOPIC", "sensor-topic");
		Path csvPath = Path.of(env("CSV_PATH", "analysis-service/src/test/resources/data/step01.csv"));
		int sessionCount = Integer.parseInt(env("SESSION_COUNT", "50"));
		int repeatCount = Integer.parseInt(env("REPEAT_COUNT", "5"));
		long eventIntervalMs = Long.parseLong(env("EVENT_INTERVAL_MS", "0"));
		String sessionPrefix = env("SESSION_PREFIX", "load-session");

		Properties props = new Properties();
		props.put("bootstrap.servers", bootstrapServers);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("acks", "all");
		props.put("linger.ms", "0");

		try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
			for (int repeat = 1; repeat <= repeatCount; repeat++) {
				for (int session = 1; session <= sessionCount; session++) {
					String sessionId = sessionPrefix + "-" + repeat + "-" + session;
					publishCsv(csvPath, topic, sessionId, producer, eventIntervalMs);
				}
			}
			producer.flush();
		}

		System.out.printf(
			"Published sensor events to %s on %s with %d sessions x %d repeats%n",
			topic,
			bootstrapServers,
			sessionCount,
			repeatCount
		);
	}

	private static void publishCsv(
		Path csvPath,
		String topic,
		String sessionId,
		KafkaProducer<String, String> producer,
		long eventIntervalMs
	) throws IOException, InterruptedException {
		try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
			String line;
			boolean header = true;
			while ((line = reader.readLine()) != null) {
				if (header) {
					header = false;
					continue;
				}

				String[] tokens = line.split(",");
				if (tokens.length < 12) {
					continue;
				}

				String payload = String.format(
					"{\"time\":%s,\"x\":%s,\"y\":%s,\"z\":%s,\"steer\":%s,\"wheel_degree\":%s,\"handle_angle\":%s,\"speed\":%s,\"front_dist\":%s,\"left_dist\":%s,\"right_dist\":%s,\"rear_dist\":%s}",
					tokens[0].trim(),
					tokens[1].trim(),
					tokens[2].trim(),
					tokens[3].trim(),
					tokens[4].trim(),
					tokens[5].trim(),
					tokens[6].trim(),
					tokens[7].trim(),
					tokens[8].trim(),
					tokens[9].trim(),
					tokens[10].trim(),
					tokens[11].trim()
				);

				producer.send(new ProducerRecord<>(topic, sessionId, payload)).get();
				if (eventIntervalMs > 0) {
					Thread.sleep(eventIntervalMs);
				}
			}
		} catch (Exception e) {
			throw new IOException("Failed to publish CSV for session " + sessionId, e);
		}
	}

	private static String env(String key, String defaultValue) {
		String value = System.getenv(key);
		return value == null || value.isBlank() ? defaultValue : value;
	}
}
