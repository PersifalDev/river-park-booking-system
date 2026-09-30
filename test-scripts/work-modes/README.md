# Сравнение SYNC и ASYNC

Основная инструкция: [../research/README.md](../research/README.md). `compare_work_modes.ps1` делегирует общий исследовательский runner и сохраняет его протокол.

```powershell
& .\test-scripts\research\run.ps1 -Comparison modes -Repeats 5 -TargetRate 2 -Duration 60s
```

Для каждой итерации сохраняются время HTTP-ответа, время HOLD, время PENDING платежа, время PAYMENT_PENDING уведомления и завершение всех трёх состояний. Дополнительно учитываются отказ в принятии, неуспех после принятия, таймаут, dropped_iterations и очистка. Каждая серия создаёт RESULTS.md, исходные данные и парную статистику.

SYNC и ASYNC используют одинаковый бизнес-процесс и входной поток; тип потоков и лимиты фиксированы. Для platform/virtual предусмотрена отдельная серия `-Comparison threads` внутри ASYNC. Протокол допускает синхронные вызовы в асинхронном процессе; отдельный HYBRID не вводится.
