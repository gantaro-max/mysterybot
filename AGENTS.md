# MysteryBot Agent Guide

## Project
- Java 21 / Spring Boot 4.0.1 application using MyBatis XML mappers and Thymeleaf templates.
- Authentication is session based. Controllers check `HttpSession` attribute `loginGroupId`; the value `admin` means super admin.
- Keep `/callback` reachable from LINE and excluded from CSRF checks (configured in `SecurityConfig`).

## Commands
- Build and test: `./gradlew build`
- Tests only: `./gradlew test`
- Run locally: `./gradlew bootRun`
- Local DB: `docker-compose up -d`

## Security Model (implemented as of v2.2.0)
- Passwords stored as BCrypt hashes (`BCryptPasswordEncoder`).
- CSRF protection via Spring Security `SecurityFilterChain`; all Thymeleaf forms use `th:action`.
- Session fixation protection: `request.changeSessionId()` called on login and register.
- Logout is `POST`-only.
- Riddle ownership checked via `EventAdminService.getRiddleOwnedBy(id, groupId)` before any edit/delete.
- Reserved group IDs ("admin", "system", "root", "superadmin", "test") are blocked at registration.
- Image uploads validated by magic bytes (JPEG/PNG/GIF); Content-Type is fixed to `IMAGE_JPEG`.
- `TestController` has been deleted; no unauthenticated data endpoints exist.

## Editing Guidelines
- Prefer existing controller/service/repository boundaries.
- Keep MyBatis SQL in `src/main/resources/mappers`.
- Use Thymeleaf `th:action` for POST forms so CSRF tokens are emitted automatically.
- Do not commit `src/main/resources/application.properties`; it is gitignored. Use `application.properties.example` as the shareable template.
- When adding new riddle operations in `UserController`, always call `getRiddleOwnedBy(id, groupId)` for ownership verification.
