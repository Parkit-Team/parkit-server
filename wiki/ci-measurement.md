# CI Measurement Guide

## Scope
이 문서는 `refactor/ci`에서 적용한 "Jenkins와 Docker의 중복 Gradle 빌드 제거" 전후를 비교하기 위한 측정 기준입니다.

현재 변경의 목표는 다음 두 가지입니다.

- Jenkins에서 `bootJar`를 한 번만 수행하고 Docker는 결과 jar를 복사만 하도록 변경
- Jenkins 로그만으로 전후 시간을 비교할 수 있도록 측정 포인트를 고정

## Expected Impact
중복 빌드 제거로 기대하는 1차 효과는 아래와 같습니다.

- 서비스별 `Build Jar` 단계 시간 단축
- Docker build 단계 시간 단축
- 서비스별 전체 파이프라인 시간 단축

예상 범위:

- `Build Jar` + `Build Docker Image` 합산 시간: 25~45% 단축 가능
- 서비스별 전체 파이프라인 시간: 15~35% 단축 가능

실제 수치는 Jenkins runner 성능, Docker layer cache 적중률, 의존성 다운로드 여부에 따라 달라질 수 있습니다.

## Measurement Points
각 서비스별 Jenkins 로그에서 아래 시간을 기록합니다.

1. `Checkout`
2. `Grant Permissions`
3. `Build Jar`
4. `Build Docker Image`
5. `Push Docker Image`
6. `Update K8s Manifests`
7. `Cleanup`
8. 전체 파이프라인 시작 시각과 종료 시각

핵심 비교 대상은 아래 세 개입니다.

- `Build Jar`
- `Build Docker Image`
- 전체 파이프라인 시간

## How To Read Jenkins Logs
Jenkins Stage View 또는 Blue Ocean에서 각 stage duration을 기록합니다.

로그에서 특히 확인할 항목:

- `Build Jar` 단계에서 `bootJar` 수행 시간
- `Build Docker Image` 단계에서 build context 전송 시간
- `Build Docker Image` 단계에 Gradle compile 로그가 더 이상 나타나지 않는지 여부
- Docker layer cache hit/miss 여부
- `Push Docker Image` 단계가 전체 병목으로 남는지 여부

## Before/After Scorecard
아래 표를 서비스별로 채웁니다.

| Service | Run Date | Build Jar | Build Docker Image | Push Docker Image | Update K8s Manifests | Total |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| analysis-service before |  |  |  |  |  |  |
| analysis-service after |  |  |  |  |  |  |
| socket-service before |  |  |  |  |  |  |
| socket-service after |  |  |  |  |  |  |
| report-service before |  |  |  |  |  |  |
| report-service after |  |  |  |  |  |  |

## Delta Summary
전후 차이는 아래 계산식으로 정리합니다.

- `Build Delta = before(Build Jar + Build Docker Image) - after(Build Jar + Build Docker Image)`
- `Pipeline Delta = before(Total) - after(Total)`
- `Reduction % = Delta / before * 100`

예시:

- before: `Build Jar` 2m 40s, `Build Docker Image` 3m 20s, `Total` 9m 30s
- after: `Build Jar` 2m 10s, `Build Docker Image` 1m 10s, `Total` 7m 00s
- build-related delta: 2m 40s
- total delta: 2m 30s
- total reduction: 약 26.3%

## Notes
- 비교는 같은 서비스, 비슷한 변경량, 비슷한 시간대의 실행으로 맞추는 것이 좋습니다.
- Docker cache 상태가 크게 다르면 `Build Docker Image` 시간 비교가 왜곡될 수 있습니다.
- 이후 테스트 복구나 변경 감지 기준 보정이 들어가면 이 문서는 별도 버전으로 갱신하는 것이 좋습니다.
