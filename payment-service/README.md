# payment-service

Сервис учебной оплаты без эквайринга. Создает запись PENDING по событию HOLD, подтверждает намерение оплатить на месте и публикует payment-события в Kafka.

## API

- `GET /payments` — получить платежи текущего пользователя
- `GET /payments/booking/{bookingId}` — получить платеж по броне
- `PATCH /payments/booking/{bookingId}/confirm` — подтвердить намерение оплатить на месте
- `PATCH /payments/booking/{bookingId}/cancel` — отменить подтверждение

Kafka consumer:
- `BOOKING_HOLD_CREATED`
- `BOOKING_CANCELLED`
- `BOOKING_EXPIRED`

Kafka producer:
- `PAYMENT_PENDING`
- `PAYMENT_CONFIRMED`
- `PAYMENT_CANCELLED`


## Конфигурация env

- `PAYMENT_DB_URL`
- `PAYMENT_DB_USERNAME`
- `PAYMENT_DB_PASSWORD`
- `PAYMENT_SERVER_PORT`
- `JWT_SECRET_KEY`
- `JWT_LIFETIME`
- `KAFKA_HOST_PORT`
- `BOOKING_EVENTS_TOPIC`
- `PAYMENT_EVENTS_TOPIC`
- `PAYMENT_CONTACT_PHONE`
- `PAYMENT_CONTACT_COMMENT`


## Сборка и запуск

```bash
./mvnw -pl payment-service -am clean package
```

```bash
./mvnw -pl payment-service -am spring-boot:run
```

## Docker

```bash
docker compose --env-file .env up --build -d
```

Приложение по умолчанию доступно на порту `8087`.
