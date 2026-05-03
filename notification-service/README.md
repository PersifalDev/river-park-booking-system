# notification-service

Сервис уведомлений. Слушает booking- и payment-события, сохраняет уведомления в БД и отдает непрочитанные уведомления пользователю.

## API

- `GET /notifications` — получить все уведомления текущего пользователя
- `GET /notifications/unread` — получить непрочитанные уведомления
- `PATCH /notifications/{notificationId}/read` — отметить уведомление как прочитанное
- `PATCH /notifications/read-all` — отметить все уведомления как прочитанные

Kafka consumer:
- `BOOKING_CREATED`
- `BOOKING_HOLD_CREATED`
- `BOOKING_CONFIRMED`
- `BOOKING_CANCELLED`
- `BOOKING_EXPIRED`
- `BOOKING_FAILED`
- `PAYMENT_INVOICE_CREATED`
- `PAYMENT_PENDING`
- `PAYMENT_CONFIRMED`
- `PAYMENT_CANCELLED`
- `PAYMENT_FAILED`


## Конфигурация env

- `NOTIFICATION_DB_URL`
- `NOTIFICATION_DB_USERNAME`
- `NOTIFICATION_DB_PASSWORD`
- `NOTIFICATION_SERVER_PORT`
- `JWT_SECRET_KEY`
- `JWT_LIFETIME`
- `KAFKA_HOST_PORT`
- `BOOKING_EVENTS_TOPIC`
- `PAYMENT_EVENTS_TOPIC`


## Сборка и запуск

```bash
./mvnw -pl notification-service -am clean package
```

```bash
./mvnw -pl notification-service -am spring-boot:run
```

## Docker

```bash
docker compose --env-file .env up --build -d
```

Приложение по умолчанию доступно на порту `8088`.
