#!/usr/bin/env bash

set -euo pipefail

KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"
KAFKA_TOPIC="${KAFKA_TOPIC:-sensor-topic}"
CSV_PATH="${CSV_PATH:-analysis-service/src/test/resources/data/step01.csv}"
SESSION_COUNT="${SESSION_COUNT:-50}"
REPEAT_COUNT="${REPEAT_COUNT:-5}"
EVENT_INTERVAL_MS="${EVENT_INTERVAL_MS:-0}"
SESSION_PREFIX="${SESSION_PREFIX:-load-session}"

if ! command -v kcat >/dev/null 2>&1; then
	echo "kcat is required but was not found in PATH." >&2
	exit 1
fi

if [[ ! -f "${CSV_PATH}" ]]; then
	echo "CSV_PATH does not exist: ${CSV_PATH}" >&2
	exit 1
fi

sleep_seconds() {
	awk "BEGIN { printf \"%.3f\", ${EVENT_INTERVAL_MS} / 1000 }"
}

publish_event() {
	local session_id="$1"
	local time="$2"
	local x="$3"
	local y="$4"
	local z="$5"
	local steer="$6"
	local wheel_degree="$7"
	local handle_angle="$8"
	local speed="$9"
	local front_dist="${10}"
	local left_dist="${11}"
	local right_dist="${12}"
	local rear_dist="${13}"

	local payload
	printf -v payload '{"time":%s,"x":%s,"y":%s,"z":%s,"steer":%s,"wheel_degree":%s,"handle_angle":%s,"speed":%s,"front_dist":%s,"left_dist":%s,"right_dist":%s,"rear_dist":%s}' \
		"${time}" "${x}" "${y}" "${z}" "${steer}" "${wheel_degree}" "${handle_angle}" "${speed}" \
		"${front_dist}" "${left_dist}" "${right_dist}" "${rear_dist}"

	printf '%s:%s\n' "${session_id}" "${payload}"
}

for repeat in $(seq 1 "${REPEAT_COUNT}"); do
	for session in $(seq 1 "${SESSION_COUNT}"); do
		session_id="${SESSION_PREFIX}-${repeat}-${session}"
		while IFS=, read -r time x y z steer wheel_degree handle_angle speed front_dist left_dist right_dist rear_dist; do
			if [[ "${time}" == "time" ]]; then
				continue
			fi

			publish_event \
				"${session_id}" "${time}" "${x}" "${y}" "${z}" "${steer}" "${wheel_degree}" \
				"${handle_angle}" "${speed}" "${front_dist}" "${left_dist}" "${right_dist}" "${rear_dist}"

			if [[ "${EVENT_INTERVAL_MS}" != "0" ]]; then
				sleep "$(sleep_seconds)"
			fi
		done < "${CSV_PATH}"
	done
done | kcat -P -b "${KAFKA_BOOTSTRAP_SERVERS}" -t "${KAFKA_TOPIC}" -K:

echo "Published sensor events to ${KAFKA_TOPIC} on ${KAFKA_BOOTSTRAP_SERVERS}"
