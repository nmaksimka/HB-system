# HB-system

Микросервисная система для организации поздравлений с днем рождения: пользователи, группы друзей, wishlist, секретный чат подарка и уведомления.

## Stack

- Java 21, Spring Boot 4.0.7, Spring Cloud 2025.1.2, Gradle Kotlin DSL
- Spring Cloud Gateway, Web MVC/WebFlux, WebSocket/STOMP, OpenFeign
- Spring Security OAuth2 Resource Server + Keycloak для локального dev auth
- PostgreSQL 17, Flyway, Hibernate `ddl-auto: validate`
- Docker Compose, Actuator, Prometheus registry, Springdoc OpenAPI

## Services

| Service | Port | Role |
|---|---:|---|
| `apiGateway` | 6767 | единая точка входа, CORS, routes, static web UI |
| `keycloak` | 8080 | локальный OIDC provider и demo users |
| `identityService` | 6701 | users, groups, memberships, subscriptions |
| `giftService` | 6702 | wishlist |
| `chatService` | 6703 | secret gift chat |
| `notificationService` | 6704 | birthday reminders |

Внешний клиент должен ходить через Gateway `http://localhost:6767`. `/internal/**` через Gateway не публикуется.

## Run

```powershell
Copy-Item .env.example .env
.\gradlew.bat clean build
docker compose up --build
```

UI: http://localhost:6767

Keycloak admin: http://localhost:8080, `admin` / `admin` по умолчанию из `.env.example`.

Demo users импортируются в Keycloak и Flyway seed:

| Username | Password | User id |
|---|---|---|
| `maxim` | `password` | `11111111-1111-1111-1111-111111111111` |
| `anna` | `password` | `22222222-2222-2222-2222-222222222222` |
| `ivan` | `password` | `33333333-3333-3333-3333-333333333333` |

Получить dev access token:

```powershell
$token = (Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/realms/birthday-hub/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "grant_type=password&client_id=birthday-hub-ui&username=maxim&password=password").access_token
```

В UI вставьте token в поле `Access token`. API-запросы должны передавать его как `Authorization: Bearer <token>`.

## Current User Contract

Действия текущего пользователя больше не доверяют `userId`, `authorId`, `viewerId` или `subscriberId` из request body/query/path. Сервисы берут actor id из JWT claim `user_id`, с fallback на JWT `sub`.

Основные endpoints через Gateway:

```http
GET    /api/me
GET    /api/users
GET    /api/users/{id}
GET    /api/users/{id}/card

POST   /api/groups
GET    /api/groups
GET    /api/groups/{groupId}/members
PUT    /api/groups/{groupId}/members/me
DELETE /api/groups/{groupId}/members/me

POST   /api/subscriptions/users/{targetUserId}
POST   /api/subscriptions/groups/{groupId}
DELETE /api/subscriptions/users/{targetUserId}
DELETE /api/subscriptions/groups/{groupId}
GET    /api/subscriptions

GET    /api/users/{userId}/wishes
GET    /api/me/wishes
POST   /api/me/wishes
DELETE /api/me/wishes/{wishId}

GET    /api/chat/users/{subjectUserId}/messages
POST   /api/chat/users/{subjectUserId}/messages

POST   /api/notifications/refresh?days=30
GET    /api/notifications
GET    /api/notifications/unread
PUT    /api/notifications/{reminderId}/read
PUT    /api/notifications/read-all
```

Chat send body:

```json
{"text":"Предлагаю подарок из wishlist"}
```

Wish create body:

```json
{
  "title": "Наушники",
  "description": "Полноразмерные, с шумоподавлением",
  "link": "https://example.org/headphones",
  "priority": 1
}
```

## Verification

```powershell
.\gradlew.bat clean build
docker compose config
```

Covered by tests now:

- birthday date calculations, including leap day;
- birthday person cannot read/write own secret chat;
- repeated subscription is idempotent;
- user cannot delete another user's wishlist item;
- user cannot mark another user's reminder as read.

## Notes

- Schemas are managed by Flyway only; Hibernate stays on `validate`.
- `identityService` exposes `/internal/users/{id}/exists` and `/internal/users/{id}` for service-to-service checks.
- `notificationService` still has manual refresh for demo; scheduled reminder generation and notification preferences remain the next backend step.
- Web UI is currently the static Gateway UI with a dev token field. A dedicated React/Vite frontend module is still pending.
