# user-service

Сервис пользователей и JWT-аутентификации.

## API

- `POST /users` — регистрация пользователя
- `POST /users/auth` — получение JWT токена
- `GET /users/{id}` — получение пользователя по id


## Конфигурация env

- `USER_DB_URL`
- `USER_DB_USERNAME`
- `USER_DB_PASSWORD`
- `USER_SERVER_PORT`
- `JWT_SECRET_KEY`
- `JWT_LIFETIME`


## Сборка и запуск

```bash
./mvnw -pl user-service -am clean package
```

```bash
./mvnw -pl user-service -am spring-boot:run
```

## Docker

```bash
docker compose --env-file .env up --build -d
```

Приложение по умолчанию доступно на порту `8083`.
