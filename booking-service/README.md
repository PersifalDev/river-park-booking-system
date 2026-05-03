# booking-service

Сервис бронирования. Отвечает за availability, HOLD, CONFIRMED, CANCELLED, EXPIRED и inventory.

## API

- `POST /booking` — создать бронь
- `POST /booking/search` — поиск броней текущего пользователя по фильтру
- `GET /booking` — получить активные брони текущего пользователя
- `GET /booking/{uuid}` — получить бронь по uuid
- `PATCH /booking/{uuid}/cancel` — отменить бронь
- `PATCH /booking/{uuid}/confirm` — подтвердить бронь

Kafka consumer:
- `PAYMENT_CONFIRMED`
- `PAYMENT_CANCELLED`
- `PAYMENT_FAILED`

Kafka producer:
- `BOOKING_CREATED`
- `BOOKING_HOLD_CREATED`
- `BOOKING_CONFIRMED`
- `BOOKING_CANCELLED`
- `BOOKING_EXPIRED`
- `BOOKING_FAILED`


## Конфигурация env

- `BOOKING_DB_URL`
- `BOOKING_DB_USERNAME`
- `BOOKING_DB_PASSWORD`
- `BOOKING_SERVER_PORT`
- `JWT_SECRET_KEY`
- `JWT_LIFETIME`
- `KAFKA_HOST_PORT`
- `BOOKING_EVENTS_TOPIC`
- `PAYMENT_EVENTS_TOPIC`
- `CATALOG_SERVICE_BASE_URL`
- `USER_SERVICE_BASE_URL`


## Сборка и запуск

```bash
./mvnw -pl booking-service -am clean package
```

```bash
./mvnw -pl booking-service -am spring-boot:run
```

## Docker

```bash
docker compose --env-file .env up --build -d
```

Приложение по умолчанию доступно на порту `8084`.
