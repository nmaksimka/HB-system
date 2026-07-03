# HB-system

Микросервисная платформа для организации поздравлений с днём рождения.

## Технологии

- Java 21
- Spring Boot 4.0.7
- Spring Cloud 2025.1.2
- Gradle Kotlin DSL 8.14.3
- Spring Cloud Gateway 5 и OpenFeign 5
- PostgreSQL 17 и Flyway
- WebSocket/STOMP
- Resilience4j
- Springdoc OpenAPI 3.0.3
- Actuator, Micrometer и Prometheus registry
- Docker Compose

## Модули

| Модуль | Порт | Назначение |
|---|---:|---|
| `apiGateway` | 8080 | Единая точка входа, маршрутизация, CORS и correlation ID |
| `identityService` | 8081 | Пользователи, дни рождения, группы и подписки |
| `giftService` | 8082 | Списки желаемых подарков |
| `chatService` | 8083 | Секретные обсуждения подарков и real-time сообщения |
| `notificationService` | 8084 | Создание и чтение напоминаний |

Каждый stateful-сервис владеет отдельной PostgreSQL-базой и миграциями Flyway.
Общих JPA entity и общих таблиц между сервисами нет.

## Структура

Структура модулей приведена к форме проекта DeliveryHub:

```text
HB-system/
├── apiGateway/
│   ├── Dockerfile
│   └── src/main/java/.../gateway/
│       └── filter/
├── identityService/
│   ├── Dockerfile
│   └── src/main/java/.../identity/
│       ├── client/
│       ├── config/
│       ├── controller/
│       ├── dto/
│       ├── entity/
│       ├── mapper/
│       ├── repository/
│       └── service/
├── giftService/
├── chatService/
├── notificationService/
├── docker-compose.yml
├── build.gradle.kts
└── settings.gradle.kts
```

CamelCase используется для Gradle-модулей, Compose-сервисов и внутренних
идентификаторов. Java packages остаются lowercase, Spring properties —
kebab-case, переменные окружения — UPPER_SNAKE_CASE, а SQL — snake_case согласно
стандартам этих экосистем.

## Запуск

Из корня проекта:

```powershell
docker compose -f docker-compose.yml up --build
```

Для другого пароля PostgreSQL:

```powershell
$env:DB_PASSWORD = "change-me"
docker compose -f docker-compose.yml up --build
```

Остановка:

```powershell
docker compose -f docker-compose.yml down
```

Удаление также томов с локальными данными:

```powershell
docker compose -f docker-compose.yml down --volumes
```

## Проверка

```powershell
.\gradlew.bat clean build
docker compose -f docker-compose.yml config --quiet
```

После запуска:

- Gateway health: http://localhost:8080/actuator/health
- Identity Swagger UI: http://localhost:8081/swagger-ui.html
- Gift Swagger UI: http://localhost:8082/swagger-ui.html
- Chat Swagger UI: http://localhost:8083/swagger-ui.html
- Notification Swagger UI: http://localhost:8084/swagger-ui.html

## Пример сценария

Создание пользователей:

```http
POST http://localhost:8080/api/users
Content-Type: application/json

{"name":"Максим","birthDate":"2003-11-18"}
```

```http
POST http://localhost:8080/api/users
Content-Type: application/json

{"name":"Анна","birthDate":"2004-07-15"}
```

Создание группы:

```http
POST http://localhost:8080/api/groups
Content-Type: application/json

{"name":"972501 ТГУ","ownerId":"<maximId>"}
```

Добавление участника:

```http
PUT http://localhost:8080/api/groups/<groupId>/members/<annaId>
```

Подписка на группу:

```http
POST http://localhost:8080/api/subscriptions
Content-Type: application/json

{"subscriberId":"<maximId>","targetType":"GROUP","targetId":"<groupId>"}
```

Добавление желания:

```http
POST http://localhost:8080/api/users/<annaId>/wishes
Content-Type: application/json

{
  "title":"Наушники",
  "description":"Полноразмерные, с шумоподавлением",
  "link":"https://example.org/headphones",
  "priority":1
}
```

Карточка друга:

```http
GET http://localhost:8080/api/users/<annaId>/card
```

Напоминания:

```http
POST http://localhost:8080/api/notifications/users/<maximId>/refresh?days=30
```

Сообщение в секретный чат:

```http
POST http://localhost:8080/api/chat/users/<annaId>/messages
Content-Type: application/json

{"authorId":"<maximId>","text":"Предлагаю подарить первый пункт из списка"}
```

Именинник не может читать собственный секретный чат или писать в него.

### Real-time чат

- STOMP endpoint: `ws://localhost:8080/ws`
- отправка: `/app/chat/<birthdayUserId>`
- подписка: `/topic/chat/<birthdayUserId>`
- payload: `{"authorId":"<authorId>","text":"..."}`

## Архитектурные решения

- DTO отделены от JPA entity.
- Контроллеры не содержат бизнес-логику.
- Constructor injection используется во всех компонентах.
- Транзакции задаются на уровне сервисов; чтение помечено `readOnly`.
- Ошибки HTTP возвращаются как RFC 9457 `ProblemDetail`.
- Hibernate работает с `ddl-auto: validate`; схемой управляет только Flyway.
- `open-in-view` отключён.
- Feign имеет короткие connect/read timeouts и circuit breaker.
- Сбой зависимого сервиса возвращает 503 и не маскируется пустыми данными.
- Время передаётся через `Clock`, что делает тесты детерминированными.
- Gateway создаёт или прокидывает `X-Correlation-Id`.
- Контейнеры приложений запускаются не от root.
- Readiness health checks управляют порядком запуска Compose.
- Пароль БД можно передавать только через окружение; production-секреты не
  должны храниться в Git.

OpenFeign сохранён согласно требованиям проекта. Для нового production-кода
следует также оценить Spring HTTP Service Clients: OpenFeign находится в режиме
feature-complete.

Аутентификация намеренно не реализована самодельным JWT. Следующий безопасный
шаг — подключение внешнего OIDC-провайдера (Keycloak, Auth0, Яндекс ID) и
настройка Gateway/сервисов как OAuth2 Resource Server.
