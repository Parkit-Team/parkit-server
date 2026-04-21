# Performance Test Guide

이 디렉터리는 `parkit-server`의 부하테스트 자산을 모아둔 곳입니다.

원칙
- 운영 클라이언트 DTO는 변경하지 않습니다.
- WebSocket 부하테스트는 기본적으로 `socket-service`의 mock destination(`/topic/coaching-mock`)을 사용합니다.
- 지연시간은 기존 공개 DTO의 `timestamp` 필드를 기준으로 근사 측정합니다.

테스트 대상
- `report-service`: HTTP API 지연시간과 에러율
- `socket-service`: STOMP/WebSocket 브로드캐스트 지연시간
- `analysis-service`: Kafka 센서 이벤트 발행 부하

사전 조건
- `k6` 설치
- `kcat` 설치
- 대상 서비스 기동
- `socket-service` mock 부하테스트 시 `parkit.mock.coaching.enabled=true`

## Report Service

실행 예시

```bash
REPORT_BASE_URL=http://localhost:8083 \
SENSOR_LOGS_PER_ITERATION=50 \
STAGE_1_TARGET=20 \
STAGE_2_TARGET=100 \
./perf/run_report_load.sh
```

주요 메트릭
- `http_req_duration`
- `report_sensor_log_duration`
- `report_fetch_duration`
- `http_req_failed`

## Socket Service

기본 대상
- SockJS endpoint: `/ws/parkit`
- 기본 subscribe destination: `/topic/coaching-mock`

실행 예시

```bash
SOCKET_HOST=localhost \
CLIENT_COUNT=100 \
DURATION_SECONDS=30 \
./perf/run_socket_load.sh
```

출력 예시

```text
clients=100 connected=100 failures=0 messages=31344
avg_ms=308.65 p50_ms=15 p95_ms=2175 p99_ms=3165 max_ms=3882
```

주의
- `timestamp` 기반 근사 지연시간이므로 테스트 서버와 대상 서버의 시계가 크게 어긋나면 수치가 왜곡될 수 있습니다.
- 운영 DTO 계약을 건드리지 않기 위해 mock channel 부하를 기본값으로 둡니다.

## Analysis Service Publish Load

센서 이벤트를 Kafka `sensor-topic`으로 발행합니다.

```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
SESSION_COUNT=100 \
REPEAT_COUNT=10 \
EVENT_INTERVAL_MS=20 \
./perf/run_analysis_publish.sh
```

이 스크립트는 실행 전에 CSV에서 `nan` 값을 제거한 정제본을 사용합니다.

## Optional E2E

운영 DTO 변경 없이 `timestamp`만으로 근사 지연을 보려면:

```bash
SOCKET_WS_URL=ws://localhost:8082/ws/parkit/websocket \
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
SESSION_COUNT=100 \
REPEAT_COUNT=10 \
STAGE_1_TARGET=20 \
STAGE_2_TARGET=100 \
./perf/run_analysis_e2e.sh
```

## Ubuntu Quick Start

```bash
sudo apt update
sudo apt install -y git curl jq openjdk-17-jdk kcat
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://dl.k6.io/key.gpg | sudo gpg --dearmor -o /etc/apt/keyrings/k6.gpg
echo "deb [signed-by=/etc/apt/keyrings/k6.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt update
sudo apt install -y k6
```

```bash
git clone https://github.com/Parkit-Team/parkit-server.git
cd parkit-server
chmod +x perf/*.sh perf/analysis-service/*.sh
```

## 결과 기록 형식

```text
대상: socket-service
시나리오: 동시 100 구독자, 30초 mock coaching 수신
결과: connected=100, failures=0, avg=xxx ms, p50=xxx ms, p95=xxx ms, p99=xxx ms, max=xxx ms
판정: 목표 p95 500ms 충족/미충족
```
