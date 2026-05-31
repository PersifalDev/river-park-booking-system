# River Park Booking System

Микросервисная система автоматизации бронирования номеров для отеля River Park в Новосибирске.

## Состав системы

| Модуль | Порт | Назначение |
| --- | ---: | --- |
| `user-service` | `8083` | Регистрация, авторизация, JWT |
| `booking-service` | `8084` | Оркестрация бронирования, inventory, HOLD, подтверждение, промокоды |
| `catalog-service` | `8085` | Категории номеров, услуги, правила проживания, фото |
| `telegram-bot` | `8086` | Пользовательский интерфейс |
| `payment-service` | `8087` | Учебная оплата без эквайринга, подтверждение оплаты на месте |
| `notification-service` | `8088` | Уведомления пользователя |
| `common-libs` | - | Общие DTO, Kafka events, security и page utilities |
| `infra` | - | Общий Docker Compose стенд |

## Основной сценарий

1. Пользователь выбирает даты, гостей и категорию номера.
2. `booking-service` проверяет ограничения гостей, доступность и цену.
3. `booking-service` удерживает inventory и переводит бронь в `HOLD`.
4. `payment-service` создает платежное намерение для оплаты на месте.
5. `notification-service` сохраняет уведомления.
6. Пользователь подтверждает бронь.
7. `payment-service` публикует подтверждение, `booking-service` переводит бронь в `CONFIRMED`.
8. После выезда inventory освобождается планировщиком.

`booking-service` является оркестратором саги. Бот не управляет состоянием бизнес-процесса, он только вызывает API и показывает результат пользователю.

## Технические решения

| Задача | Решение |
| --- | --- |
| Конкурентное бронирование | Inventory rows создаются заранее через `INSERT ... ON CONFLICT DO NOTHING`, затем перечитываются `SELECT ... FOR UPDATE` |
| Повторные запросы | `Idempotency-Key` на `POST /booking` |
| Защита API бронирования | Sliding-window rate limiter |
| Сетевые сбои | Timeout и circuit breaker для booking -> catalog/user |
| Kafka надежность | Transactional outbox в `booking-service` |
| Асинхронная цепочка брони | Virtual threads для внешних HTTP-задач |
| Документация API | Springdoc OpenAPI + Swagger UI |

## Требования

| Инструмент | Версия |
| --- | --- |
| Java | `25` |
| Maven | Wrapper из проекта |
| Docker | Docker Desktop / Docker Engine |
| Docker Compose | v2 |

## Запуск всей системы

```powershell
cd infra
docker compose --env-file .env up --build -d
```

Проверка конфигурации:

```powershell
docker compose --env-file infra\.env -f infra\docker-compose.yaml config --quiet
```

## Локальная сборка

```powershell
.\mvnw.cmd -DskipTests compile
```

## Swagger UI

| Сервис | URL |
| --- | --- |
| User | `http://localhost:8083/swagger-ui.html` |
| Booking | `http://localhost:8084/swagger-ui.html` |
| Catalog | `http://localhost:8085/swagger-ui.html` |
| Telegram Bot | `http://localhost:8086/swagger-ui.html` |
| Payment | `http://localhost:8087/swagger-ui.html` |
| Notification | `http://localhost:8088/swagger-ui.html` |

## Общие переменные

| Переменная | Назначение |
| --- | --- |
| `JWT_SECRET_KEY` | Секрет подписи JWT |
| `JWT_LIFETIME` | Время жизни JWT в миллисекундах |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers |
| `SPRINGDOC_API_DOCS_ENABLED` | Включить `/v3/api-docs` |
| `SPRINGDOC_SWAGGER_UI_ENABLED` | Включить Swagger UI |
| `SPRINGDOC_SWAGGER_UI_PATH` | Путь Swagger UI |

Все `.env` добавлены в `.gitignore`, чтобы секреты не попадали в репозиторий.
