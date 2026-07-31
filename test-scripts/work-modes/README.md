# Нагрузочное сравнение `WORK_MODE=SYNC/ASYNC`

Этот набор тестов отделён от сравнения platform/virtual threads. Здесь меняется только
способ межсервисного взаимодействия:

- `SYNC`: Booking Service синхронно обрабатывает задачу и вызывает Payment/Notification по REST;
- `ASYNC`: Booking Service отвечает после принятия заявки, а события доставляются через
  transactional outbox и Kafka.

Сценарий измеряет три разные задержки:

1. `booking_response_latency_ms` — время HTTP-ответа `POST /booking`;
2. `booking_hold_latency_ms` — время до статуса `HOLD`;
3. `notification_completion_latency_ms` — время до появления связанного уведомления;
4. `business_completion_latency_ms` — время до готовности `HOLD`, платежа `PENDING` и уведомления.

Это не позволяет выдать быстрый HTTP-ответ async-режима за завершение всей бизнес-операции.

Пример запуска из корня проекта:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass `
  -File .\test-scripts\work-modes\compare_work_modes.ps1 `
  -Token $token `
  -CategoryIds $categoryIds `
  -Repeats 5 `
  -TargetRate 10 `
  -Duration "60s"
```

Порядок режимов чередуется между повторами, перед измерением выполняется прогрев, а после
каждого запуска скрипт ждёт опустошения task/outbox-очередей. JWT в результаты не записывается.
Для каждого запуска сохраняется `summary.json`, а параметры эксперимента и git revision —
в соседнем `run-manifest.json`. После серии автоматически создаются
`aggregate-summary.json` и `aggregate-summary.csv` с медианой, средним значением
и 95%-м доверительным интервалом Стьюдента.
