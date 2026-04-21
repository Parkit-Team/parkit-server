# Performance Test Guide

이 디렉터리는 `parkit-server`의 세 서비스에 맞춘 부하테스트 자산을 모아둔 곳입니다.

테스트 대상
- `report-service`: HTTP API 지연시간과 에러율
- `socket-service`: STOMP/WebSocket 브로드캐스트 지연시간
- `analysis-service`: Kafka 입력부터 `socket-service` 브로드캐스트까지의 E2E 흐름

사전 조건
- `k6` 설치
- `kcat` 설치 (`analysis-service` E2E용)
- 대상 서비스 기동
- `analysis-service`와 `socket-service` E2E 측정 시 Kafka와 Redis도 함께 기동

권장 SLO 예시
- `report-service`: `p95 http_req_duration < 500ms`, `error rate < 1%`
- `socket-service`: `p95 socket_ws_delivery_latency_ms < 500ms`
- `analysis-service` + `socket-service`: `p95 analysis_processing_latency_ms < 200ms`, `p95 socket_ws_delivery_latency_ms < 500ms`

## Report Service

대상 엔드포인트
- `POST /api/driving-sessions/start`
- `POST /api/driving-sessions/{sessionId}/sensor-logs`
- `GET /api/driving-sessions/{sessionId}/report`
- `POST /api/driving-sessions/{sessionId}/stop`

실행 예시

```bash
k6 run \
  -e REPORT_BASE_URL=http://localhost:8083 \
  -e SENSOR_LOGS_PER_ITERATION=50 \
  -e STAGE_1_TARGET=20 \
  -e STAGE_2_TARGET=100 \
  perf/report-service/driving-session-load.js
```

측정 포인트
- `http_req_duration`
- `report_sensor_log_duration`
- `report_fetch_duration`
- `http_req_failed`

주의
- 현재 구현은 전역적으로 가장 최근 `RUNNING` 세션을 재사용합니다.
- 따라서 `start` 자체의 동시성 성능보다, 센서 로그 적재와 조회 부하를 보는 용도로 해석하는 것이 맞습니다.

## Socket Service

대상
- SockJS websocket endpoint: `/ws/parkit/websocket`
- STOMP subscribe destination: `/topic/coaching`

실행 예시

```bash
k6 run \
  -e SOCKET_WS_URL=ws://localhost:8082/ws/parkit/websocket \
  -e STAGE_1_TARGET=50 \
  -e STAGE_2_TARGET=300 \
  -e TEST_DURATION_MS=120000 \
  perf/socket-service/stomp-broadcast-load.js
```

측정 포인트
- `analysis_processing_latency_ms`
- `socket_broker_latency_ms`
- `socket_ws_delivery_latency_ms`
- `socket_valid_messages`

해석
- `analysis_processing_latency_ms`: `analysis-service`가 Kafka 레코드를 읽고 코칭 이벤트를 만든 시간
- `socket_broker_latency_ms`: `analysis-service`가 Kafka에 보낸 뒤 `socket-service`가 브로드캐스트 직전까지 걸린 시간
- `socket_ws_delivery_latency_ms`: `socket-service` 브로드캐스트 후 k6 클라이언트가 메시지를 받은 시간

## Analysis Service E2E

대상
- Kafka topic `sensor-topic`
- `analysis-service` consumer
- `socket-service` websocket broadcast

실행 순서
1. `analysis-service`, `socket-service`, Kafka, Redis를 기동합니다.
2. WebSocket 구독 부하를 시작합니다.
3. Kafka에 센서 이벤트를 발행합니다.
4. `k6` 결과에서 `analysis_processing_latency_ms`, `socket_ws_delivery_latency_ms`를 확인합니다.

원샷 실행 예시

```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
SOCKET_WS_URL=ws://localhost:8082/ws/parkit/websocket \
SESSION_COUNT=100 \
REPEAT_COUNT=10 \
STAGE_1_TARGET=50 \
STAGE_2_TARGET=200 \
perf/analysis-service/run-e2e-latency.sh
```

센서 이벤트만 별도 발행하고 싶다면:

```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
SESSION_COUNT=100 \
REPEAT_COUNT=10 \
EVENT_INTERVAL_MS=20 \
perf/analysis-service/publish-sensor-topic.sh
```

산출물
- `perf/results/analysis-socket-summary.json`
- `perf/results/analysis-socket-k6.log`

## Kubernetes / ArgoCD 환경 권장 방식

운영 유사 환경에서는 다음 순서가 가장 안정적입니다.

1. ArgoCD로 스테이징 환경에 서비스 배포
2. Kafka, Redis, Mongo 상태를 동일하게 준비
3. `k6`는 별도 runner Pod 또는 외부 runner에서 실행
4. 결과는 `k6 summary + Prometheus + Grafana`로 같이 확인

운영 체크 항목
- CPU / memory usage
- Kafka consumer lag
- Redis command latency
- Mongo write latency
- WebSocket 연결 유지율

## 결과 보고 형식

부하테스트 결과는 아래 형식으로 남기면 됩니다.

```text
대상: socket-service
시나리오: 동시 300 구독자, 100 세션 x 10회 sensor-topic 발행
결과: p95 analysis_processing_latency_ms=120ms, p95 socket_ws_delivery_latency_ms=210ms, error rate=0%
판정: 목표(500ms 이하) 충족
```
