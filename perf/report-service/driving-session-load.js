import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const baseUrl = __ENV.REPORT_BASE_URL || 'http://localhost:8083';
const sensorLogsPerIteration = Number(__ENV.SENSOR_LOGS_PER_ITERATION || 20);
const sensorLogPauseMs = Number(__ENV.SENSOR_LOG_PAUSE_MS || 0);
const reportLimit = Number(__ENV.REPORT_LIMIT || 2000);

const sensorLogDuration = new Trend('report_sensor_log_duration', true);
const startDuration = new Trend('report_start_duration', true);
const stopDuration = new Trend('report_stop_duration', true);
const reportDuration = new Trend('report_fetch_duration', true);
const sensorLogFailures = new Counter('report_sensor_log_failures');

export const options = {
	scenarios: {
		driving_session_http: {
			executor: 'ramping-vus',
			startVUs: 0,
			stages: [
				{ duration: __ENV.STAGE_1_DURATION || '30s', target: Number(__ENV.STAGE_1_TARGET || 10) },
				{ duration: __ENV.STAGE_2_DURATION || '1m', target: Number(__ENV.STAGE_2_TARGET || 50) },
				{ duration: __ENV.STAGE_3_DURATION || '30s', target: 0 },
			],
			gracefulRampDown: '10s',
		},
	},
	thresholds: {
		http_req_failed: ['rate<0.01'],
		http_req_duration: ['p(95)<500'],
		report_sensor_log_duration: ['p(95)<400'],
		report_fetch_duration: ['p(95)<700'],
	},
};

function jsonParams(tags) {
	return {
		headers: {
			'Content-Type': 'application/json',
		},
		tags,
	};
}

function buildSensorPayload(sequence) {
	return JSON.stringify({
		time: sequence * 0.1,
		x: 12.0 + sequence * 0.05,
		y: -2.0 + sequence * 0.01,
		z: 0.0,
		steer: 0.0,
		wheel_degree: 0.0,
		handle_angle: (sequence % 10) * 3,
		speed: 1.5,
		front_dist: 2.4,
		left_dist: 1.2,
		right_dist: 1.3,
		rear_dist: 2.1,
	});
}

export default function () {
	const userId = `load-user-${__VU}-${__ITER}`;
	const startResponse = http.post(
		`${baseUrl}/api/driving-sessions/start`,
		JSON.stringify({ userId }),
		jsonParams({ endpoint: 'start-session' }),
	);
	startDuration.add(startResponse.timings.duration);
	check(startResponse, {
		'start session returns 201': (res) => res.status === 201,
	});

	const sessionId = startResponse.json('sessionId');

	for (let i = 0; i < sensorLogsPerIteration; i += 1) {
		const sensorResponse = http.post(
			`${baseUrl}/api/driving-sessions/${sessionId}/sensor-logs`,
			buildSensorPayload(i),
			jsonParams({ endpoint: 'sensor-log' }),
		);
		sensorLogDuration.add(sensorResponse.timings.duration);
		const ok = check(sensorResponse, {
			'sensor log returns 201': (res) => res.status === 201,
		});
		if (!ok) {
			sensorLogFailures.add(1);
		}

		if (sensorLogPauseMs > 0) {
			sleep(sensorLogPauseMs / 1000);
		}
	}

	const reportResponse = http.get(
		`${baseUrl}/api/driving-sessions/${sessionId}/report?limit=${reportLimit}`,
		{ tags: { endpoint: 'session-report' } },
	);
	reportDuration.add(reportResponse.timings.duration);
	check(reportResponse, {
		'report returns 200': (res) => res.status === 200,
	});

	const stopResponse = http.post(
		`${baseUrl}/api/driving-sessions/${sessionId}/stop`,
		JSON.stringify({ frontendScore: 87.5 }),
		jsonParams({ endpoint: 'stop-session' }),
	);
	stopDuration.add(stopResponse.timings.duration);
	check(stopResponse, {
		'stop session returns 200': (res) => res.status === 200,
	});
}
