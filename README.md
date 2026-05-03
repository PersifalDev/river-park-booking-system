# River Park Booking System

Микросервисная система автоматизации бронирования номеров для River Park через Telegram-бота.

## Сервисы

- `user-service` — регистрация, аутентификация, JWT
- `catalog-service` — категории номеров, фото, услуги, правила проживания
- `booking-service` — поиск, создание, HOLD, CONFIRMED, CANCELLED, EXPIRED
- `payment-service` — учебный платеж без эквайринга, подтверждение намерения оплатить на месте
- `notification-service` — прием событий из Kafka, сохранение уведомлений, выдача непрочитанных уведомлений
- `telegram-bot` — пользовательский интерфейс
- `infra` — общий стенд инфраструктуры
- `common-libs` — общие DTO, kafka-события, security-модели и utility

## Бизнес-процесс

1. Пользователь выбирает даты, гостей и категорию в Telegram.
2. `booking-service` проверяет availability, считает сумму и создает бронь в статусе `HOLD`.
3. `booking-service` публикует `BOOKING_HOLD_CREATED`.
4. `payment-service` создает учебную запись `PENDING` без реального эквайринга и публикует `PAYMENT_PENDING`.
5. `notification-service` сохраняет уведомление с суммой проживания и инструкцией, что оплата выполняется при заселении у администратора.
6. Пользователь подтверждает бронь в боте.
7. `payment-service` переводит запись `PENDING -> CONFIRMED` и публикует `PAYMENT_CONFIRMED`.
8. `booking-service` слушает `PAYMENT_CONFIRMED` и переводит бронь `HOLD -> CONFIRMED`.
9. `notification-service` сохраняет итоговое уведомление о подтвержденной брони.
10. Если пользователь отменяет подтверждение, идет `PAYMENT_CANCELLED`, затем `BOOKING_CANCELLED`.
11. Если истек TTL удержания, `booking-service` переводит бронь в `EXPIRED`, освобождает inventory и публикует событие об автоотмене.

## Запуск инфраструктуры

Общий стенд расположен в `infra`.

```bash
cd infra
docker compose --env-file .env up --build -d
```

## Сервисные docker-compose

Для каждого сервиса есть свой `docker-compose.yaml`.
Он поднимает приложение, его БД и необходимые внешние зависимости.

## JWT

Секреты вынесены в `.env` каждого сервиса.
Перед запуском обязательно установи свои реальные значения.

## README по сервисам

- `user-service/README.md`
- `catalog-service/README.md`
- `booking-service/README.md`
- `payment-service/README.md`
- `notification-service/README.md`
- `telegram-bot/README.md`
