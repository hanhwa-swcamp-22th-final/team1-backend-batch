# team1-backend-batch

Spring Batch 기반 정산 서비스 스켈레톤이다.

현재 포함 범위:

- Spring Boot/Gradle 기본 구동 구조
- batch DB + WMS read datasource 설정 뼈대
- Feign/Kafka 설정 뼈대
- billing 도메인/리포지토리/서비스/스케줄러 기본 패키지 구조

다음 단계에서는 WMS 내부 API, 배치 테이블, 월 정산 SQL, Kafka consumer 연동을 순서대로 채운다.

공개 저장소 기준 보안 원칙:

- 운영/개발용 DB 계정과 비밀번호는 커밋하지 않는다.
- 민감 설정은 환경변수 또는 커밋 제외된 로컬 설정 파일로만 주입한다.
- 로컬 전용 설정 파일 예시는 `src/main/resources/application-local.yml` 형태로 관리하고, Git에는 포함하지 않는다.

현재 실행에 필요한 주요 환경변수:

- `BATCH_DB_URL`
- `BATCH_DB_USERNAME`
- `BATCH_DB_PASSWORD`
- `WMS_READ_DB_URL`
- `WMS_READ_DB_USERNAME`
- `WMS_READ_DB_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`
- `WMS_BASE_URL`
