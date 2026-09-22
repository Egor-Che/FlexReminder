# PROJECT_HISTORY — Гибкие напоминания

Сжатая история проекта: архитектура, ключевые решения, что уже работает.
Нужна для продолжения работы в новом чате без потери контекста.

---

## Что за приложение

Android-приложение для гибких напоминаний. Пользователь может задавать
расписание двумя способами:

1. **Интервалы** — «N дней подряд включено, M дней перерыв». Например,
   2 через 2, 3 через 5, 10 через 20.
2. **Конкретные даты** — множественный выбор в календаре, можно кликать
   отдельные даты и переключать месяцы.

Плюс: период [startDate; endDate], точные будильники, snooze, беззвучный режим.

**Публикация:** планируется бесплатно в RuStore с минимумом разрешений.

---

## Стек и версии

- **Язык:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **БД:** Room
- **AGP:** 8.2.2 (НЕ обновлять! Android Studio настойчиво предлагает 8.13.2 — отказывать)
- **Gradle:** 8.4
- **Kotlin:** 1.9.22
- **Compose Compiler:** 1.5.10
- **compileSdk / targetSdk:** 34
- **minSdk:** 24
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
- В `INTERVAL` используются `daysOn`, `daysOff`, `startDate`, `endDate`.
- В `CUSTOM_DATES` используется `customDates: Set<Long>` (хранится как CSV-строка через TypeConverter).
- Поля не взаимоисключающие: при переключении режима данные не теряются.

### 3. Синхронизация режимов в редакторе
- **Интервалы → Календарь:** при переключении вкладки даты генерируются из текущих интервалов.
- **Календарь → Интервалы:** пробуем распознать простой паттерн (`detectPattern`).
- Ключ `lastGeneratedKey` защищает ручные правки от перезаписи.

### 4. Два канала уведомлений (Android 8+)
- `reminders_loud` — IMPORTANCE_HIGH.
- `reminders_silent` — IMPORTANCE_LOW.
- `Reminder.silent` определяет канал.

### 5. Логика свитчей
- Свитч «Включено» — главный.
- Свитч «Звук» заблокирован, пока событие выключено.
- Включён = звук есть, выключен = без звука.
- Обратной зависимости нет.

### 6. Расписание и уведомления
- `endDate` учитывается и в `nextTriggerTime`, и в `isActiveOn`.
---

## Структура проекта (все файлы)

```
FlexReminder/
├── .github/workflows/build.yml
├── .gitignore
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── BACKLOG.md
├── PROJECT_HISTORY.md
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/flexreminder/
        │   ├── MainActivity.kt
        │   ├── alarm/
        │   │   ├── AlarmScheduler.kt
        │   │   ├── BootReceiver.kt
        │   │   ├── DateUtils.kt
        │   │   ├── Notifications.kt
        │   │   ├── ReminderLogic.kt
        │   │   ├── ReminderReceiver.kt
        │   │   └── SnoozeReceiver.kt
        │   ├── data/
        │   │   ├── AppDatabase.kt
        │   │   ├── Reminder.kt
        │   │   └── ReminderDao.kt
        │   └── ui/
        │       ├── Dialogs.kt
        │       ├── EditReminderScreen.kt
        │       ├── ReminderCalendar.kt
        │       ├── ReminderListScreen.kt
        │       ├── ReminderViewModel.kt
        │       └── ViewReminderScreen.kt
        └── res/
            ├── drawable/ic_launcher.xml
            └── values/
                ├── strings.xml
                └── themes.xml
```
---

## Модель данных (текущая, БД version = 3)

```kotlin
enum class ScheduleMode { INTERVAL, CUSTOM_DATES }

@Entity(tableName = "reminders")
data class Reminder(
    id: Long,
    title: String,
    notes: String,
    mode: ScheduleMode,
    startDate: Long,
    endDate: Long?,
    daysOn: Int,
    daysOff: Int,
    customDates: Set<Long>,
    hour: Int,
    minute: Int,
    enabled: Boolean,
    silent: Boolean
)
```

**Миграции:**
- 1→2: `intervalDays` → `daysOn` + `daysOff`
- 2→3: добавлены `mode` (default 'INTERVAL') и `customDates` (default '')

---

## Инфраструктура

### GitHub
- Репозиторий: https://github.com/Egor-Che/FlexReminder (Private)
- Ветка: `main`
- Пользователь: Egor-Che

### GitHub Actions
- Файл: `.github/workflows/build.yml`
- Триггер: push в `main`
- Собирает **release APK**, подписанный постоянным ключом.
- Артефакт: `flexreminder-release`.

### Секреты GitHub
- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS` = `flexreminder`
- `KEY_PASSWORD`

### Локальный keystore
- Файл `release.keystore` в корне проекта (в .gitignore).
- Алиас: `flexreminder`.
- НЕ коммитится.

### Сборка APK
- Локально: Android Studio → Build → Build Bundle(s) / APK(s) → Build APK(s).
- Через GitHub: Actions → latest build → Artifacts → `flexreminder-release`.

### Обновление приложения
- Первое обновление с debug на release — надо удалить старое приложение.
- Дальше обновление поверх без конфликтов.

---

## Известные подводные камни

1. **Android Studio настойчиво предлагает обновить AGP до 8.13.2** — отказывать
   всегда. Если случайно нажали — откатить `build.gradle.kts` через Git.
2. **«Project was built with AGP 8.2.2 but synced with 8.13.2»** — нажать
   «Sync project», не менять версию.
3. **Копирование кода из чата** иногда вставляет невидимые символы. Если что-то
   не компилируется — открыть файл, выделить всё, перезаписать заново.
4. **Путь файла при создании в Android Studio:** если писать `alarm\ReminderLogic.kt`,
   IDE создаст вложенные папки `alarm\alarm\alarm\`. Создавать просто имя файла.
5. **Импорт в build.gradle.kts:** `android.util.Base64` НЕ работает в build-скриптах.
   Использовать `java.util.Base64`.
6. **project.buildDir deprecated** → `project.layout.buildDirectory`.
7. **Темизация UI — только через единую систему.**
   Когда речь идёт о темах, цветах, типографике и других сквозных аспектах UI —
   сначала проектируем всю систему целиком:
   - Кастомная `MaterialTheme` (`ui/theme/Theme.kt` с полной `colorScheme`).
   - Библиотека цветов и хелперов (`ui/theme/AppThemeColors.kt`, `ReminderPalette.kt`).
   - Стили для системных диалогов (`res/values/themes.xml`).
   
   Только потом — код экранов. Никаких точечных переопределений «на лету» через
   `MaterialTheme.colorScheme.primary` — дефолтная фиолетовая Material-схема будет
   пролазить везде, где явно не переопределено. Мы уже наступили на эти грабли:
   лавандовые поля ввода, вкладки, свитчи, диалоги; бирюзовые DatePicker/TimePicker.

8. **Если сомневаешься между «быстро точечно» и «сразу системно» — уточняй у
   пользователя.** Оба варианта допустимы, но выбор должен быть осознанным.

---

## Как продолжить в новом чате

Скопировать в первое сообщение содержимое этого файла (`PROJECT_HISTORY.md`)
и файла `BACKLOG.md`. Дальше — просто указать, какую задачу делаем.

---

## Что делать после каждого пуша

1. Коммит и push.
2. Дождаться зелёной галочки в GitHub Actions.
3. Скачать APK из Artifacts.
4. Установить, протестировать.
5. Если что-то не так — скинуть в чат лог/скриншот.

---

## Ритм работы

- Меняем один-два файла за итерацию.
- Каждый файл даётся целиком.
- После правок: локальная сборка → тест → commit/push → свежий APK.
- Раз в 1–2 недели: обновление BACKLOG.md (что готово) и при необходимости
  PROJECT_HISTORY.md.
  ---

## Принятые решения по фичам (из обсуждения 19.09.2026)

### Кастомный звук
- Реализуем через SAF (`ACTION_OPEN_DOCUMENT`, тип `audio/*`) — без разрешений на файлы.
- Хранится в `Reminder.soundUri: String?`.
- **НЕ переносится** между устройствами и **НЕ шарится** через мессенджеры.
- При импорте — сбрасывается на системный по умолчанию.

### Шаринг напоминаний
- Формат файла: **`.flexreminder`** (не `.json`!), MIME: `application/x-flexreminder`.
- В манифесте — intent-filter на это расширение и MIME.
- Цель: чтобы Android предлагал ТОЛЬКО наше приложение, а не десятки других.
- При получении файла — предпросмотр + кнопка «Сохранить».

### Цветовая палитра
- 16 пастельных оттенков.
- Каждый RGB-канал в диапазоне **[64; 191]** (отступ ≥25% от краёв).
- Исключаем: чёрный, белый, серый, коричневый.
- Светлые тона — для карточки списка, насыщенные — для свитчей, акцентов, календаря.