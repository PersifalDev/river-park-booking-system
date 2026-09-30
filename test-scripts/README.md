# Проверки River Park

Актуальный протокол и команды находятся в [research/README.md](research/README.md). Исследовательские скрипты используют отдельный Docker-проект `river-park-research`, новые наборы данных и синтетических пользователей. Метрики, исходные журналы и Markdown-результаты сохраняются в `research/results/`.

```powershell
# Maven/JUnit в Java 25, затем корректность и короткие серии двух режимов и двух исполнителей
& .\test-scripts\research\verify.ps1 -Pilot

# Основное сравнение архитектур
& .\test-scripts\research\run.ps1 -Comparison modes -Repeats 5 -TargetRate 2 -Duration 60s

# Отдельное сравнение исполнителей внутри ASYNC
& .\test-scripts\research\run.ps1 -Comparison threads -Repeats 5 -TargetRate 2 -Duration 60s
```

Совместимость старых точек входа:

| Файл | Текущее действие |
|---|---|
| smoke_test.ps1 | По одному измерению SYNC/ASYNC с отдельным прогревом |
| compare_modes.ps1 | Один повтор platform/virtual при одинаковых лимитах |
| run_thread_matrix.ps1 | Серия повторов platform/virtual |
| work-modes/compare_work_modes.ps1 | Серия SYNC/ASYNC |
| create_and_poll.js, work-modes/create_booking.js | Общая реализация research/flow.js |
| summarize_thread_results.ps1, work-modes/summarize_results.ps1 | Общая статистика research/summarize.ps1 |

Для старых обёрток `Token` больше не нужен: тестовые пользователи создаются в исследовательском стенде. Старые параметры URL не выбирают исходный рабочий стенд. Полный бизнес-результат теперь включает HOLD, платёж PENDING и уведомление PAYMENT_PENDING; быстрый HTTP-ответ сам по себе успехом не считается.

Фактический статус проверок: [docs/research/validation.md](../docs/research/validation.md). До завершения Docker-пилота подготовленные сценарии нельзя считать успешно испытанными.
