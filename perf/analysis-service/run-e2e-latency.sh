#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
K6_BIN="${K6_BIN:-k6}"
K6_SCRIPT="${K6_SCRIPT:-${ROOT_DIR}/perf/socket-service/stomp-broadcast-load.js}"
SUMMARY_EXPORT="${SUMMARY_EXPORT:-${ROOT_DIR}/perf/results/analysis-socket-summary.json}"
K6_STDOUT_LOG="${K6_STDOUT_LOG:-${ROOT_DIR}/perf/results/analysis-socket-k6.log}"
PUBLISH_SCRIPT="${PUBLISH_SCRIPT:-${ROOT_DIR}/perf/analysis-service/publish-sensor-topic.sh}"

mkdir -p "${ROOT_DIR}/perf/results"

"${K6_BIN}" run --summary-export "${SUMMARY_EXPORT}" "${K6_SCRIPT}" > "${K6_STDOUT_LOG}" 2>&1 &
K6_PID=$!

cleanup() {
	if kill -0 "${K6_PID}" >/dev/null 2>&1; then
		kill "${K6_PID}" >/dev/null 2>&1 || true
	fi
}

trap cleanup EXIT

sleep "${K6_WARMUP_SECONDS:-5}"
"${PUBLISH_SCRIPT}"

wait "${K6_PID}"
trap - EXIT

echo "k6 summary written to ${SUMMARY_EXPORT}"
echo "k6 log written to ${K6_STDOUT_LOG}"
