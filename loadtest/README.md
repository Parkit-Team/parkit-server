# Load Test (k6)

목표
- 동시 세션 50대
- 세션당 0.5초에 1건 (총 100 msg/s)
- E2E 지연(p95) 100ms 목표: `sensor-topic` 발행 시각(`sent_at_epoch_ms`)부터 클라이언트 `/topic/coaching` 수신 시각까지

## 준비

1) Kafka/Redis 기동

```bash
docker compose -f docker-compose.loadtest.yml up -d
```

2) 서비스 실행 (각 서비스 디렉터리에서)

analysis-service

```bash
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
SPRING_DATA_REDIS_HOST=localhost \
bash ./gradlew bootRun
```

socket-service

```bash
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
PARKIT_WS_RAW_ENABLED=true \
bash ./gradlew bootRun
```

3) k6 설치

```bash
brew install k6
```

## 센서 이벤트 발행(부하 생성)

`kcat` 컨테이너를 사용해 `sensor-topic`에 키=세션ID로 메시지를 발행합니다.

```bash
bash loadtest/scripts/produce_sensor_kcat.sh
```

환경변수
- `BOOTSTRAP` (default: `localhost:9092`)
- `TOPIC` (default: `sensor-topic`)
- `SESSIONS` (default: `50`)
- `INTERVAL_MS` (default: `500`) 세션별 발행 주기

## WS 구독 + 지연 측정(k6)

socket-service의 raw WS 엔드포인트(`/ws/parkit-raw`)로 연결한 뒤 `/topic/coaching`를 구독하고, 메시지의 `sensorSentAtEpochMs`로 지연을 계산합니다.

```bash
k6 run loadtest/k6/ws_coaching_latency.js
```

환경변수
- `WS_URL` (default: `ws://localhost:8082/ws/parkit-raw`)
- `VUS` (default: `50`)
- `DURATION` (default: `2m`)

## 참고(관측)

Prometheus 포맷 메트릭
- analysis-service: `http://localhost:8081/actuator/prometheus`
- socket-service: `http://localhost:8082/actuator/prometheus`
