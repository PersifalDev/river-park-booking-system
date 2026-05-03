# catalog-service

Справочный сервис отеля River Park: категории номеров, фотографии, услуги и правила проживания.

## API

- `GET /api/v1/catalog/rooms`
- `GET /api/v1/catalog/rooms/{id}`
- `POST /api/v1/catalog/rooms/search`
- `GET /api/v1/catalog/rooms/{categoryId}/photos`
- `GET /api/v1/catalog/services`
- `GET /api/v1/catalog/services/{id}`
- `GET /api/v1/catalog/services/by-type/{type}`
- `GET /api/v1/catalog/rules/document`
- `GET /api/v1/catalog/rules/document/file`
- `GET /api/v1/internal/catalog/rooms/{id}`


## Конфигурация env

- `POSTGRES_CATALOG_SERVICE_PORT`
- `POSTGRES_CATALOG_SERVICE_USER`
- `POSTGRES_CATALOG_SERVICE_PASSWORD`
- `POSTGRES_CATALOG_SERVICE_DB`
- `REDIS_CATALOG_SERVICE_PORT`
- `CATALOG_SERVICE_PORT`
- `APP_BASE_DIR`
- `CATALOG_PUBLIC_BASE_URL`


## Сборка и запуск

```bash
./mvnw -pl catalog-service -am clean package
```

```bash
./mvnw -pl catalog-service -am spring-boot:run
```

## Docker

```bash
docker compose --env-file .env up --build -d
```

Приложение по умолчанию доступно на порту `8085`.
