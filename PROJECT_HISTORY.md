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
беззвучный режим, две темы (Палитра и Монохром), звуки уведомлений,
архив с историей итераций.

**Публикация:** планируется бесплатно в RuStore с минимумом разрешений.

---

## Стек и версии

- **Язык:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **БД:** Room (SQLite)
- **Настройки:** DataStore Preferences
- **Планировщик:** AlarmManager
- **Фоновая работа:** WorkManager
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

### 4. Два базовых канала уведомлений

- `reminders_loud` — со звуком (встроенная мелодия приложения).
- `reminders_silent` — беззвучный.
- `Reminder.silent` определяет канал.

### 5. Логика свитчей

- Свитч «Включено» — главный.
- Свитч «Звук» заблокирован, пока событие выключено.
- Включён = звук есть, выключен = без звука.
- Обратной зависимости нет.

### 6. Расписание и уведомления

- `endDate` учитывается и в `nextTriggerTime`, и в `isActiveOn`.

### 7. Схема каналов уведомлений (свой звук)

Один уникальный звук = один канал. ID канала формируется как
`reminders_custom_<sha1(uri).take(8)>` или `reminders_default_<sha1(uri).take(8)>`.
Это позволяет:
- иметь максимум 2–3 канала у большинства пользователей,
- переиспользовать один канал для нескольких напоминаний с одним звуком,
- удалять неиспользуемые каналы автоматически.

Базовые каналы: `reminders_loud` (встроенная мелодия приложения),
`reminders_silent` (беззвучный).

### 8. Приоритет звука при показе уведомления

Порядок приоритетов:

1. `Reminder.silent = true` → канал `reminders_silent`.
2. `Reminder.soundUri` валиден → канал `custom_<hash>`.
3. Глобальный звук валиден → канал `default_<hash>`.
4. Иначе → `reminders_loud` (встроенная мелодия).

Резолвер: `SoundResolver.resolveSoundUri` + `NotificationChannels.findChannelIdForUri`.

### 9. Архив и история

- Поле `Reminder.archivedAt: Long?` — момент архивации.
- Поле `Iteration.snoozeHistory: String` — история переносов.
- `ArchiveManager.archiveExpired` — единая точка входа для автоархивации.
  Вызывается при старте приложения, при перезагрузке и при сохранении.
- Экран `ArchiveScreen` — список компактных карточек в серой зебре.
- Экран `ReminderHistoryScreen` — read-only просмотр с календарём.
- Создание по примеру — переиспользует `EditReminderScreen` с `templateId`.

### 10. Ресурсы и локализация

Все пользовательские тексты вынесены в `res/values/strings.xml`. Это даёт:
- Возможность менять формулировки без правки кода.
- Единое место для проверки тона.
- Задел под локализацию (в будущем — смена языка через `AppCompatDelegate.setApplicationLocales`).

**Соглашения по ключам:**
- `screen_<name>_<element>` — строки экранов.
- `common_<element>` — общие элементы (кнопки, «Назад», «Отмена»).
- `action_<verb>` — действия.
- `pattern_<name>` — параметризованные строки с `%1$s` / `%1$d`.
- `format_<name>` — паттерны `SimpleDateFormat`.
- `unit_<name>` — единицы измерения (сокращения).
- `emoji_<name>` — эмодзи отдельно от текста.

**Важно:** с подволны 1.5 функции, использующие `stringResource(...)`, должны быть `@Composable`. Если функция вызывается из не-Composable контекста, использовать `context.getString(...)`.
---

## Структура проекта

```
FlexReminder/
├── .github/
│   ├── workflows/build.yml
│   └── ISSUE_TEMPLATE/
│       ├── bug_report.md
│       ├── feature_request.md
│       └── config.yml
├── .gitignore
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── README.md
├── APP_DESCRIPTION.md
├── TEST_PLAN.md
├── TEST_CASES.md
├── CHECKLIST_IMPLEMENTED.md
├── CHECKLIST_FUTURE.md
├── TRACEABILITY_MATRIX.md
├── REGRESSION_TEMPLATE.md
├── BACKLOG.md
├── PROJECT_HISTORY.md
├── docs/
│   ├── design/
│   │   ├── DESIGN_1.1.md
│   │   ├── DESIGN_1.2.md
│   │   ├── DESIGN_1.3.md
│   │   └── DESIGN_1.4.md
│   └── regression-reports/
│       ├── _TEMPLATE.md
│       └── 2026-09-22-regression-v1.0.md
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/io/github/egorche/flexreminder/
        │   ├── MainActivity.kt
        │   ├── alarm/
        │   │   ├── AlarmScheduler.kt
        │   │   ├── AutoSkipWorker.kt
        │   │   ├── BootReceiver.kt
        │   │   ├── DateUtils.kt
        │   │   ├── IterationLogic.kt
        │   │   ├── MarkCompletedReceiver.kt
        │   │   ├── MarkSkippedReceiver.kt
        │   │   ├── NotificationChannels.kt
        │   │   ├── Notifications.kt
        │   │   ├── ReminderLogic.kt
        │   │   ├── ReminderReceiver.kt
        │   │   ├── SnoozeReceiver.kt
        │   │   ├── SnoozeRestorer.kt
        │   │   └── SoundResolver.kt
        │   ├── data/
        │   │   ├── AppDatabase.kt
        │   │   ├── ArchiveManager.kt
        │   │   ├── Iteration.kt
        │   │   ├── IterationDao.kt
        │   │   ├── Reminder.kt
        │   │   ├── ReminderDao.kt
        │   │   └── SettingsRepository.kt
        │   └── ui/
        │       ├── ArchiveScreen.kt
        │       ├── ArchiveViewModel.kt
        │       ├── ColorPalettePicker.kt
        │       ├── Dialogs.kt
        │       ├── EditReminderScreen.kt
        │       ├── IterationDetailsDialog.kt
        │       ├── IterationStatusLabel.kt
        │       ├── MarkIterationsComponents.kt
        │       ├── MarkIterationsScreen.kt
        │       ├── ReminderCalendar.kt
        │       ├── ReminderHistoryScreen.kt
        │       ├── ReminderHistoryViewModel.kt
        │       ├── ReminderListScreen.kt
        │       ├── ReminderViewModel.kt
        │       ├── SettingsScreen.kt
        │       ├── SettingsViewModel.kt
        │       ├── SoundPickerDialog.kt
        │       ├── SoundPickerViewModel.kt
        │       ├── ViewReminderScreen.kt
        │       └── theme/
        │           ├── AppThemeColors.kt
        │           ├── ReminderPalette.kt
        │           └── Theme.kt
        └── res/
            ├── drawable/
            │   └── ic_launcher_background.xml
            ├── mipmap-anydpi-v26/
            │   ├── ic_launcher.xml
            │   └── ic_launcher_round.xml
            ├── mipmap-hdpi/
            │   ├── ic_launcher.webp
            │   ├── ic_launcher_foreground.webp
            │   └── ic_launcher_round.webp
            ├── mipmap-mdpi/
            │   ├── ic_launcher.webp
            │   ├── ic_launcher_foreground.webp
            │   └── ic_launcher_round.webp
            ├── mipmap-xhdpi/
            │   ├── ic_launcher.webp
            │   ├── ic_launcher_foreground.webp
            │   └── ic_launcher_round.webp
            ├── mipmap-xxhdpi/
            │   ├── ic_launcher.webp
            │   ├── ic_launcher_foreground.webp
            │   └── ic_launcher_round.webp
            ├── mipmap-xxxhdpi/
            │   ├── ic_launcher.webp
            │   ├── ic_launcher_foreground.webp
            │   └── ic_launcher_round.webp
            ├── raw/
            │   └── default_ringtone.ogg
            └── values/
                ├── colors.xml
                ├── strings.xml
                └── themes.xml
```

---

## Модель данных (текущая, БД version = 9)

### Reminder

Поля:
- `id: Long` — PK.
- `title: String` — название.
- `notes: String` — заметка.
- `mode: ScheduleMode` — INTERVAL / CUSTOM_DATES.
- `startDate: Long` — полночь начала периода.
- `endDate: Long?` — полночь окончания. null = бессрочно.
- `daysOn: Int` — для INTERVAL.
- `daysOff: Int` — для INTERVAL.
- `customDates: Set<Long>` — для CUSTOM_DATES. Хранится в БД как CSV.
- `hour: Int`, `minute: Int` — время срабатывания.
- `enabled: Boolean` — включено/выключено.
- `silent: Boolean` — без звука.
- `colorIndex: Int?` — 0..15 или null (белый).
- `soundUri: String?` — URI звука или null.
- `archivedAt: Long?` — null = активно, иначе в архиве.

### Iteration

Поля:
- `id: Long` — PK.
- `reminderId: Long` — FK.
- `dateMillis: Long` — полночь итерации.
- `status: IterationStatus` — PENDING / COMPLETED / SKIPPED.
- `statusSource: StatusSource?` — USER / SYSTEM / null.
- `statusChangedAt: Long?` — когда поставлен статус.
- `firedAt: Long?` — когда сработало уведомление.
- `snoozeCount: Int` — сколько раз перенесено.
- `lastSnoozeAt: Long?` — время последнего переноса.
- `snoozeUntil: Long?` — когда сработает отложка.
- `snoozeHistory: String` — `"ts1:min1,ts2:min2,..."`.

### Миграции БД

- **1 → 2:** `intervalDays` → `daysOn` + `daysOff`.
- **2 → 3:** добавлены `mode` (default 'INTERVAL') и `customDates` (default '').
- **3 → 4:** создана таблица `iterations`.
- **4 → 5:** в `iterations` добавлены `snoozeCount`, `lastSnoozeAt`.
- **5 → 6:** в `reminders` добавлено `colorIndex`.
- **6 → 7:** в `reminders` добавлено `soundUri`.
- **7 → 8:** в `iterations` добавлено `snoozeUntil`.
- **8 → 9:** в `reminders` добавлено `archivedAt`, в `iterations` — `snoozeHistory`.

---

## Инфраструктура

### GitHub

- Репозиторий: https://github.com/Egor-Che/FlexReminder (Private)
- Ветка: `main`
- Пользователь: Egor-Che

### RuStore

- Контактный email для карточки: `flexreminder@mail.ru`
- Политика конфиденциальности: https://Egor-Che.github.io/FlexReminder/privacy-policy.html
- Название приложения и в магазине, и в лаунчере: **Напоминалка**
- Категория: Продуктивность
- Возрастной рейтинг: 0+

### GitHub Actions

- Файл: `.github/workflows/build.yml`
- Триггер: push в `main`
- Собирает release APK, подписанный постоянным ключом.
- Артефакт: `flexreminder-release`.

### Секреты GitHub

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS` = `flexreminder`
- `KEY_PASSWORD`

### Локальный keystore

- Файл `release.keystore` в корне проекта (в .gitignore).
- Пароль — в `local.properties`.
- Пароль — в менеджере паролей (сохранить отдельно!).

### Сборка APK

- Локально: Android Studio → Build → Build Bundle(s) / APK(s) → Build APK(s).
- Через GitHub: Actions → latest build → Artifacts → `flexreminder-release`.

### Обновление приложения

- Первое обновление debug → release — надо удалить старое приложение.
- Дальше обновление поверх без конфликтов (одинаковая подпись).

### Тестовые устройства

- POCO X4 GT (Android 14) — основной целевой девайс.
- Xiaomi Mi 8 Lite (Android 10, MIUI 12) — сложная оболочка.
- Xiaomi Mi A2 (Android 10, Android One) — чистый Android.
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
   `MaterialTheme.colorScheme.primary` — дефолтная фиолетовая Material-схема
   будет пролазить везде, где явно не переопределено.

8. **Если сомневаешься между «быстро точечно» и «сразу системно» — уточняй у
   пользователя.** Оба варианта допустимы, но выбор должен быть осознанным.

9. **Звук уведомлений на Android 8+ задаётся только через канал.**
   Звук существующего канала изменить нельзя — только удалить канал и
   создать новый. При удалении приложения все каналы сбрасываются.
   Если тестируешь новую логику звука — удали приложение и установи
   заново, иначе старые каналы с прежним звуком сохранятся.

10. **RingtoneManager дублирует звуки.**
    На многих устройствах один и тот же трек возвращается дважды: из
    системной базы и из MediaStore. URI разные, имя одинаковое.
    Дедупликация делается по имени (в `SoundResolver.listSystemSounds`).

11. **SAF и persistable URI.**
    Для выбора пользовательского файла используем `ACTION_OPEN_DOCUMENT`
    (не `ACTION_GET_CONTENT`) и `takePersistableUriPermission`. Без этого
    доступ к файлу теряется после перезапуска приложения.

12. **Громкость уведомлений на Android.**
    Громкость уведомлений — отдельный stream (STREAM_NOTIFICATION).
    На многих оболочках (MIUI, EMUI) ползунок «Уведомления» скрыт, а
    фактическая громкость зависит от громкости рингтона (STREAM_RING).
    Мы остаёмся на стандартном USAGE_NOTIFICATION, не вмешиваемся
    в системный stream.

13. **Архив напоминаний.**
    Архивация срабатывает в трёх точках: при сохранении (если endDate в прошлом),
    при старте приложения, при перезагрузке. Перед архивацией закрываются все
    PENDING-итерации как SKIPPED (SYSTEM), чтобы в архиве не было «висящих».
    Архив всегда отображается в серой зебре — не зависит от активной темы.

14. **История итераций через snoozeHistory.**
    История переносов хранится в строке формата `"ts1:min1,ts2:min2,..."`
    в поле `Iteration.snoozeHistory`. Максимум 5 записей. Парсится через
    `parseSnoozeHistory` (пустой список при невалидном формате).

15. **Создание по примеру — сдвиг дат.**
    При создании из шаблона пересчитываются даты: `startDate = сегодня`,
    `endDate = сегодня + длительность шаблона`. Для режима CUSTOM_DATES
    все даты сдвигаются на ту же разницу. `soundUri` не копируется.

16. **`stringResource` требует `@Composable`.**
    При выносе строк в ресурсы все функции, использующие `stringResource(...)`,
    должны стать `@Composable`. Если функция вызывается из не-Composable контекста
    (ViewModel, `BroadcastReceiver`, утилита) — использовать `context.getString(...)`.
	
17. **Иконка приложения — адаптивная (Android 8+).**
    Сгенерирована через Image Asset Studio в Android Studio.
    Содержит: `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml`
    (адаптивные), `drawable/ic_launcher_background.xml` (фон),
    растр-версии в `mipmap-{mdpi..xxxhdpi}` для API < 26.
    **Не удалять ни один из этих файлов** — иначе поломается отображение
    иконки на части устройств.
---

## Как продолжить в новом чате

Скопировать в первое сообщение содержимое этого файла (PROJECT_HISTORY.md).
Дальше — просто указать, какую задачу делаем.

Ассистент по этому файлу поймёт:
- какая архитектура;
- какие версии;
- что уже работает;
- какие подводные камни;
- что делать дальше.

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

## Что дальше по плану

- ✅ Волна 0 — Базовая функциональность
- ✅ Волна 1.1 — Отметки выполнения
- ✅ Волна 1.2 — Звуки уведомлений
- ✅ Волна 1.3 — Цвета и темы
- ✅ Волна 1.4 — Архив и история итераций
- ✅ Волна 1.5 — Ресурсы и локализация (служебная)
- ⏳ Волна 2 — Перенос и шаринг
- ⏳ Волна 3 — Продвинутое планирование (категории, роли по дням, часовые пояса)
- ⏳ Волна 4 — Виджет
- ⏳ Волна 5 — Публикация
- ⏳ Волна 7 — Планы на будущее (резерв)