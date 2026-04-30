import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const wsBaseUrl = __ENV.SOCKET_WS_URL || 'ws://localhost:8082/ws/parkit/websocket';
const subscribeDestination = __ENV.STOMP_SUBSCRIBE_DESTINATION || '/topic/coaching-mock';
const testDurationMs = Number(__ENV.TEST_DURATION_MS || 60000);

const connectFailures = new Counter('socket_connect_failures');
const stompFramesReceived = new Counter('socket_stomp_frames_received');
const wsDeliveryLatency = new Trend('socket_ws_delivery_latency_ms', true);
const validMessages = new Rate('socket_valid_messages');

export const options = {
	scenarios: {
		stomp_clients: {
			executor: 'ramping-vus',
			startVUs: 0,
			stages: [
				{ duration: __ENV.STAGE_1_DURATION || '30s', target: Number(__ENV.STAGE_1_TARGET || 20) },
				{ duration: __ENV.STAGE_2_DURATION || '1m', target: Number(__ENV.STAGE_2_TARGET || 100) },
				{ duration: __ENV.STAGE_3_DURATION || '30s', target: 0 },
			],
			gracefulRampDown: '10s',
			exec: 'subscribe',
		},
	},
	thresholds: {
		socket_connect_failures: ['count<1'],
		socket_valid_messages: ['rate>0.99'],
		socket_ws_delivery_latency_ms: ['p(95)<500'],
	},
};

function stompFrame(command, headers = {}, body = '') {
	const headerLines = Object.entries(headers)
		.map(([key, value]) => `${key}:${value}`)
		.join('\n');
	return `${command}\n${headerLines}\n\n${body}\u0000`;
}

function parseStompFrames(buffer) {
	return buffer
		.split('\u0000')
		.map((frame) => frame.trim())
		.filter((frame) => frame.length > 0);
}

function parseMessageBody(frame) {
	const divider = frame.indexOf('\n\n');
	if (divider < 0) {
		return null;
	}

	return frame.slice(divider + 2);
}

export function subscribe() {
	const response = ws.connect(wsBaseUrl, {}, function (socket) {
		let receivedConnectedFrame = false;

		socket.on('open', function () {
			socket.send(
				stompFrame('CONNECT', {
					'accept-version': '1.2,1.1,1.0',
					'heart-beat': '0,0',
				}),
			);
		});

		socket.on('message', function (message) {
			const frames = parseStompFrames(message);
			for (const frame of frames) {
				stompFramesReceived.add(1);
				if (frame.startsWith('CONNECTED')) {
					receivedConnectedFrame = true;
					socket.send(
						stompFrame('SUBSCRIBE', {
							id: `sub-${__VU}`,
							destination: subscribeDestination,
							ack: 'auto',
						}),
					);
					continue;
				}

				if (!frame.startsWith('MESSAGE')) {
					continue;
				}

				const body = parseMessageBody(frame);
				if (!body) {
					validMessages.add(false);
					continue;
				}

				const payload = JSON.parse(body);
				const now = Date.now();
				const messageTimestamp = Date.parse(payload.timestamp);

				if (Number.isFinite(messageTimestamp)) {
					wsDeliveryLatency.add(now - messageTimestamp);
					validMessages.add(true);
				} else {
					validMessages.add(false);
				}
			}
		});

		socket.on('error', function () {
			connectFailures.add(1);
		});

		socket.setTimeout(function () {
			socket.close();
		}, testDurationMs);

		socket.setInterval(function () {
			if (receivedConnectedFrame) {
				socket.send('\n');
			}
		}, 10000);
	});

	check(response, {
		'websocket upgrade succeeded': (res) => res && res.status === 101,
	});

	sleep(1);
}
