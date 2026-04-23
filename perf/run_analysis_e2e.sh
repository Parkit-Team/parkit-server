#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOCKET_WS_URL="${SOCKET_WS_URL:?SOCKET_WS_URL is required}"
KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:?KAFKA_BOOTSTRAP_SERVERS is required}"

cd "${ROOT_DIR}"

mkdir -p perf/results

k6 run \
	--summary-export "${SUMMARY_EXPORT:-perf/results/e2e-summary.json}" \
	-e SOCKET_WS_URL="${SOCKET_WS_URL}" \
	-e STAGE_1_DURATION="${STAGE_1_DURATION:-10s}" \
	-e STAGE_1_TARGET="${STAGE_1_TARGET:-20}" \
	-e STAGE_2_DURATION="${STAGE_2_DURATION:-40s}" \
	-e STAGE_2_TARGET="${STAGE_2_TARGET:-100}" \
	-e STAGE_3_DURATION="${STAGE_3_DURATION:-10s}" \
	-e TEST_DURATION_MS="${TEST_DURATION_MS:-60000}" \
	perf/socket-service/stomp-broadcast-load.js &
K6_PID=$!

cleanup() {
	if kill -0 "${K6_PID}" >/dev/null 2>&1; then
		kill "${K6_PID}" >/dev/null 2>&1 || true
	fi
}

trap cleanup EXIT

sleep "${K6_WARMUP_SECONDS:-5}"

KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS}" \
KAFKA_TOPIC="${KAFKA_TOPIC:-sensor-topic}" \
SESSION_COUNT="${SESSION_COUNT:-100}" \
REPEAT_COUNT="${REPEAT_COUNT:-10}" \
EVENT_INTERVAL_MS="${EVENT_INTERVAL_MS:-20}" \
SESSION_PREFIX="${SESSION_PREFIX:-load-session}" \
SOURCE_CSV="${SOURCE_CSV:-${ROOT_DIR}/analysis-service/src/test/resources/data/step01.csv}" \
	"${ROOT_DIR}/perf/run_analysis_publish.sh"

wait "${K6_PID}"
trap - EXIT

echo "summary_export=${SUMMARY_EXPORT:-perf/results/e2e-summary.json}"
