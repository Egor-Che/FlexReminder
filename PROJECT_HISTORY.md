# PROJECT_HISTORY — Гибкие напоминания

Сжатая история проекта: архитектура, ключевые решения, что уже работает.
Нужна для продолжения работы в новом чате без потери контекста.

---

## Что за приложение

Android-приложение для гибких напоминаний. Пользователь может задавать
расписание двумя способами:

1. **Интервалы** — «N дней подряд включено, M дней перерыв».
2. **Конкретные даты** — множественный выбор в календаре с переключением
   между месяцами.

Плюс: период [startDate; endDate], точные будильники, переносы (snooze),
беззвучный режим, три темы (Палитра, Монохром, Тёмная), звуки уведомлений,
архив с историей итераций, интерактивный виджет на рабочий стол.

**Публикация:** RuStore (бесплатно, минимум разрешений).

---

## Стек и версии

- **Язык:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **БД:** Room (SQLite)
- **Настройки:** DataStore Preferences
- **Планировщик:** AlarmManager
- **Фоновая работа:** WorkManager
- **Виджет:** RemoteViews (AppWidgetProvider)
- **AGP:** 8.2.2 (НЕ обновлять!)
- **Gradle:** 8.4
- **Kotlin:** 1.9.22
- **Compose Compiler:** 1.5.10
- **compileSdk / targetSdk:** 34
- **minSdk:** 28 (Android 9)
- **JDK:** 17

**Причина фиксации версий:** связка Gradle 8.4 + AGP 8.2.2 + Kotlin 1.9.22 +
Compose Compiler 1.5.10 проверена и работает. Любое автообновление ломает
совместимость с GitHub Actions и требует переработки build.yml.

---

## Разрешения в манифесте

- `POST_NOTIFICATIONS`
- `SCHEDULE_EXACT_ALARM`
- `USE_EXACT_ALARM`
- `RECEIVE_BOOT_COMPLETED`
- `WAKE_LOCK`

Ничего лишнего. Никакого интернета, камер, контактов, геолокации.

---

## Ключевые архитектурные решения

### 1. Разделение логики и планировщика
- `ReminderLogic.kt` — **что** вычислять (активна ли дата, следующее срабатывание).
- `AlarmScheduler.kt` — **как** поставить будильник (AlarmManager).

### 2. Два режима расписания
Enum `ScheduleMode { INTERVAL, CUSTOM_DATES }`.
- В `INTERVAL` — `daysOn`, `daysOff`, `startDate`, `endDate`.
- В `CUSTOM_DATES` — `customDates: Set<Long>` (хранится как CSV-строка).
- Поля не взаимоисключающие: при переключении режима данные не теряются.

### 3. Синхронизация режимов в редакторе
- **Интервалы → Календарь:** даты генерируются из текущих интервалов.
- **Календарь → Интервалы:** распознаём простой паттерн (`detectPattern`).
- Ключ `lastGeneratedKey` защищает ручные правки пользователя.
- Флаг `intervalsTouched` предотвращает автозаполнение календаря 366 датами при создании нового напоминания (фикс #25).

### 4. Каналы уведомлений
- `reminders_loud` — со звуком (встроенная мелодия приложения).
- `reminders_silent` — беззвучный.
- `Reminder.silent` определяет канал.
- Один уникальный звук = один канал. ID канала формируется как `reminders_custom_<sha1(uri).take(8)>`.

### 5. Три темы и пастельные цвета (фикс затемнения)
- Темы: Палитра, Монохром, Тёмная.
- **Проблема:** В Палитре при использовании прозрачности (`alpha < 1f`) карточки начинали выглядеть грязно-серыми из-за смешивания с фоном экрана.
- **Решение:** В `AppThemeColors.kt` мы убрали прозрачность. Вместо этого функция `makePastel()` смешивает базовый цвет с белым в пропорции 30/70. Это дает 100% непрозрачный, идеально светлый пастельный фон, который не зависит от темы и системных теней.

### 6. Виджет на RemoteViews (отказ от Glance)
- **Проблема:** Изначально виджет делали на Jetpack Glance. Это оказалось катастрофой. `ActionCallback` (кнопки) не обновлял UI, `LocalSize` (адаптивность) работал криво, а состояние виджета кэшировалось системой намертво.
- **Решение:** Полностью выпилили Glance. Переписали виджет на классический `AppWidgetProvider` + `RemoteViews`.
- Верстка виджета теперь в XML (`widget_layout.xml`, `widget_item.xml`).
- Кнопки «Выполнено» и «Пропущено» работают через `PendingIntent` и `onReceive`.
- Обновление виджета вызывается принудительно через `ReminderWidgetProvider.updateAll()` при изменении данных или в `MainActivity.onResume()`.

### 7. Навигация и запуск из виджета
- **Проблема:** При клике по виджету приложение открывалось на том экране, где было свернуто.
- **Решение:** В манифесте `MainActivity` имеет `launchMode="singleTask"`. При клике по виджету вызывается `onNewIntent`, который жестко сбрасывает навигацию на главный экран (`navController.navigate("list") { popUpTo(0) { inclusive = true } }`).

---

## Структура проекта

```text
FlexReminder/
├── .github/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/io/github/egorche/flexreminder/
│       │   ├── MainActivity.kt
│       │   ├── alarm/
│       │   ├── data/
│       │   ├── ui/
│       │   └── widget/
│       │       └── ReminderWidgetProvider.kt
│       └── res/
│           ├── drawable/ (в т.ч. widget_bg_*.xml)
│           ├── layout/   (в т.ч. widget_*.xml)
│           ├── values/
│           └── xml/      (reminder_widget_info.xml)
├── docs/
├── APP_DESCRIPTION.md
├── TEST_PLAN.md
├── TEST_CASES.md
├── CHECKLIST_IMPLEMENTED.md
├── CHECKLIST_FUTURE.md
├── TRACEABILITY_MATRIX.md
├── REGRESSION_TEMPLATE.md
├── BACKLOG.md
├── PROJECT_HISTORY.md
└── CHANGELOG.md
```

---

## Модель данных (БД version = 9)

### Reminder
- `id`, `title`, `notes`, `mode`, `startDate`, `endDate`
- `daysOn`, `daysOff`, `customDates`
- `hour`, `minute`, `enabled`, `silent`, `colorIndex`, `soundUri`, `archivedAt`

### Iteration
- `id`, `reminderId`, `dateMillis`, `status`, `statusSource`
- `statusChangedAt`, `firedAt`, `snoozeCount`, `lastSnoozeAt`, `snoozeUntil`, `snoozeHistory`

### Миграции
- 1→2: `intervalDays` → `daysOn`+`daysOff`
- 2→3: `mode` и `customDates`
- 3→4: таблица `iterations`
- 4→5: `snoozeCount`, `lastSnoozeAt`
- 5→6: `colorIndex`
- 6→7: `soundUri`
- 7→8: `snoozeUntil`
- 8→9: `archivedAt`, `snoozeHistory`

---

## Инфраструктура

### GitHub
- Репозиторий: https://github.com/Egor-Che/FlexReminder
- Ветки: `main` (релизы), `develop` (фичи), `bug-fixes` (багфиксы).

### GitHub Actions
- Триггеры: push в `main`, `develop`, `bug-fixes` + pull_request в `main`.
- Собирает APK и AAB, подписывает ключом из Secrets.

### Тестовые устройства
- POCO X4 GT (HyperOS / Android 14)
- Xiaomi Mi 8 Lite (MIUI 12 / Android 10)
- Эмуляторы API 28, 31, 33, 34

---

## Известные подводные камни

1. **Android Studio настойчиво предлагает обновить AGP** — отказывать всегда.
2. **`android.util.Base64` НЕ работает в build-скриптах** — использовать `java.util.Base64`.
3. **Звук уведомлений на Android 8+ задается только через канал.** Если тестируешь новый звук — удали приложение и установи заново, иначе старый канал с прежним звуком сохранится.
4. **RingtoneManager дублирует звуки.** Дедупликация по имени в `SoundResolver`.
5. **SAF и persistable URI.** Используем `ACTION_OPEN_DOCUMENT` и `takePersistableUriPermission`.
6. **`stringResource` требует `@Composable`.** В не-Composable контексте использовать `context.getString()`.
7. **Тени Card в Compose.** Тени (`elevation`) могут давать грязный оттенок на пастельных фонах. Мы используем `Surface` с `tonalElevation = 0.dp` и `shadowElevation = 0.dp`.
8. **Скругления виджета (RemoteViews).** `setBackgroundColor` перекрывает скругления. Используем `setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_*)` с XML-shape, где уже заданы углы.
9. **`updatePeriodMillis` не точный.** Android в Doze Mode откладывает обновление виджета. Он может обновиться через 20 минут или через час. Это нормально. Для мгновенного обновления мы вызываем `updateAll()` в `onResume` приложения.
10. **Смена темы по расписанию на MIUI/HyperOS.** Система меняет `uiMode` только когда экран выключен (в Doze). Если экран активен, тема не сменится, пока не погаснет.

---

## Что дальше по плану

- ✅ Волна 0 — Базовая функциональность
- ✅ Волна 1.1 — Отметки выполнения
- ✅ Волна 1.2 — Звуки уведомлений
- ✅ Волна 1.3 — Цвета и темы (Палитра, Монохром, Тёмная)
- ✅ Волна 1.4 — Архив и история итераций
- ✅ Волна 1.5 — Ресурсы и локализация
- ✅ Волна 4 — Виджет
- ⏳ Волна 2 — Перенос и шаринг (Следующая)
- ⏳ Волна 3 — Продвинутое планирование
- ⏳ Волна 5 — Публикация

---

## Как продолжить в новом чате

Скопировать в первое сообщение содержимое этого файла (PROJECT_HISTORY.md).
Дальше — просто указать, какую задачу делаем.

Ассистент по этому файлу поймет:
- какая архитектура;
- какие версии;
- что уже работает;
- какие подводные камни;
- что делать дальше.