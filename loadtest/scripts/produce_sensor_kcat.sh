#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP=${BOOTSTRAP:-localhost:9092}
TOPIC=${TOPIC:-sensor-topic}
SESSIONS=${SESSIONS:-50}
INTERVAL_MS=${INTERVAL_MS:-500}

if ! command -v docker >/dev/null 2>&1; then
	echo "docker is required" >&2
	exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
	echo "docker compose is required" >&2
	exit 1
fi

session_ids=()
for i in $(seq 1 "$SESSIONS"); do
	session_ids+=("session-$i")
done

echo "Producing to $TOPIC @ $BOOTSTRAP"
echo "sessions=$SESSIONS interval_ms=$INTERVAL_MS (target ~= $(python3 - <<PY
sessions=int('$SESSIONS')
interval_ms=int('$INTERVAL_MS')
print(sessions * (1000.0/interval_ms))
PY
) msg/s)"
echo "Press Ctrl+C to stop."

producer_cmd=(docker compose -f docker-compose.loadtest.yml run --rm -T -e BOOTSTRAP="$BOOTSTRAP" -e TOPIC="$TOPIC" kcat)

coproc KCAT { "${producer_cmd[@]}"; }

cleanup() {
	if [ -n "${KCAT_PID:-}" ]; then
		kill "$KCAT_PID" >/dev/null 2>&1 || true
	fi
}
trap cleanup EXIT

while true; do
	start_ms=$(python3 - <<'PY'
import time
print(int(time.time()*1000))
PY
)

	for sid in "${session_ids[@]}"; do
		event_id=$(python3 - <<'PY'
import uuid
print(uuid.uuid4())
PY
)
		sent_at_ms=$(python3 - <<'PY'
import time
print(int(time.time()*1000))
PY
)
		sim_time=$(python3 - <<PY
print($sent_at_ms/1000.0)
PY
)

		json=$(cat <<EOF
{"time":$sim_time,"x":-5.405197,"y":-1.435728,"z":-0.073432,"steer":0.0,"wheel_degree":0,"handle_angle":0,"speed":-0.0,"front_dist":6.841736959814851,"left_dist":6.8549553028926455,"right_dist":6.819816073111288,"rear_dist":6.269557738692705,"event_id":"$event_id","sent_at_epoch_ms":$sent_at_ms}
EOF
)

		printf '%s:%s\n' "$sid" "$json" >&"${KCAT[1]}"
	done

	end_ms=$(python3 - <<'PY'
import time
print(int(time.time()*1000))
PY
)
	elapsed=$((end_ms - start_ms))
	if [ "$elapsed" -lt "$INTERVAL_MS" ]; then
		sleep_sec=$(python3 - <<PY
print(($INTERVAL_MS - $elapsed)/1000.0)
PY
)
		sleep "$sleep_sec"
	fi
done
