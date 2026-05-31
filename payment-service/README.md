# payment-service

Сервис учебной оплаты без эквайринга. После HOLD-брони создает платежное намерение и позволяет подтвердить оплату на месте.

## API

| Метод | URL | Описание |
| --- | --- | --- |
| `GET` | `/payments` | Платежи пользователя |
| `GET` | `/payments/booking/{bookingId}` | Платеж по брони |
| `PATCH` | `/payments/booking/{bookingId}/confirm` | Подтвердить оплату |
| `PATCH` | `/payments/booking/{bookingId}/cancel` | Отменить подтверждение |

Swagger UI: `http://localhost:8087/swagger-ui.html`

## Kafka

| Направление | События |
| --- | --- |
| Consumer | `BOOKING_HOLD_CREATED`, `BOOKING_CANCELLED`, `BOOKING_EXPIRED` |
| Producer | `PAYMENT_PENDING`, `PAYMENT_CONFIRMED`, `PAYMENT_CANCELLED`, `PAYMENT_FAILED` |

## Основные env

| Переменная | Пример | Назначение |
| --- | --- | --- |
| `PAYMENT_DB_URL` | `jdbc:postgresql://localhost:5438/payment_db` | PostgreSQL URL |
| `PAYMENT_SERVER_PORT` | `8087` | HTTP порт |
| `PAYMENT_EVENTS_TOPIC` | `payments-topic` | Topic платежей |
| `BOOKING_EVENTS_TOPIC` | `bookings-topic` | Topic броней |
| `PAYMENT_CONTACT_PHONE` | `+7 (383) 349-50-50` | Телефон отеля |

## Запуск

```powershell
.\mvnw.cmd -pl payment-service -am spring-boot:run
```

```powershell
cd payment-service
docker compose --env-file .env up --build -d
```
