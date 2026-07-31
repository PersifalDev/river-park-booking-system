# Нагрузочное тестирование booking-service

Первая версия проверяет основной асинхронный путь:

1. `POST /booking`;
2. polling `GET /booking/{id}` до перехода из `CREATED`;
3. измерение времени до `HOLD` или `FAILED`;
4. отмена успешного `HOLD`, чтобы освобождать inventory и создавать нагрузку на outbox.

Один и тот же k6-сценарий запускается с обычным и виртуальным
`externalHttpThreadPool`. Результаты сравнимы только при одинаковых VU,
длительности, данных, размере task pool, Hikari pool и ресурсах контейнера.

## Где переключаются virtual threads

Настройка находится в
`booking-service/src/main/resources/application.yaml`:

```yaml
app:
  booking:
    task:
      dispatcher:
        thread-pool-size: ${BOOKING_TASK_DISPATCHER_THREAD_POOL_SIZE:16}
        queue-capacity: ${BOOKING_TASK_DISPATCHER_QUEUE_CAPACITY:30}
      external-http:
        virtual-threads-enabled: ${BOOKING_EXTERNAL_HTTP_VIRTUAL_THREADS_ENABLED:true}
        virtual-max-concurrency: ${BOOKING_EXTERNAL_HTTP_VIRTUAL_MAX_CONCURRENCY:32}
        platform:
          thread-pool-size: ${BOOKING_EXTERNAL_HTTP_PLATFORM_THREAD_POOL_SIZE:32}
          queue-capacity: ${BOOKING_EXTERNAL_HTTP_PLATFORM_QUEUE_CAPACITY:64}
  task-execution:
    poller:
      poll-interval-ms: ${TASK_EXEC_POOL_INTERVAL_MS:500}
      batch-size: ${TASK_EXEC_POOL_BATCH_SIZE:50}
```

Для Docker Compose переменная передаётся в `booking-service`:

```text
BOOKING_EXTERNAL_HTTP_VIRTUAL_THREADS_ENABLED=true   # virtual
BOOKING_EXTERNAL_HTTP_VIRTUAL_THREADS_ENABLED=false  # platform
```

Важно: это **не** `spring.threads.virtual.enabled` и не виртуальные потоки
Tomcat. Флаг меняет только executor `externalHttpThreadPool`, на котором
выполняются блокирующие обращения booking-service к catalog/user и части
асинхронной booking-цепочки.

`taskDispatcherThreadPool` в обоих режимах остаётся фиксированным platform
pool. По умолчанию он имеет 16 потоков и очередь 30. Так как dispatcher ждёт
завершения цепочки, этот пул может стать bottleneck раньше внешнего executor.

В platform-режиме внешний executor имеет 32 потока и очередь 64. В
virtual-режиме задачи получают отдельные virtual threads, но concurrency
limiter разрешает одновременно выполнять не более 32 внешних задач. Поэтому
оба режима сравниваются при одинаковом максимуме внешней конкурентности.

## Установка k6

k6 v2.0.0 установлен project-local:

```powershell
.\.tools\k6\k6-v2.0.0-windows-amd64\k6.exe version
```

Повторная установка:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\test-scripts\install-k6.ps1
```

`.tools` исключён из Git. Альтернативные официальные способы — Winget,
Chocolatey или Docker.

## Подготовка

Все команды ниже выполняются из корня репозитория:

```powershell
Set-Location C:\Users\harit\Desktop\Java\river-park-booking-system
```

Запустить стенд вместе с observability:

```powershell
docker compose --env-file infra\.env -f infra\docker-compose.yaml `
  --profile observability up --build -d
```

Проверить контейнеры и health-check:

```powershell
docker compose --env-file infra\.env -f infra\docker-compose.yaml ps
Invoke-RestMethod http://localhost:8084/actuator/health
```

Если тестового пользователя ещё нет, зарегистрировать его:

```powershell
$registration = @{
  login = "k6_load_test"
  key = "LoadTest123!"
  personalDataConsentAccepted = $true
  privacyPolicyAccepted = $true
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8083/users" `
  -ContentType "application/json" `
  -Body $registration
```

Повторная регистрация существующего пользователя вернёт ошибку — это нормально.
Получить JWT и оставить его только в переменной текущей PowerShell-сессии:

```powershell
$credentials = @{
  login = "k6_load_test"
  password = "LoadTest123!"
} | ConvertTo-Json

$auth = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8083/users/auth" `
  -ContentType "application/json" `
  -Body $credentials

$token = $auth.accessToken
```

Получить актуальные идентификаторы категорий:

```powershell
$rooms = Invoke-RestMethod `
  -Uri "http://localhost:8085/api/v1/catalog/rooms?pageNumber=0&pageSize=100"

$categoryIds = ($rooms.content | ForEach-Object { $_.id }) -join ","
$categoryIds
```

JWT не записывается в репозиторий. Не выводите `$token` в терминал и не
добавляйте его в README или `.env`.

## Какие тесты есть

### 1. Smoke test

`smoke_test.ps1` выполняет ровно одну итерацию в текущем режиме контейнера:

1. health-check booking-service;
2. создание бронирования;
3. polling до выхода из `CREATED`;
4. проверка перехода в `HOLD`;
5. отмена бронирования.

Запуск:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\test-scripts\smoke_test.ps1 `
  -Token $token `
  -CategoryIds $categoryIds
```

У smoke нет параметров `-Vus` и `-Iterations`: это намеренно одиночная
функциональная проверка. Результат сохраняется в:

```text
test-scripts/results/<timestamp>/smoke/summary.json
```

### 2. Фиксированное число итераций

Команда ниже автоматически выполняет оба режима: сначала platform, затем
virtual. Пример: 1 VU и 10 бронирований в каждом режиме:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\test-scripts\compare_modes.ps1 `
  -Token $token `
  -Vus 1 `
  -Iterations 10 `
  -CategoryIds $categoryIds `
  -CooldownSeconds 5
```

`-Iterations` — общее число итераций на режим, а не число на каждого VU.

### 3. A/B по длительности

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\test-scripts\compare_modes.ps1 `
  -Token $token `
  -Vus 10 `
  -Duration "60s" `
  -CategoryIds $categoryIds `
  -CooldownSeconds 10
```

Скрипт для каждого режима:

- переключая режим, пересоздаёт `booking-service`, не трогая БД;
- отключает локальный booking rate limit, иначе тест быстро упрётся в
  `20 create/min` и будет измерять ответы `429`;
- временно пересоздаёт `catalog-service` с отключённым catalog rate limit,
  потому что одна booking-итерация делает несколько внутренних catalog-вызовов;
- ждёт `/actuator/health`;
- запускает один и тот же `create_and_poll.js`;
- сохраняет `summary.json` в
  `test-scripts/results/<timestamp>/<mode>/`.

В блоке `finally` catalog-service пересоздаётся ещё раз с обычным
`CATALOG_RATE_LIMIT_ENABLED=true`. Если PowerShell-процесс был принудительно
завершён, восстановите его вручную:

```powershell
docker compose --env-file infra\.env -f infra\docker-compose.yaml `
  up -d --no-deps --force-recreate catalog-service
```

Для каждого A/B-запуска создаются:

```text
test-scripts/results/<timestamp>/platform/summary.json
test-scripts/results/<timestamp>/virtual/summary.json
```

Папка `results` исключена из Git. Найти последний запуск:

```powershell
$lastResult = Get-ChildItem .\test-scripts\results -Directory |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 1

$lastResult.FullName
Get-Content -Raw -Encoding UTF8 `
  (Join-Path $lastResult.FullName "platform\summary.json")
Get-Content -Raw -Encoding UTF8 `
  (Join-Path $lastResult.FullName "virtual\summary.json")
```

### 4. Матрица нагрузки

Начинайте со ступеней, не запускайте сразу 100 VU:

```powershell
# 1 VU, ровно 10 итераций на режим
powershell -NoProfile -ExecutionPolicy Bypass -File .\test-scripts\compare_modes.ps1 `
  -Token $token -Vus 1 -Iterations 10 -CategoryIds $categoryIds -CooldownSeconds 5

# 10 VU, 60 секунд на режим
powershell -NoProfile -ExecutionPolicy Bypass -File .\test-scripts\compare_modes.ps1 `
  -Token $token -Vus 10 -Duration "60s" -CategoryIds $categoryIds -CooldownSeconds 10

# 30 VU, 120 секунд на режим
powershell -NoProfile -ExecutionPolicy Bypass -File .\test-scripts\compare_modes.ps1 `
  -Token $token -Vus 30 -Duration "120s" -CategoryIds $categoryIds -CooldownSeconds 30

# 50 VU, 180 секунд на режим
powershell -NoProfile -ExecutionPolicy Bypass -File .\test-scripts\compare_modes.ps1 `
  -Token $token -Vus 50 -Duration "180s" -CategoryIds $categoryIds -CooldownSeconds 30

# 100 VU, 300 секунд на режим
powershell -NoProfile -ExecutionPolicy Bypass -File .\test-scripts\compare_modes.ps1 `
  -Token $token -Vus 100 -Duration "300s" -CategoryIds $categoryIds -CooldownSeconds 60
```

Переходите на следующую ступень, только если:

- `http_req_failed < 1%`;
- `successful_iterations > 95%`;
- `terminal_timeouts_total = 0`;
- backlog после теста возвращается к нулю;
- Hikari `pending = 0`;
- нет постоянной насыщенности CPU, executor queue и pool.

После теста `catalog-service` и `booking-service` пересоздаются с обычными
значениями из `infra/.env`. При текущем `.env` это virtual-режим и включённые
rate limits. Для ручного переключения на platform:

```powershell
$env:BOOKING_EXTERNAL_HTTP_VIRTUAL_THREADS_ENABLED = "false"
docker compose --env-file infra\.env -f infra\docker-compose.yaml `
  up -d --no-deps --force-recreate booking-service
Remove-Item Env:BOOKING_EXTERNAL_HTTP_VIRTUAL_THREADS_ENABLED
```

Не запускайте сравнивающий скрипт против production: сценарий создаёт и
отменяет реальные бронирования.

## Быстрая пересборка

Пересобрать и перезапустить только booking-service:

```powershell
docker compose --env-file infra\.env -f infra\docker-compose.yaml `
  up -d --build --no-deps booking-service
```

Применить только настройки Compose/`.env`, не пересобирая image:

```powershell
docker compose --env-file infra\.env -f infra\docker-compose.yaml `
  up -d --no-deps --force-recreate booking-service booking-postgres
```

Посмотреть фактически применённые лимиты:

```powershell
docker inspect booking-service `
  --format 'CPU={{.HostConfig.NanoCpus}} Memory={{.HostConfig.Memory}}'
docker inspect booking-postgres `
  --format 'CPU={{.HostConfig.NanoCpus}} Memory={{.HostConfig.Memory}}'
docker stats --no-stream booking-service booking-postgres
```

Текущие экспериментальные лимиты задаются в `infra/.env`:

```text
BOOKING_SERVICE_CPUS=1.00
BOOKING_SERVICE_MEMORY_LIMIT=768m
BOOKING_POSTGRES_CPUS=0.50
BOOKING_POSTGRES_MEMORY_LIMIT=384m
```

## Параметры k6

| Переменная | Default | Назначение |
| --- | ---: | --- |
| `BASE_URL` | `http://localhost:8084` | booking-service |
| `TOKEN` | — | JWT пользователя, обязателен |
| `VUS` | `10` | параллельные k6 virtual users |
| `DURATION` | `60s` | длительность |
| `CATEGORY_IDS` | `1,2,3` | существующие категории |
| `POLL_INTERVAL_MS` | `250` | интервал polling |
| `TERMINAL_TIMEOUT_MS` | `30000` | ожидание выхода из `CREATED` |
| `DATE_SPREAD_DAYS` | `180` | разброс дат для снижения искусственных конфликтов inventory |
| `CANCEL_HOLDS` | `true` | освобождать HOLD после измерения |

Основные k6-метрики:

- `create_latency_ms` — синхронная часть `POST /booking`;
- `terminal_latency_ms` — end-to-end `POST → HOLD/FAILED`;
- `successful_iterations` — доля бронирований, дошедших до `HOLD`;
- `terminal_timeouts_total` — застрявшие в `CREATED`;
- стандартные `http_req_duration`, `http_req_failed`, `http_reqs`.

Для Trend-метрик выводятся `avg`, `min`, `med`, `max`, `p(90)`, `p(95)` и
`p(99)`. `p(99)` имеет смысл оценивать на продолжительных тестах с тысячами
запросов; при 100 измерениях это практически один самый медленный запрос.

Threshold — условие успешности теста:

- `http_req_failed rate < 1%`;
- `create_latency_ms p95 < 1000 ms`;
- `terminal_latency_ms p95 < 10000 ms`;
- `successful_iterations > 95%`;
- `terminal_timeouts_total = 0`.

Нарушенный performance threshold даёт k6 exit code `99`, но
`compare_modes.ps1` всё равно запускает второй режим и сохраняет оба отчёта.

Rate-limit проверяется отдельными integration/security тестами. Обычный
performance A/B не должен упираться в `429`, иначе измеряется лимитер, а не
производительность thread pools, БД и внешних вызовов.

## Как искать bottleneck

Откройте Grafana: `http://localhost:3000` (локально default `admin/admin`) и
dashboard **River Park / Booking Load Test**.

Сигналы:

| Симптом | Метрики | Вероятная причина |
| --- | --- | --- |
| backlog `new/retryable` растёт, poll batch постоянно равен limit | `async_booking_task_backlog`, `booking_task_poller_last_batch_size`, `rate(booking_task_poller_tasks_total[1m])` | текущих poll interval 500 ms / batch 50 недостаточно либо dispatcher насыщен |
| executor active = pool size, queue растёт | `executor_active_threads`, `executor_queued_tasks` | мало platform threads или медленные blocking calls |
| Hikari active = max, pending > 0 | `hikaricp_connections_active/max/pending` | pool исчерпан или транзакции/SQL медленные |
| HTTP p95 растёт без очереди/Hikari saturation | `http_server_requests_seconds` и traces | медленный controller/service/external dependency |
| CPU около лимита, throughput перестал расти | `process_cpu_usage`, cAdvisor | CPU bottleneck; virtual threads не помогут |
| GC pause/heap растут | `jvm_gc_pause_seconds`, `jvm_memory_used_bytes` | allocation/heap/GC bottleneck |
| outbox `new` растёт | `booking_outbox_backlog`, `booking_outbox_last_batch_size` | Kafka медленная/недоступна либо outbox batch 10 и delay 5 s малы |
| много `FAILED_RETRYABLE` | task backlog + Loki + Tempo | timeout/circuit breaker/ошибка catalog или user-service |
| PostgreSQL locks и latency растут на одинаковых датах | DB/trace + inventory conflicts | row lock contention, а не thread pool |
| Redis latency/ошибки растут | Loki/Tempo | distributed lock, cache или rate limiter |

Сначала сравните default-конфигурацию. Затем меняйте **один** параметр за
запуск: `DispatcherPoolSize`, `DispatcherQueueCapacity`,
`ExternalPlatformPoolSize`, `ExternalPlatformQueueCapacity`,
`ExternalVirtualMaxConcurrency`, `HikariPoolSize`, `PollBatchSize` или
`PollIntervalMs`. Иначе нельзя определить причину улучшения.

## Куда идут логи и что делает стек

- Приложения пишут structured logs в stdout контейнеров.
- Docker хранит их через `json-file` с ротацией `10m × 3`.
- Alloy читает Docker logs и отправляет их в Loki.
- Prometheus каждые 15 секунд забирает `/actuator/prometheus`.
- Tempo хранит traces, которые приложения отправляют через OTLP в Alloy.
- Grafana — единая UI над Prometheus (metrics), Loki (logs) и Tempo (traces).

Полезный LogQL в Grafana Explore:

```logql
{service="booking-service"} |= "Picked tasks for polling"
```

```logql
{service="booking-service"} |~ "failed|timeout|Circuit"
```

Логи конкретного контейнера без Grafana:

```powershell
docker compose --env-file infra\.env -f infra\docker-compose.yaml `
  logs -f --tail=200 booking-service
```
