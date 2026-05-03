# telegram-bot

Telegram-бот River Park. Показывает номера, услуги, правила проживания и контакты. Подготовлена конфигурация для интеграции с booking-service и payment-service по сценарию подтверждения оплаты на месте.

## API

Команды и сценарии:
- `/start`
- `/help`
- `Подобрать номер`
- `Все номера`
- `Найти номер`
- `Услуги`
- `Найти услугу`
- `Правила проживания`
- `Контакты`

Платежный сценарий для MVP:
- показать пользователю рассчитанную сумму проживания
- предложить кнопку подтверждения намерения оплатить на месте
- после подтверждения вызвать `payment-service`
- после отмены вызвать `payment-service` cancel


## Конфигурация env

- `TELEGRAM_BOT_PORT`
- `TELEGRAM_BOT_USERNAME`
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_ADMIN_CONTACT`
- `CATALOG_SERVICE_URL`
- `BOOKING_SERVICE_URL`
- `PAYMENT_SERVICE_URL`
- `USER_SERVICE_URL`


## Сборка и запуск

```bash
./mvnw -pl telegram-bot -am clean package
```

```bash
./mvnw -pl telegram-bot -am spring-boot:run
```

## Docker

```bash
docker compose --env-file .env up --build -d
```

Приложение по умолчанию доступно на порту `8086`.
