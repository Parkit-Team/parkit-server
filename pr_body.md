## 📌 관련 이슈
- #이슈번호

## ✨ 변경 내용
- `report-service` OpenAPI(Swagger) 설정 추가 (`SwaggerConfig.kt`, `application.yml`)
- `analysis-service` OpenAPI(Swagger) 설정 추가 (`SwaggerConfig.kt`, `application.yml`)
- `socket-service` OpenAPI(Swagger) 설정 추가 (`SwaggerConfig.kt`, `application.yml`)
- 각 마이크로서비스에서 개별적으로 API 문서를 확인할 수 있도록 `/swagger-ui.html` 및 `/v3/api-docs` 엔드포인트 활성화

## ✅ 체크리스트
- [x] 빌드 및 테스트를 통과했나요?
- [x] 불필요한 로그나 주석은 제거했나요?

## 📝 리뷰 포인트
- 각 서비스의 OpenAPI Info(Title, Version, Description)가 적명한지 확인 부탁드립니다.
- 추후 전체 서비스를 묶어주는 API Gateway 통합 시 해당 개별 Swagger 문서들을 하나로 통합할 기반이 확보되었습니다.
