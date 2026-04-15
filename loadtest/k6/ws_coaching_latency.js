import ws from 'k6/ws';
import { Trend, Rate, Counter } from 'k6/metrics';
import { check, sleep } from 'k6';

const e2eLatencyMs = new Trend('e2e_latency_ms');
const messages = new Counter('coaching_messages');
const missingMetadata = new Rate('missing_latency_metadata');
const connectFailures = new Counter('stomp_connect_failures');

const WS_URL = __ENV.WS_URL || 'ws://localhost:8082/ws/parkit-raw';

function parseDurationMs(s) {
	const v = String(s).trim();
	const m = v.match(/^([0-9]+)(ms|s|m|h)$/);
	if (!m) return 120000;
	const n = parseInt(m[1], 10);
	const unit = m[2];
	if (unit === 'ms') return n;
	if (unit === 's') return n * 1000;
	if (unit === 'm') return n * 60 * 1000;
	if (unit === 'h') return n * 60 * 60 * 1000;
	return 120000;
}

const RUN_FOR_MS = parseDurationMs(__ENV.DURATION || '2m');

export const options = {
	vus: parseInt(__ENV.VUS || '50', 10),
	duration: __ENV.DURATION || '2m',
	thresholds: {
		e2e_latency_ms: ['p(95)<100'],
		stomp_connect_failures: ['count==0'],
	},
};

function stompFrame(command, headers, body) {
	let out = `${command}\n`;
	for (const [k, v] of Object.entries(headers || {})) {
		out += `${k}:${v}\n`;
	}
	out += '\n';
	if (body) out += body;
	return out + '\u0000';
}

function parseStompMessage(data) {
	// data: "MESSAGE\nheader:...\n\n{json}\u0000"
	const s = String(data);
	const sep = s.indexOf('\n\n');
	if (sep === -1) return null;
	const bodyWithNull = s.slice(sep + 2);
	const nullIdx = bodyWithNull.lastIndexOf('\u0000');
	const body = (nullIdx === -1 ? bodyWithNull : bodyWithNull.slice(0, nullIdx)).trim();
	return body;
}

export default function () {
	const res = ws.connect(WS_URL, {}, (socket) => {
		let connected = false;

		socket.on('open', () => {
			socket.send(
				stompFrame('CONNECT', {
					'accept-version': '1.2',
					'host': 'localhost',
					'heart-beat': '0,0',
				})
			);
		});

		socket.on('message', (data) => {
			const s = String(data);
			if (!connected) {
				if (s.startsWith('CONNECTED')) {
					connected = true;
					socket.send(
						stompFrame('SUBSCRIBE', {
							id: `sub-${__VU}`,
							destination: '/topic/coaching',
							ack: 'auto',
						})
					);
					return;
				}

				// Any ERROR frame here should fail the check
				if (s.startsWith('ERROR')) {
					connectFailures.add(1);
					socket.close();
				}
				return;
			}

			if (!s.startsWith('MESSAGE')) return;
			messages.add(1);

			const body = parseStompMessage(s);
			if (!body) {
				missingMetadata.add(1);
				return;
			}

			let payload;
			try {
				payload = JSON.parse(body);
			} catch (_) {
				missingMetadata.add(1);
				return;
			}

			const sentAt = payload.sensorSentAtEpochMs;
			if (typeof sentAt !== 'number') {
				missingMetadata.add(1);
				return;
			}

			missingMetadata.add(0);
			const latency = Date.now() - sentAt;
			e2eLatencyMs.add(latency);
		});

		socket.setTimeout(() => {
			if (!connected) connectFailures.add(1);
			socket.close();
		}, 10000);

		// keep the socket open for the iteration
		socket.setTimeout(() => socket.close(), RUN_FOR_MS);
	});

	check(res, {
		'ws connected': (r) => r && r.status === 101,
	});

	sleep(1);
}
