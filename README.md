# Parkit
실시간 주차 센서 데이터를 기반으로 채점/코칭을 계산하고, 클라이언트에 WebSocket(STOMP)으로 전달하는 멀티 서비스 레포입니다.

## Services
- `analysis-service/` : Kafka 센서 이벤트 소비 → 주차 채점/코칭 계산 → Kafka 이벤트 발행
- `socket-service/` : Kafka 코칭 이벤트 소비 → WebSocket(STOMP)으로 브로드캐스트
- `report-service/` : 리포트/저장(별도 서비스)

## Data Flow (Local)
1) `sensor-topic` (센서 이벤트) → analysis-service consume
2) analysis-service 계산
   - 코칭: `coaching-event`
   - 채점: `parking-score-result`
3) socket-service가 `coaching-event` consume 후 `/topic/coaching` 브로드캐스트

## WebSocket
- Real endpoint: `http://localhost:8082/ws/parkit`
- Real topic: `/topic/coaching`
- Mock endpoint: `http://localhost:8082/ws/parkit-mock`
- Mock topic: `/topic/coaching-mock`
- 테스트 클라이언트: `socket-test.html`

## Swagger
- analysis-service: `http://localhost:8081/swagger-ui.html`
- socket-service: `http://localhost:8082/swagger-ui.html`

## Run / Test
이 레포는 루트 Gradle 빌드가 없습니다. 서비스 디렉토리에서 실행하세요.

```bash
# analysis-service
cd analysis-service
bash ./gradlew clean test
bash ./gradlew bootRun

# socket-service
cd ../socket-service
bash ./gradlew clean test
bash ./gradlew bootRun
```

로컬 Kafka가 필요합니다(기본 `localhost:9092`).

## Local Infra (Optional)
```bash
docker compose up -d
```
`docker-compose.yml`은 Postgres/Redis만 포함합니다.
