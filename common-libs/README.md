# common-libs

Общий Maven-модуль River Park Booking System. Не запускается как отдельный сервис.

## Содержимое

| Пакет | Назначение |
| --- | --- |
| `dto` | Общие DTO между сервисами |
| `dto.kafka` | Kafka events и payload |
| `security` | Общие security-модели |
| `exception` | Общие исключения |
| `utils.pages` | Общие page/filter utilities |

## Сборка

```powershell
.\mvnw.cmd -pl common-libs clean package
```

Обычно модуль собирается вместе с сервисом:

```powershell
.\mvnw.cmd -pl booking-service -am clean package
```
