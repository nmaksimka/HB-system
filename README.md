# Birthday Platform

Микросервисная система организации поздравлений с днём рождения. Backend написан на
Java 21, сборка — Gradle Kotlin DSL, основа — Spring Boot 4.0.2 и Spring Cloud
2025.1.1.

## Архитектура

| Компонент | Порт | Ответственность |
|---|---:|---|
| `api-gateway` | 8080 | Единая точка входа, CORS, REST- и WebSocket-маршрутизация |
| `identity-service` | 8081 | Пользователи, даты рождения, группы, членство и подписки |
| `gift-service` | 8082 | Личные списки желаемых подарков |
| `chat-service` | 8083 | Секретный чат друзей, REST-история и STOMP/WebSocket |
| `notification-service` | 8084 | Материализация и чтение напоминаний |

Каждый stateful-сервис владеет своей PostgreSQL-базой и схемой Flyway. Общих
таблиц и JPA entity между сервисами нет. Синхронные запросы выполняются через
OpenFeign с таймаутами, circuit breaker и безопасным fallback.

Пакеты внутри сервисов разделены на `domain`, `application` и `adapter`:
HTTP/WebSocket и Feign являются адаптерами, а бизнес-правила находятся в
application/domain слоях.

## Запуск

Требуются Docker и Docker Compose:

```bash
docker compose up --build
```

Проверка сборки без Docker:

```bash
./gradlew test
```

Все пользовательские HTTP-запросы отправляются через
`http://localhost:8080`. Actuator health каждого сервиса доступен напрямую,
например `http://localhost:8081/actuator/health`.

## Основной сценарий API

Создать двух пользователей:

```http
POST /api/users
Content-Type: application/json

{"name":"Максим","birthDate":"2003-11-18"}
```

```http
POST /api/users
Content-Type: application/json

{"name":"Анна","birthDate":"2004-07-15"}
```

Создать группу и добавить участника:

```http
POST /api/groups
Content-Type: application/json

{"name":"972501 ТГУ","ownerId":"<maxim-id>"}
```

```http
PUT /api/groups/<group-id>/members/<anna-id>
```

Подписаться на всю группу:

```http
POST /api/subscriptions
Content-Type: application/json

{"subscriberId":"<maxim-id>","targetType":"GROUP","targetId":"<group-id>"}
```

Опубликовать желание:

```http
POST /api/users/<anna-id>/wishes
Content-Type: application/json

{
  "title":"Наушники",
  "description":"Полноразмерные, с шумоподавлением",
  "link":"https://example.org/headphones",
  "priority":1
}
```

Получить агрегированную карточку друга:

```http
GET /api/users/<anna-id>/card
```

Обновить напоминания подписчика:

```http
POST /api/notifications/users/<maxim-id>/refresh?days=30
```

Отправить сообщение в секретный чат:

```http
POST /api/chat/users/<anna-id>/messages
Content-Type: application/json

{"authorId":"<maxim-id>","text":"Предлагаю подарить первый пункт из списка"}
```

Именинник не может читать собственный секретный чат или писать в него.

### Real-time чат

- STOMP endpoint: `ws://localhost:8080/ws`
- отправка: `/app/chat/<birthday-user-id>`
- подписка: `/topic/chat/<birthday-user-id>`
- payload: `{"authorId":"<author-id>","text":"..."}`

## Принятые решения

- Авторизация намеренно оставлена за границей MVP. Сейчас идентификаторы
  пользователя передаются явно; следующим production-шагом должен быть OIDC/JWT,
  а `authorId` и `viewerId` должны извлекаться из токена.
- Напоминания создаются идемпотентно по запросу `refresh`. В production этот use
  case следует запускать планировщиком или событием через Kafka/RabbitMQ.
- OpenFeign использован согласно требованиям. Для нового проекта Spring также
  рекомендует рассмотреть HTTP Service Clients, так как OpenFeign находится в
  feature-complete режиме.
