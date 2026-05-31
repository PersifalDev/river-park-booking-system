# catalog-service

Справочник River Park: категории номеров, фотографии, услуги и правила проживания.

## API

| Метод | URL | Описание |
| --- | --- | --- |
| `GET` | `/api/v1/catalog/rooms` | Список категорий |
| `GET` | `/api/v1/catalog/rooms/{id}` | Категория по id |
| `POST` | `/api/v1/catalog/rooms/search` | Поиск категорий |
| `GET` | `/api/v1/catalog/rooms/{categoryId}/photos` | Фото категории |
| `GET` | `/api/v1/catalog/services` | Активные услуги |
| `GET` | `/api/v1/catalog/services/{id}` | Услуга по id |
| `GET` | `/api/v1/catalog/services/by-type/{type}` | Услуга по типу |
| `GET` | `/api/v1/catalog/rules/document` | Метаданные правил |
| `GET` | `/api/v1/catalog/rules/document/file` | PDF правил |
| `GET` | `/api/v1/internal/catalog/rooms/{id}` | Краткая категория для внутренних сервисов |

Swagger UI: `http://localhost:8085/swagger-ui.html`

## Фото

Фото раздаются из `APP_BASE_DIR` через `/static/**`. Сервис не ходит на сайт River Park при каждом запросе, потому что это замедляет карточки и делает систему зависимой от внешнего сайта.

## Основные env

| Переменная | Пример | Назначение |
| --- | --- | --- |
| `CATALOG_SERVICE_DB_URL` | `jdbc:postgresql://localhost:5437/catalogdb` | PostgreSQL URL |
| `POSTGRES_CATALOG_SERVICE_USER` | `catalog` | Пользователь БД |
| `POSTGRES_CATALOG_SERVICE_PASSWORD` | `service` | Пароль БД |
| `CATALOG_SERVICE_REDIS_HOST` | `localhost` | Redis host |
| `CATALOG_SERVICE_REDIS_PORT` | `6381` | Redis port |
| `APP_BASE_DIR` | `./data/static` | Каталог статических файлов |
| `CATALOG_PUBLIC_BASE_URL` | `http://localhost:8085` | Базовый публичный URL |

## Пример `.env`

```env
POSTGRES_CATALOG_SERVICE_USER=catalog
POSTGRES_CATALOG_SERVICE_PASSWORD=service
CATALOG_SERVICE_DB_URL=jdbc:postgresql://localhost:5437/catalogdb
CATALOG_SERVICE_REDIS_HOST=localhost
CATALOG_SERVICE_REDIS_PORT=6381
APP_BASE_DIR=./data/static
CATALOG_PUBLIC_BASE_URL=http://localhost:8085
```

## Запуск

```powershell
.\mvnw.cmd -pl catalog-service -am spring-boot:run
```

```powershell
cd catalog-service
docker compose --env-file .env up --build -d
```
