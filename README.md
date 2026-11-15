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
  ├── comment-service ─ ewm-comment
  └── stats-server ──── ewm-stats

discovery-server :8761 — регистрация и поиск сервисов (Eureka)
config-server — централизованная конфигурация из infra/config-repo
```

### Модули

- `core/event-service` — события, категории и подборки. Публичные просмотры отправляет в статистику; счётчики подтверждённых заявок получает из `request-service`.
- `core/request-service` — создание, отмена, просмотр и модерация заявок. Проверяет пользователей и параметры событий через внутренние API.
- `core/user-service` — администрирование пользователей, рейтинг комментариев, предупреждения и блокировки.
- `core/comment-service` — дополнительная функциональность комментариев и связанная модерация.
- `core/interaction-api` — общие внешние и внутренние DTO, enum-ы, Feign-контракты и обработка ошибок. JPA-сущности сервисов в этот модуль не входят.
- `stat/stats-server`, `stat/stats-client`, `stat/stats-dto` — сбор и получение статистики просмотров.
- `infra` — Config Server, Eureka и Spring Cloud Gateway.

Каждый бизнес-сервис имеет собственную PostgreSQL-базу и `schema.sql`. Между базами нет общих таблиц и внешних ключей. События и комментарии хранят снимки имён пользователей, поэтому их чтение не требует запроса к `user-service`.

## Внешний API

Базовый URL: `http://localhost:8080`.

Спецификации:

- [Основной API](ewm-main-service-spec.json)
- [API статистики](ewm-stats-service-spec.json)

Gateway направляет существующие пути `/admin/**`, `/users/**`, `/events/**`, `/categories/**`, `/compilations/**`, `/comments/**`, `/hit` и `/stats` в соответствующие сервисы. Внутренние endpoint-ы `/internal/**` через Gateway не публикуются.

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

Пакетные операции исключают N+1 межсервисные запросы при формировании списков событий.

## Конфигурация

Локальные настройки каждого приложения находятся в `src/main/resources/application.yml`. Они содержат имя приложения, подключение к Config Server и адрес Eureka.

Остальные настройки централизованы в `infra/config-repo`:

- `application.yaml` — общие настройки, таймауты Feign, Circuit Breaker и Retry;
- `gateway-server.yaml` — порт и маршруты Gateway;
- `event-service.yaml`, `user-service.yaml`, `request-service.yaml`, `comment-service.yaml`, `stats-server.yaml` — случайные порты и независимые datasource-ы.

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

Config Server в Docker читает репозиторий конфигурации из смонтированного каталога `/config-repo`. Для локального запуска вне Docker сначала запускаются базы, `discovery-server` и `config-server`, затем бизнес-сервисы и Gateway. Config Server должен запускаться из корня проекта, поскольку его локальная конфигурация использует путь `./infra/config-repo`.

## Устойчивость к сбоям

- Для Feign настроены конечные connect/read timeout-ы.
- Пакетное получение счётчиков заявок защищено Resilience4j Circuit Breaker и Retry. При недоступности `request-service` события возвращаются с `confirmedRequests = 0`.
- При недоступности `stats-server` просмотры возвращаются со значением `0`; обращения к статистике не блокируют основной сценарий.
- Неидемпотентные операции записи автоматически не повторяются. Если обязательная зависимость недоступна, сервис возвращает управляемую `503 SERVICE_UNAVAILABLE` в формате `ApiError`, а не имитирует успешную запись.
- Ошибки внешнего API `400`, `404` и `409` сохраняются при межсервисных вызовах.

## Проверка

- `mvn clean verify` — сборка и все unit/controller-тесты.
- `mvn verify -Pcheck` — дополнительная проверка Checkstyle и SpotBugs.
- `postman/feature.json` — Postman-тесты дополнительной функциональности; URL в запросах должен указывать на Gateway `localhost:8080`.
- `docker compose config -q` — проверка конфигурации Compose.
