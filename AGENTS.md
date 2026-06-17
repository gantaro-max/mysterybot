# MysteryBot Agent Guide

## Project
- Java 21 / Spring Boot 4.0.1 application using MyBatis XML mappers and Thymeleaf templates.
- Authentication is session based. Controllers check `HttpSession` attribute `loginGroupId`; the value `admin` means super admin.
- Keep `/callback` reachable from LINE and excluded from CSRF checks.

## Commands
- Build and test: `./gradlew build`
- Tests only: `./gradlew test`
- Run locally: `./gradlew bootRun`
- Local DB: `docker-compose up -d`

## Security Notes
- Do not commit real secrets in `src/main/resources/application.properties`; keep placeholders there and copy real values locally when needed.
- Use `src/main/resources/application.properties.example` as the shareable config template.
- Validate tenant ownership before allowing event-admin reads, updates, or deletes of riddles.
- Uploaded images must be validated by file bytes, not client-provided MIME headers.

## Editing Guidelines
- Prefer existing controller/service/repository boundaries.
- Keep MyBatis SQL in `src/main/resources/mappers`.
- Use Thymeleaf `th:action` for POST forms so CSRF tokens are emitted automatically.
- Avoid unrelated refactors while security fixes are in progress.
