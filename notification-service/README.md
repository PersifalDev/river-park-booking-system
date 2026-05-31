# notification-service

Сервис уведомлений River Park: принимает события из Kafka, сохраняет уведомления и отдает их пользователю.

## API

| Метод | URL | Описание |
| --- | --- | --- |
| `GET` | `/notifications` | Все уведомления пользователя |
| `GET` | `/notifications/unread` | Непрочитанные уведомления |
| `PATCH` | `/notifications/{notificationId}/read` | Отметить одно уведомление прочитанным |
| `PATCH` | `/notifications/read-all` | Отметить все уведомления прочитанными |

Swagger UI: `http://localhost:8088/swagger-ui.html`

## Основные env

| Переменная | Пример | Назначение |
| --- | --- | --- |
| `NOTIFICATION_DB_URL` | `jdbc:postgresql://localhost:5439/notification_db` | PostgreSQL URL |
| `NOTIFICATION_SERVER_PORT` | `8088` | HTTP порт |
| `BOOKING_EVENTS_TOPIC` | `bookings-topic` | Topic броней |
| `PAYMENT_EVENTS_TOPIC` | `payments-topic` | Topic платежей |
| `NOTIFICATION_EVENTS_TOPIC` | `notifications-topic` | Topic direct notifications |

## Запуск

```powershell
.\mvnw.cmd -pl notification-service -am spring-boot:run
```

```powershell
cd notification-service
docker compose --env-file .env up --build -d
```
