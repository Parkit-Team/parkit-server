#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOCKET_HOST="${SOCKET_HOST:?SOCKET_HOST is required}"
SOCKET_PORT="${SOCKET_PORT:-8082}"
STOMP_DESTINATION="${STOMP_DESTINATION:-/topic/coaching}"
CLIENT_COUNT="${CLIENT_COUNT:-20}"
DURATION_SECONDS="${DURATION_SECONDS:-30}"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
SOCKET_CP_FILE="${SOCKET_CP_FILE:-/tmp/socket.cp}"

export JAVA_HOME
export PATH="${JAVA_HOME}/bin:${PATH}"

cd "${ROOT_DIR}"

if [[ ! -f "${SOCKET_CP_FILE}" ]]; then
	cd socket-service
	bash ./gradlew compileKotlin --console=plain
	cd "${ROOT_DIR}"
	find ~/.gradle/caches/modules-2/files-2.1 -name '*.jar' | paste -sd: - > "${SOCKET_CP_FILE}"
fi

if [[ ! -f perf/socket-service/StompLoadClient.class ]]; then
	javac -cp "$(cat "${SOCKET_CP_FILE}")" perf/socket-service/StompLoadClient.java
fi

STOMP_ENDPOINT="http://${SOCKET_HOST}:${SOCKET_PORT}/ws/parkit" \
STOMP_DESTINATION="${STOMP_DESTINATION}" \
CLIENT_COUNT="${CLIENT_COUNT}" \
DURATION_SECONDS="${DURATION_SECONDS}" \
java -cp "perf/socket-service:$(cat "${SOCKET_CP_FILE}")" StompLoadClient
