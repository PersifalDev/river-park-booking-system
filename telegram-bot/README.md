# telegram-bot

Telegram-интерфейс River Park Booking System. Бот показывает каталог, создает брони, отображает активные и несостоявшиеся брони, уведомления и промокоды.

## Сценарии

| Сценарий | Описание |
| --- | --- |
| Подбор номера | Пользователь выбирает даты, гостей и категорию |
| Мои брони | Активные брони с пагинацией |
| Несостоявшиеся брони | Отмененные, истекшие и failed-брони |
| Подтверждение брони | Подтверждение HOLD-брони |
| Промокоды | Бот показывает промокод после бронирования, пользователь может применить его в следующей брони |

Swagger UI: `http://localhost:8086/swagger-ui.html`

## Основные env

| Переменная | Пример | Назначение |
| --- | --- | --- |
| `TELEGRAM_BOT_TOKEN` | `123:token` | Токен Telegram |
| `TELEGRAM_BOT_USERNAME` | `river_park_booking_bot` | Username бота |
| `CATALOG_SERVICE_URL` | `http://catalog-service:8085` | URL catalog-service |
| `BOOKING_SERVICE_URL` | `http://booking-service:8084` | URL booking-service |
| `PAYMENT_SERVICE_URL` | `http://payment-service:8087` | URL payment-service |
| `USER_SERVICE_URL` | `http://user-service:8083` | URL user-service |
| `NOTIFICATION_SERVICE_URL` | `http://notification-service:8088` | URL notification-service |

## Запуск

```powershell
.\mvnw.cmd -pl telegram-bot -am spring-boot:run
```

```powershell
cd telegram-bot
docker compose --env-file .env up --build -d
```
