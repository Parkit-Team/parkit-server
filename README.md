# Parkit
실시간 주차 센서 데이터를 기반으로 주차 코칭 데이터를 생성하고, 클라이언트에 WebSocket(STOMP)으로 전달하는 마이크로서비스 레포입니다.



https://github.com/user-attachments/assets/04a8bd32-9d5a-49a7-90c9-b135222fdd78






## Contributor
### [김리나](https://github.com/ri-naa)
- Jenkins와 Docker를 활용하여 CI 자동화
- 실시간 스트림 데이터 수집을 위한 Kafka Consumer 구현

### [유진](https://github.com/HI-JIN2) 
- 실시간 주차 코칭 알고리즘 설계 및 구현
- Kafka를 활용해 마이크로서비스 간의 데이터 파이프라인 구축
- WebSocket 기반 실시간 코칭 데이터 스트리밍 구현

## Tech Stack
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?logo=kotlin&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-6DB33F?logo=springboot&logoColor=white)
![Spring Kafka](https://img.shields.io/badge/Spring-Kafka-6DB33F?logo=spring&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-010101?logo=websocket&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-Database-47A248?logo=mongodb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Cache-DC382D?logo=redis&logoColor=white)

## Services
- `analysis-service/` : Kafka 센서 이벤트 소비 → 주차 코칭 계산 → Kafka 이벤트 발행
- `socket-service/` : Kafka 코칭 이벤트 소비 → WebSocket(STOMP)으로 브로드캐스트
- `report-service/` : 주행 세션(start/stop) → 센서 로그 저장/조회 (MongoDB)

report-service 주요 API
- `POST /api/driving-sessions/start`
- `POST /api/driving-sessions/{sessionId}/stop` (body: `frontendScore`)
- `GET /api/driving-sessions/{sessionId}/report` (세션 + 센서로그)

## Data Flow (Local)
1) `sensor-topic` (센서 이벤트) → analysis-service consume
2) analysis-service 계산
   - 코칭: `coaching-event`
3) socket-service가 `coaching-event` consume 후 `/topic/coaching` 브로드캐스트

4) (선택) report-service가 `sensor-topic`을 consume하여 RUNNING 세션에 센서 로그를 자동 저장
   - Kafka record key가 `sessionId`이면 해당 세션으로 저장
   - key가 없으면 가장 최근 RUNNING 세션으로 저장
   - 비활성화: `parkit.kafka.enabled=false`

## WebSocket
- Real endpoint: `http://localhost:8082/ws/parkit`
- Real topic: `/topic/coaching`
- Mock endpoint: `http://localhost:8082/ws/parkit-mock`
- Mock topic: `/topic/coaching-mock`
- 테스트 클라이언트: `socket-test.html`

코칭 메시지 스키마(요약)
- `targetAngle`/`targetDistance`: step별 고정값 (angle=deg, distance=cm)
- `currentAngle`/`currentDistance`: 실시간값 (angle=deg, distance=cm)
- `distances`: 장애물 거리(cm)

## Swagger
- analysis-service: `http://localhost:8081/swagger-ui.html`
- socket-service: `http://localhost:8082/swagger-ui.html`
- report-service: `http://localhost:8083/swagger-ui.html`

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

# report-service
cd ../report-service
bash ./gradlew clean test
bash ./gradlew bootRun
```

로컬 Kafka가 필요합니다(기본 `localhost:9092`, env: `SPRING_KAFKA_BOOTSTRAP_SERVERS`).

report-service는 MongoDB가 필요합니다(env: `SPRING_MONGODB_URI`, 기본: `mongodb://mongodb:27017/parkit`).
- 로컬에서 report-service를 호스트에서 직접 실행하면 `mongodb://localhost:27017/parkit`로 설정하세요.

Known limitations
- analysis-service는 현재 Kafka 메시지에 sessionId가 연결되지 않아(임시로 `unknown-session`) 멀티 세션 시나리오에서는 step/상태가 섞일 수 있습니다.

## Local Infra (Optional)
```bash
docker compose up -d
```
`docker-compose.yml`은 MongoDB/Redis를 포함합니다. Kafka는 별도로 준비해야 합니다.
