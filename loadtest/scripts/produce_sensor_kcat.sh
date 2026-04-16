#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP=${BOOTSTRAP:-}
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

if [ -z "$BOOTSTRAP" ]; then
	echo "Producing to $TOPIC (bootstrap=default from docker-compose)"
else
	echo "Producing to $TOPIC @ $BOOTSTRAP"
fi
echo "sessions=$SESSIONS interval_ms=$INTERVAL_MS"
echo "Press Ctrl+C to stop."

producer_cmd=(docker compose -f docker-compose.loadtest.yml run --rm -T -e BOOTSTRAP="$BOOTSTRAP" -e TOPIC="$TOPIC" kcat)

coproc KCAT { "${producer_cmd[@]}"; }

cleanup() {
	if [ -n "${KCAT_PID:-}" ]; then
		kill "$KCAT_PID" >/dev/null 2>&1 || true
	fi
}
trap cleanup EXIT

SESSIONS="$SESSIONS" INTERVAL_MS="$INTERVAL_MS" python3 loadtest/scripts/produce_sensor.py >&"${KCAT[1]}"
