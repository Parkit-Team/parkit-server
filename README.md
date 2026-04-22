# Parkit

실시간 주차 센서 데이터를 기반으로 주차 코칭 데이터를 생성하고, 클라이언트에 WebSocket으로 전달하는 MSA 기반 서버 레포지토리입니다.

## Overview

- `analysis-service/`: 센서 이벤트를 소비해 주차 코칭 데이터를 계산하고 Kafka로 발행합니다.
- `socket-service/`: 코칭 이벤트를 소비해 STOMP/WebSocket으로 브로드캐스트합니다.
- `report-service/`: 주행 세션과 센서 로그를 저장하고 조회 API를 제공합니다.

## Tech Stack
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?logo=kotlin&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-6DB33F?logo=springboot&logoColor=white)
![Spring Kafka](https://img.shields.io/badge/Spring-Kafka-6DB33F?logo=spring&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-010101?logo=websocket&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-Database-47A248?logo=mongodb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Cache-DC382D?logo=redis&logoColor=white)

## Contributors

### [김리나](https://github.com/ri-naa)

- Jenkins와 Docker를 활용하여 CI 자동화
- 실시간 스트림 데이터 수집을 위한 Kafka Consumer 구현

### [유진](https://github.com/HI-JIN2)

- 실시간 주차 코칭 알고리즘 설계 및 구현
- Kafka를 활용해 마이크로서비스 간의 데이터 파이프라인 구축
- WebSocket 기반 실시간 코칭 데이터 스트리밍 구현

## Services

- `analysis-service/`: Kafka 센서 이벤트 소비 → 주차 코칭 계산 → Kafka 이벤트 발행
- `socket-service/`: Kafka 코칭 이벤트 소비 → WebSocket(STOMP) 브로드캐스트
- `report-service/`: 주행 세션 start/stop → 센서 로그 저장/조회(MongoDB)

report-service 주요 API
- `POST /api/driving-sessions/start`
- `POST /api/driving-sessions/{sessionId}/stop` (body: `frontendScore`)
- `GET /api/driving-sessions/{sessionId}/report` (세션 + 센서로그)

## Prerequisites

이 레포는 루트 Gradle 빌드가 없습니다. 각 서비스 디렉토리에서 개별적으로 실행해야 합니다.

필수 런타임

- Java 17
- Kafka

서비스별 추가 의존성

- `analysis-service`: Redis
- `report-service`: MongoDB
- `socket-service`: 추가 저장소 없음

기본 환경 변수

- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: 기본값 `localhost:9092`
- `SPRING_DATA_REDIS_HOST`: 기본값 `localhost`
- `SPRING_DATA_REDIS_PORT`: 기본값 `6379`
- `SPRING_MONGODB_URI`: 기본값 `mongodb://mongodb:27017/parkit`

## Quick Start

로컬에서 가장 단순하게 확인하려면 아래 순서로 실행하면 됩니다.

1. Kafka를 준비합니다.
2. 선택적으로 MongoDB/Redis를 Docker Compose로 올립니다.
3. 서비스별로 개별 터미널에서 실행합니다.

MongoDB와 Redis만 빠르게 올리기:

```bash
docker compose up -d
```

`docker-compose.yml`은 MongoDB와 Redis만 포함합니다. Kafka는 별도로 준비해야 합니다.

서비스 실행:

```bash
# terminal 1
cd analysis-service
bash ./gradlew bootRun

# terminal 2
cd socket-service
bash ./gradlew bootRun

# terminal 3
cd report-service
bash ./gradlew bootRun
```

기본 포트

- `analysis-service`: `8081`
- `socket-service`: `8082`
- `report-service`: `8083`

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

Mock endpoint는 기본적으로 비활성화되어 있습니다. 사용하려면 `socket-service` 실행 시 `parkit.mock.coaching.enabled=true`를
설정해야 합니다.

```bash
cd socket-service
bash ./gradlew bootRun --args='--parkit.mock.coaching.enabled=true'
```

코칭 메시지 스키마(요약)
- `targetAngle`/`targetDistance`: step별 고정값 (angle=deg, distance=cm)
- `currentAngle`/`currentDistance`: 실시간값 (angle=deg, distance=cm)
- `distances`: 장애물 거리(cm)

## Swagger
- analysis-service: `http://localhost:8081/swagger-ui.html`
- socket-service: `http://localhost:8082/swagger-ui.html`
- report-service: `http://localhost:8083/swagger-ui.html`

## Run / Test

Gradle wrapper는 실행 권한이 없을 수 있으므로 `bash ./gradlew ...` 형식으로 실행하는 것을 권장합니다.

```bash
# analysis-service
cd analysis-service
bash ./gradlew clean build
bash ./gradlew test
bash ./gradlew bootRun

# socket-service
cd ../socket-service
bash ./gradlew clean build
bash ./gradlew test
bash ./gradlew bootRun

# report-service
cd ../report-service
bash ./gradlew clean build
bash ./gradlew test
bash ./gradlew bootRun
```

예시

```bash
cd report-service
SPRING_MONGODB_URI=mongodb://localhost:27017/parkit bash ./gradlew bootRun
```

```bash
cd analysis-service
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
SPRING_DATA_REDIS_HOST=localhost \
SPRING_DATA_REDIS_PORT=6379 \
bash ./gradlew bootRun
```

Known limitations
- analysis-service는 현재 Kafka 메시지에 sessionId가 연결되지 않아(임시로 `unknown-session`) 멀티 세션 시나리오에서는 step/상태가 섞일 수 있습니다.

## Performance Tests

부하 테스트를 위한 세팅과 실행 가이드는 [perf/README.md](/Users/yujin/IdeaProjects/parkit-server/perf/README.md) 에
정리되어 있습니다.
