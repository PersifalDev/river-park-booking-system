# booking-service

Сервис бронирования River Park. Отвечает за создание брони, проверку доступности, удержание и освобождение inventory, подтверждение, отмену, промокоды, напоминания и публикацию booking-событий.

## API

| Метод | URL | Описание |
| --- | --- | --- |
| `POST` | `/booking` | Создать бронь. Поддерживает `Idempotency-Key` |
| `GET` | `/booking` | Активные брони пользователя |
| `GET` | `/booking/inactive` | Отмененные, истекшие и failed-брони |
| `GET` | `/booking/{uuid}` | Бронь по UUID |
| `POST` | `/booking/search` | Поиск броней по фильтру |
| `POST` | `/booking/available/search` | Поиск доступных категорий номеров |
| `PATCH` | `/booking/{uuid}/cancel` | Отменить бронь |
| `PATCH` | `/booking/{uuid}/confirm` | Подтвердить бронь |

Swagger UI: `http://localhost:8084/swagger-ui.html`

## Бизнес-правила

| Правило | Значение по умолчанию |
| --- | --- |
| Максимум гостей | `6` |
| Минимум взрослых | `1` |
| Максимум взрослых | `4` |
| Максимум детей | `5` |
| Время HOLD | `15m` |
| Часовой пояс | `Asia/Novosibirsk` |

## Надежность

| Механизм | Где используется |
| --- | --- |
| `Idempotency-Key` | `POST /booking` |
| Sliding-window rate limit | Все booking endpoints, отдельно лимит на создание брони |
| Circuit breaker | HTTP-клиенты catalog/user |
| Timeout | HTTP-клиенты catalog/user |
| Transactional outbox | Booking Kafka events |
| Virtual threads | Внешние HTTP-задачи async booking chain |

## Основные env

| Переменная | Пример | Назначение |
| --- | --- | --- |
| `BOOKING_DB_URL` | `jdbc:postgresql://localhost:5436/booking_db` | PostgreSQL URL |
| `BOOKING_DB_USERNAME` | `postgres` | Пользователь БД |
| `BOOKING_DB_PASSWORD` | `postgres` | Пароль БД |
| `BOOKING_SERVER_PORT` | `8084` | HTTP порт |
| `BOOKING_MAX_TOTAL_GUESTS` | `6` | Максимум гостей |
| `BOOKING_HOLD_TTL` | `15m` | Время удержания брони |
| `BOOKING_IDEMPOTENCY_TTL` | `24h` | TTL ключа идемпотентности |
| `BOOKING_RATE_LIMIT_MAX_REQUESTS_PER_WINDOW` | `120` | Общий лимит в минуту |
| `BOOKING_RATE_LIMIT_MAX_CREATE_BOOKING_REQUESTS_PER_WINDOW` | `20` | Лимит создания броней в минуту |
| `CATALOG_SERVICE_BASE_URL` | `http://localhost:8085` | URL catalog-service |
| `USER_SERVICE_BASE_URL` | `http://localhost:8083` | URL user-service |

## Пример `.env`

```env
BOOKING_DB_USERNAME=postgres
BOOKING_DB_PASSWORD=postgres
BOOKING_DB_URL=jdbc:postgresql://localhost:5436/booking_db
BOOKING_SERVER_PORT=8084
BOOKING_MAX_TOTAL_GUESTS=6
BOOKING_HOLD_TTL=15m
BOOKING_IDEMPOTENCY_TTL=24h
BOOKING_EXTERNAL_HTTP_VIRTUAL_THREADS_ENABLED=true
CATALOG_SERVICE_BASE_URL=http://localhost:8085
USER_SERVICE_BASE_URL=http://localhost:8083
```

## Запуск

```powershell
.\mvnw.cmd -pl booking-service -am spring-boot:run
```

```powershell
cd booking-service
docker compose --env-file .env up --build -d
```
