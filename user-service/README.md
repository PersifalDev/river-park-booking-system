# user-service

Сервис пользователей River Park: регистрация, аутентификация и выдача JWT.

## API

| Метод | URL | Описание |
| --- | --- | --- |
| `POST` | `/users` | Зарегистрировать пользователя |
| `POST` | `/users/auth` | Получить JWT |
| `GET` | `/users/{id}` | Получить пользователя по id |

Swagger UI: `http://localhost:8083/swagger-ui.html`

## Основные env

| Переменная | Пример | Назначение |
| --- | --- | --- |
| `USER_DB_URL` | `jdbc:postgresql://localhost:5435/user_db` | PostgreSQL URL |
| `USER_DB_USERNAME` | `postgres` | Пользователь БД |
| `USER_DB_PASSWORD` | `postgres` | Пароль БД |
| `USER_SERVER_PORT` | `8083` | HTTP порт |
| `JWT_SECRET_KEY` | `change-me` | Секрет JWT |
| `JWT_LIFETIME` | `86400000` | TTL JWT |

## Пример `.env`

```env
USER_DB_USERNAME=postgres
USER_DB_PASSWORD=postgres
USER_DB_URL=jdbc:postgresql://localhost:5435/user_db
USER_SERVER_PORT=8083
JWT_SECRET_KEY=change-me
JWT_LIFETIME=86400000
```

## Запуск

```powershell
.\mvnw.cmd -pl user-service -am spring-boot:run
```

```powershell
cd user-service
docker compose --env-file .env up --build -d
```
