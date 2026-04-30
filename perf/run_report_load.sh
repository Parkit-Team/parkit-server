#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_BASE_URL="${REPORT_BASE_URL:?REPORT_BASE_URL is required}"

cd "${ROOT_DIR}"

k6 run \
	-e REPORT_BASE_URL="${REPORT_BASE_URL}" \
	-e SENSOR_LOGS_PER_ITERATION="${SENSOR_LOGS_PER_ITERATION:-50}" \
	-e SENSOR_LOG_PAUSE_MS="${SENSOR_LOG_PAUSE_MS:-0}" \
	-e REPORT_LIMIT="${REPORT_LIMIT:-2000}" \
	-e STAGE_1_DURATION="${STAGE_1_DURATION:-30s}" \
	-e STAGE_1_TARGET="${STAGE_1_TARGET:-10}" \
	-e STAGE_2_DURATION="${STAGE_2_DURATION:-1m}" \
	-e STAGE_2_TARGET="${STAGE_2_TARGET:-50}" \
	-e STAGE_3_DURATION="${STAGE_3_DURATION:-30s}" \
	perf/report-service/driving-session-load.js
