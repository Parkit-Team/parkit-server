#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:?KAFKA_BOOTSTRAP_SERVERS is required}"
SOURCE_CSV="${SOURCE_CSV:-${ROOT_DIR}/analysis-service/src/test/resources/data/step01.csv}"
CLEAN_CSV_PATH="${CLEAN_CSV_PATH:-/tmp/step01_clean.csv}"

cd "${ROOT_DIR}"

awk -F, 'NR==1 || ($8 != "nan" && $8 != "NaN" && $8 != "NAN")' \
	"${SOURCE_CSV}" > "${CLEAN_CSV_PATH}"

KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS}" \
KAFKA_TOPIC="${KAFKA_TOPIC:-sensor-topic}" \
SESSION_COUNT="${SESSION_COUNT:-100}" \
REPEAT_COUNT="${REPEAT_COUNT:-10}" \
EVENT_INTERVAL_MS="${EVENT_INTERVAL_MS:-20}" \
SESSION_PREFIX="${SESSION_PREFIX:-load-session}" \
CSV_PATH="${CLEAN_CSV_PATH}" \
	"${ROOT_DIR}/perf/analysis-service/publish-sensor-topic.sh"
