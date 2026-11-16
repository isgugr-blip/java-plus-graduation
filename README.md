# Explore With Me

Backend-сервис для публикации событий, заявок на участие, подборок и комментариев. Проект разделён на микросервисы; существующий внешний API сохранён и доступен через API Gateway на порту `8080`.

## Архитектура

```text
Client
  │
  ▼
gateway-server :8080
  ├── user-service ──── ewm-user
  ├── event-service ─── ewm-main
  ├── request-service ─ ewm-request
  └── comment-service ─ ewm-comment

event-service, request-service ──gRPC──▶ collector ──▶ stats.user-actions.v1
                                                         ├──▶ aggregator ──▶ stats.events-similarity.v1
                                                         │                          │
                                                         ▼                          │
event-service ─────────────gRPC────────────────────▶ analyzer ◀─────────────────────┘
                                                         └── ewm-analyzer

discovery-server :8761 — регистрация и поиск сервисов (Eureka)
config-server — централизованная конфигурация из infra/config-repo
```

### Модули

- `core/event-service` — события, категории и подборки. Просмотры и лайки отправляет в `collector`, рейтинг и рекомендации получает из `analyzer`; счётчики подтверждённых заявок получает из `request-service`.
- `core/request-service` — создание, отмена, просмотр и модерация заявок. Регистрацию на событие отправляет в `collector`. Проверяет пользователей и параметры событий через внутренние API.
- `core/user-service` — администрирование пользователей, рейтинг комментариев, предупреждения и блокировки.
- `core/comment-service` — дополнительная функциональность комментариев и связанная модерация.
- `core/interaction-api` — общие внешние и внутренние DTO, enum-ы, Feign-контракты и обработка ошибок. JPA-сущности сервисов в этот модуль не входят.
- `stat/collector` — gRPC-сервис `UserActionController`: принимает действия пользователей и пишет их в Kafka (`stats.user-actions.v1`, Avro).
- `stat/aggregator` — читает действия, инкрементально пересчитывает косинусное сходство мероприятий и пишет его в `stats.events-similarity.v1`. Состояние хранится в памяти.
- `stat/analyzer` — сохраняет действия и сходство в PostgreSQL и отдаёт рекомендации через gRPC-сервис `RecommendationsController`.
- `stat/stats-client` — gRPC-клиенты `CollectorClient` и `AnalyzerClient` (адреса `discovery:///collector`, `discovery:///analyzer`).
- `stat/proto-schemas`, `stat/avro-schemas` — схемы gRPC и Avro, сериализаторы Kafka и веса действий.
- `infra` — Config Server, Eureka и Spring Cloud Gateway.

Каждый бизнес-сервис имеет собственную PostgreSQL-базу и `schema.sql`. Между базами нет общих таблиц и внешних ключей. События и комментарии хранят снимки имён пользователей, поэтому их чтение не требует запроса к `user-service`.

## Внешний API

Базовый URL: `http://localhost:8080`.

Спецификации:

- [Основной API](ewm-main-service-spec.json)

Gateway направляет существующие пути `/admin/**`, `/users/**`, `/events/**`, `/categories/**`, `/compilations/**` и `/comments/**` в соответствующие сервисы.

Рекомендации:

- поле `views` у событий заменено на `rating` (`double`) — сумма максимальных весов действий пользователей с событием (просмотр 0.4, регистрация 0.8, лайк 1.0);
- `GET /events/{id}` требует заголовок `X-EWM-USER-ID` и отправляет просмотр; `GET /events` просмотры не отправляет;
- `GET /events/recommendations?maxResults=10` с заголовком `X-EWM-USER-ID` — рекомендованные события;
- `PUT /events/{eventId}/like` с заголовком `X-EWM-USER-ID` — лайк; доступен только участнику с подтверждённой заявкой, иначе `400`. Внутренние endpoint-ы `/internal/**` через Gateway не публикуются.

## Внутренний API

Взаимодействие выполняется через OpenFeign с именами сервисов в Eureka.

### `user-service`

- `GET /internal/users/{userId}` — пользователь, счётчики и состояние блокировки.
- `POST /internal/users/batch` — пакетное получение пользователей по массиву ID.
- `POST /internal/users/{userId}/comment-count?delta={delta}` — изменить счётчик комментариев и пересчитать ранг.
- `POST /internal/users/{userId}/warnings` — добавить предупреждение и применить правило блокировки.

### `event-service`

- `GET /internal/events/{eventId}` — инициатор, состояние, лимит участников и признак модерации события.

### `request-service`

- `POST /internal/requests/counts?status=CONFIRMED` — пакетные счётчики заявок; тело запроса содержит массив `eventId`.
- `GET /internal/requests/confirmed?userId={userId}&eventId={eventId}` — есть ли у пользователя подтверждённая заявка на событие.

Пакетные операции исключают N+1 межсервисные запросы при формировании списков событий.

## Конфигурация

Локальные настройки каждого приложения находятся в `src/main/resources/application.yml`. Они содержат имя приложения, подключение к Config Server и адрес Eureka.

Остальные настройки централизованы в `infra/config-repo`:

- `application.yaml` — общие настройки, таймауты Feign, Circuit Breaker и Retry, имена топиков Kafka и адреса gRPC-клиентов;
- `gateway-server.yaml` — порт и маршруты Gateway;
- `event-service.yaml`, `user-service.yaml`, `request-service.yaml`, `comment-service.yaml` — случайные порты и независимые datasource-ы;
- `collector.yaml`, `aggregator.yaml`, `analyzer.yaml` — Kafka, случайный порт gRPC-сервера и datasource анализатора.

## Запуск

Требуются Java 21, Maven и Docker Compose.

Собрать JAR-файлы и запустить тесты:

```bash
mvn clean verify
```

Запустить всю систему:

```bash
docker compose up --build -d
```

Проверить состояние:

```bash
docker compose ps
curl http://localhost:8080/events
```

Остановить систему:

```bash
docker compose down
```

Kafka поднимается в Compose вместе с контейнером `kafka-init-topics`, который создаёт топики `stats.user-actions.v1` и `stats.events-similarity.v1`. Снаружи брокер доступен на `localhost:9092`, внутри сети Compose — на `kafka:29092`.

Config Server в Docker читает репозиторий конфигурации из смонтированного каталога `/config-repo`. Для локального запуска вне Docker сначала запускаются базы, `discovery-server` и `config-server`, затем бизнес-сервисы и Gateway. Config Server должен запускаться из корня проекта, поскольку его локальная конфигурация использует путь `./infra/config-repo`.

## Устойчивость к сбоям

- Для Feign настроены конечные connect/read timeout-ы.
- Пакетное получение счётчиков заявок защищено Resilience4j Circuit Breaker и Retry. При недоступности `request-service` события возвращаются с `confirmedRequests = 0`.
- При недоступности `analyzer` рейтинг событий возвращается со значением `0`; сбой отправки просмотра или регистрации в `collector` только логируется. Лайк и рекомендации при недоступности сервисов возвращают `503`.
- Неидемпотентные операции записи автоматически не повторяются. Если обязательная зависимость недоступна, сервис возвращает управляемую `503 SERVICE_UNAVAILABLE` в формате `ApiError`, а не имитирует успешную запись.
- Ошибки внешнего API `400`, `404` и `409` сохраняются при межсервисных вызовах.

## Проверка

- `mvn clean verify` — сборка и все unit/controller-тесты.
- `mvn verify -Pcheck` — дополнительная проверка Checkstyle и SpotBugs.
- `postman/feature.json` — Postman-тесты дополнительной функциональности; URL в запросах должен указывать на Gateway `localhost:8080`.
- `docker compose config -q` — проверка конфигурации Compose.
