# DESIGN 1.5 — Ресурсы и локализация

Дизайн-документ подволны 1.5: вынос всех пользовательских текстов
в `res/values/strings.xml`, подготовка базы для локализации.

**Статус:** утверждён
**Дата:** 25.09.2026
**Авторы:** Egor-Che (продуктовые решения), ассистент (технические решения)

---

## Цель

Вынести **все** пользовательские тексты приложения из кода в `strings.xml`.
Это даст:

1. Возможность менять формулировки без пересборки кода.
2. Единое место для проверки тона и стиля.
3. Задел под локализацию (в будущем — мгновенная смена языка в приложении).
4. Возможность заменять эмодзи на кастомный набор.

---

## Что выносим

### Все категории текстов

- Заголовки экранов и топбаров.
- Подписи кнопок и иконок (content description).
- Тексты диалогов.
- Пустые состояния («Нет напоминаний…»).
- Подписи полей (label, placeholder).
- Тексты уведомлений.
- Тексты настроек.
- Составные строки через шаблоны (`%1$s`, `%1$d`).
- Эмодзи (отдельные ключи).
- Единицы измерения (сокращения).
- Форматы дат и времени (паттерны `SimpleDateFormat`).

### Что НЕ выносим

- Названия классов, методов, переменных в коде.
- Комментарии в коде.
- Форматы, которые задаются системой (например, `Locale.getDefault()`).
- Названия дней недели и месяцев (берутся из локали).
- Пользовательские данные (названия напоминаний, заметки).

---

## Соглашения по ключам

### Префиксы

| Префикс | Назначение | Пример |
|---|---|---|
| `screen_<name>_<element>` | Строки конкретного экрана | `screen_list_title` |
| `common_<element>` | Общие элементы | `common_cancel`, `common_save` |
| `action_<verb>` | Действия (кнопки, иконки) | `action_edit`, `action_delete` |
| `dialog_<name>_<element>` | Строки диалогов | `dialog_delete_title` |
| `pattern_<name>` | Составные строки с параметрами | `pattern_next_trigger` |
| `status_<name>` | Статусы | `status_completed` |
| `unit_<name>` | Единицы измерения | `unit_minutes_short` |
| `emoji_<name>` | Эмодзи | `emoji_completed` |
| `notification_<name>` | Тексты уведомлений | `notification_snooze_short` |
| `format_<name>` | Паттерны форматирования дат | `format_date_short` |

### Названия экранов

- `list` — главный экран со списком напоминаний.
- `archive` — экран архива.
- `history` — экран истории напоминания.
- `edit` — редактор напоминания.
- `view` — просмотр напоминания.
- `mark` — экран отметок итераций.
- `settings` — настройки приложения.
- `sound_picker` — диалог выбора звука.
- `color_picker` — компонент палитры.
- `calendar` — компонент календаря.

### Формат плейсхолдеров

- `%1$s` — строка.
- `%1$d` — целое число.
- Несколько параметров: `%1$s %2$s`, `%1$d из %2$d`.

**Примеры:**
```xml
<string name="pattern_selected_count">Выбрано: %1$d шт.</string>
<string name="pattern_next_trigger">Следующее: %1$s</string>
<string name="pattern_snooze_label">Перенести %1$s</string>
```

---

## Эмодзи — отдельные ключи

Эмодзи хранятся отдельно от текста. Это позволяет:
- Заменить весь набор эмодзи в одном месте.
- Убрать эмодзи (поставить пустую строку) без правки текстов.

**Список эмодзи:**
```xml
<string name="emoji_completed">✅</string>
<string name="emoji_skipped_user">⏭️</string>
<string name="emoji_skipped_system">❌</string>
<string name="emoji_sound_on">🔔</string>
<string name="emoji_sound_off">🔇</string>
```

**Шаблон сборки:**
```xml
<string name="pattern_with_emoji">%1$s %2$s</string>
```

---

## Единицы измерения (сокращения)

Без `plurals` — используем сокращения:
```xml
<string name="unit_days_short">дн.</string>
<string name="unit_minutes_short">мин</string>
<string name="unit_hours_short">ч</string>
<string name="unit_items_short">шт.</string>
```

**Причина:** русские склонения («1 день / 2 дня / 5 дней») требуют `plurals`, что усложняет вёрстку. Сокращения работают везде одинаково.

---

## Форматы дат и времени

Выносим паттерны `SimpleDateFormat` в ресурсы:
```xml
<string name="format_date_short">dd.MM.yyyy</string>
<string name="format_date_time">dd.MM.yyyy HH:mm</string>
<string name="format_time">HH:mm</string>
<string name="format_date_with_weekday">dd.MM.yyyy, EEEE</string>
<string name="format_date_short_no_year">dd.MM</string>
<string name="format_date_with_short_weekday">dd.MM, EEE</string>
```

**Использование в коде:**
```kotlin
val df = remember { SimpleDateFormat(
    stringResource(R.string.format_date_short),
    Locale.getDefault()
) }
```

При локализации паттерны можно менять под каждую локаль.

---

## Локализация — только задел, не реализация

**Сейчас:** только русский (`values/strings.xml`).

**В будущем (после публикации):**
- Добавление других локалей — через `values-en/`, `values-de/` и т.д.
- Выбор языка в настройках — через `AppCompatDelegate.setApplicationLocales()`.
- Пользовательские данные (названия напоминаний) не переводятся.
- Технически: `AppCompatDelegate` поддерживает API 21+, у нас `minSdk = 28`.

**Фиксация идеи в BACKLOG.md и PROJECT_HISTORY.md.**
---

## Порядок работы

Идём **по экрану целиком**. Для каждого файла:
1. Выносим строки этого файла в `strings.xml` (одним блоком с комментарием).
2. Заменяем в коде `Text("...")` на `Text(stringResource(R.string.xxx))`.
3. Собираем, проверяем визуально, коммитим.

Это позволяет ловить ошибки сразу и не накапливать их на весь проект.

---

## Список файлов для обработки

### Компоненты (сначала — маленькие)

1. `ui/IterationStatusLabel.kt` — статусы итераций.
2. `ui/ReminderCalendar.kt` — дни недели, единицы измерения.
3. `ui/IterationStatusLabel.kt` — вспомогательные функции.
4. `ui/ColorPalettePicker.kt` — content description.
5. `ui/Dialogs.kt` — обёртки над DatePicker/TimePicker.

### Экраны

6. `ui/ReminderListScreen.kt` — главный экран.
7. `ui/ArchiveScreen.kt` — архив.
8. `ui/ReminderHistoryScreen.kt` — история напоминания.
9. `ui/IterationDetailsDialog.kt` — модалка итерации.
10. `ui/ViewReminderScreen.kt` — просмотр напоминания.
11. `ui/EditReminderScreen.kt` — редактор (большой).
12. `ui/SettingsScreen.kt` — настройки.
13. `ui/SoundPickerDialog.kt` — диалог выбора звука.
14. `ui/MarkIterationsScreen.kt` — экран отметок.
15. `ui/MarkIterationsComponents.kt` — компоненты экрана отметок.

### Уведомления и сервисы

16. `alarm/Notifications.kt` — тексты уведомлений, кнопки.
17. `alarm/Notifications.kt` — формат snooze (перенос).
18. `alarm/SnoozeReceiver.kt` — тексты (если есть).
19. `alarm/MarkCompletedReceiver.kt` — тексты (если есть).
20. `alarm/MarkSkippedReceiver.kt` — тексты (если есть).

### Основное

21. `MainActivity.kt` — заголовки роутов (если есть).
22. `app/src/main/res/values/strings.xml` — итоговая сводка.

---

## Что проверяем после каждого файла

1. **Сборка проходит.** Никаких `Unresolved reference: R.string.xxx`.
2. **Визуально** — тексты отображаются корректно.
3. **Плейсхолдеры** — не съехали при подстановке.
4. **Эмодзи** — если были, отображаются.
5. **Длина строк** — не ломает вёрстку.

---

## Инструменты и методы

### Создание `strings.xml`

Все строки в одном файле. Группировка по экранам через комментарии:

```xml
<resources>
    <!-- Общие -->
    <string name="app_name">Гибкие напоминания</string>
    <string name="common_cancel">Отмена</string>
    <string name="common_save">Сохранить</string>
    <string name="common_delete">Удалить</string>
    <string name="common_close">Закрыть</string>
    <string name="common_back">Назад</string>
    <string name="common_ok">Понятно</string>
    
    <!-- Главный экран -->
    <string name="screen_list_title">Мои напоминания</string>
    <string name="screen_list_empty">Нет напоминаний.\nНажмите + чтобы создать.</string>
    <string name="screen_list_settings">Настройки</string>
    <string name="screen_list_archive">Архив</string>
    
    <!-- ... и так далее по экранам ... -->
</resources>
```

### Замена в коде

Было:
```kotlin
Text("Мои напоминания")
```

Стало:
```kotlin
Text(stringResource(R.string.screen_list_title))
```

Для составных строк:
```kotlin
// Было:
Text("Выбрано: ${selectedDates.size} шт.")

// Стало:
Text(stringResource(R.string.pattern_selected_count, selectedDates.size))
```

Для contentDescription:
```kotlin
// Было:
Icon(Icons.Default.Settings, contentDescription = "Настройки")

// Стало:
Icon(
    Icons.Default.Settings,
    contentDescription = stringResource(R.string.screen_list_settings)
)
```

### Импорты

В каждом файле, где используем `stringResource`, добавляем:
```kotlin
import androidx.compose.ui.res.stringResource
import io.github.egorche.flexreminder.R
```

Для `R.string.*` — импорт R-класса.

---

## Обработка особых случаев

### 1. Toast / Snackbar

В местах, где раньше был захардкоженный текст, теперь берём из ресурсов:
```kotlin
Toast.makeText(
    context,
    context.getString(R.string.some_message),
    Toast.LENGTH_SHORT
).show()
```

### 2. Уведомления

`Notifications.kt` не имеет доступа к Compose-контексту, но имеет `Context`. Используем `context.getString(R.string.xxx)`.

### 3. Диалоги AlertDialog

Заголовки и тексты — из ресурсов.

### 4. Функции, возвращающие строку

Например, `formatPattern(daysOn, daysOff): String` возвращает строку. Теперь эта функция должна принимать `Context` или быть `@Composable`:
```kotlin
@Composable
fun formatPattern(daysOn: Int, daysOff: Int): String = when {
    daysOff <= 0 -> stringResource(R.string.pattern_every_day)
    daysOn == 1 && daysOff == 1 -> stringResource(R.string.pattern_every_other_day)
    // ...
    else -> stringResource(R.string.pattern_days_on_off, daysOn, daysOff)
}
```

**Важно:** все такие функции придётся превратить в `@Composable`. Это нормально.

---

## Проверка на локализацию (тестовая)

После выноса строк делаем **проверку симуляцией другого языка**:

1. Создаём временный файл `values-en/strings.xml` с произвольными английскими текстами.
2. Запускаем приложение → проверяем, что все тексты поменялись.
3. Удаляем временный файл (или оставляем для будущей локализации).

Это докажет, что вынос сделан корректно и локализация действительно возможна.

**Не обязательно делать прямо сейчас.** Достаточно, что структура позволяет.

---

## Что НЕ делаем в 1.5

- **Реализацию смены языка в настройках.** Только задел. Сама смена языка — отдельная задача после публикации.
- **Перевод на английский.** Пока только русский.
- **Форматирование чисел и дат под локаль.** Используем `Locale.getDefault()` как сейчас. Локаль приложения — отдельная задача.
- **Plurals для склонений.** Используем сокращения.

---

## Финальная проверка 1.5

После обработки всех файлов:

1. **Поиск по проекту** захардкоженных русских строк:
   - Visual Studio: Ctrl+Shift+F → поиск по `Text("` в `.kt` файлах.
   - Или grep: `grep -r "Text(\"" app/src/main/java/`.
   - Должно остаться 0 совпадений (кроме случаев, где строка точно не пользовательская).

2. **Сборка** — release APK проходит без ошибок.

3. **Проверка на POCO** — пройтись по всем экранам, убедиться, что тексты на месте.

4. **Проверка смены темы** — тексты отображаются корректно в Монохроме и Палитре.

5. **Проверка уведомлений** — тексты кнопок и уведомлений корректны.

6. **Проверка многострочных текстов** — пустые состояния, диалоги, длинные описания.

---

## Оценка

- **~2 сессии** работы.
- **~200–300 строк** для выноса.
- **~15 файлов** для правки.

---

## Связанные документы

- `BACKLOG.md` → Волна 1.5
- `PROJECT_HISTORY.md` → подводные камни и архитектурные решения
- `TEST_CASES.md` → возможно, небольшой модуль для проверки текстов

---

## История изменений

| Дата | Что |
|---|---|
| 25.09.2026 | Утверждение дизайна. По экрану целиком, только русский, эмодзи отдельно. |