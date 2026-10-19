# KinoTV — правила для агентов

Личное приложение для Android TV (Xiaomi Mi Box 4: Android 9, 2 ГБ памяти, слабая графика).
Интерфейс в стиле Netflix, данные берутся с сайта uakinogo.io (разбор HTML уже написан).
Управление — только пультом: стрелки, OK, «Назад». Сенсорного экрана и мыши нет.

## Стек (версии зафиксированы, новые зависимости не добавлять)
- Kotlin 2.0.21, AGP 8.7.3, minSdk 21, compileSdk 35
- Compose BOM 2024.12.01 (`androidx.compose.ui`, `androidx.compose.foundation`)
- `androidx.tv:tv-material:1.0.0` — **все** интерактивные элементы берутся отсюда:
  `androidx.tv.material3.Button`, `Card`, `Surface(onClick=…)`, `Tab`, `Text`, `MaterialTheme`.
  Не используй `androidx.compose.material3` — её нет в проекте.
- Списки: `LazyRow`, `LazyColumn`, `LazyVerticalGrid` из `androidx.compose.foundation.lazy`
  (не `androidx.tv.foundation` — его нет в проекте).
- Картинки: `coil3.compose.AsyncImage` (Coil 3.0.4)
- Навигация уже настроена (`ui/AppShell.kt`), экраны её напрямую не трогают.
- Флаг `-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api` включён в сборке.

## Что можно и нельзя менять
- Меняй **только** файлы пакета экрана из задачи (`ui/<экран>/`), там можно создавать новые файлы.
- **Не трогай:** `data/`, `player/`, `ui/common/`, `ui/theme/`, `ui/AppShell.kt`, `MainActivity.kt`,
  `KinoApp.kt`, `Updater.kt`, файлы Gradle, манифест.
  Если для экрана нужно что-то в этих местах, не меняй сам, а опиши это в итоговом отчёте.
- Публичная сигнатура экрана задана в задаче. Не меняй её.

## Готовые API (используй их, не пиши свои аналоги)
- `KinoApp.repository` (`data/Repository.kt`), все функции `suspend`, сами уходят в фон:
  `category(slug, page)`, `search(query, page)` → `Page(items, hasNext)`;
  `newEpisodes()` → `List<EpisodeUpdate>`; `details(url)` → `Details`.
- `KinoApp.library` (`data/Library.kt`): `favorites`/`history` (`StateFlow<List<Card>>`, собирай через
  `collectAsState()`), `isFavorite(url)`, `toggleFavorite(card)`, `removeFromHistory(url)`.
- `KinoApp.settings` (`data/AppSettings.kt`): `siteUrl`, `resetSiteUrl()`.
- `LocalAppActions.current` (`ui/common/AppActions.kt`): `openDetails(card)`, `play(card, source?)`,
  `checkUpdates()`.
- `rememberPagedList(key) { page -> repository.… }` (`ui/common/PagedList.kt`): `items`, `isLoading`,
  `error`, `endReached`, `loadMore()`, `retry()`.
- Компоненты (`ui/common/Components.kt`): `PosterCard(card, onClick, onFocused=…, badge=…)`,
  `LoadingBox()`, `ErrorBox(message, onRetry)`, `RatingText(rating)`, `Dimens`.
- Цвета: `KinoColors` (`ui/theme/Theme.kt`). Модификатор `Modifier.onFocusGained {}` (`ui/common/Modifiers.kt`).
- Модели: `data/Models.kt` (`Card`, `Details`, `PlayerSource`, `EpisodeUpdate`, `Section`, `Sections`).

## Правила для пульта (обязательно)
1. Всё, что можно нажать, фокусируется и **видно в фокусе** (увеличение и/или рамка). Компоненты tv-material
   делают это сами, не отключай.
2. При входе на экран фокус ставится на главный элемент (`FocusRequester` + `LaunchedEffect`).
   Запрашивай фокус только после появления элемента на экране (например, когда данные загрузились).
   **Экраны под вкладками** ставят начальный фокус, только если `!LocalTabsFocused.current.value`
   (`ui/common/TabsFocus.kt`): когда пользователь идёт стрелкой по вкладкам, экран не должен забирать фокус.
3. Фокус не должен «теряться» и не должен застревать: со стрелками можно дойти до любого элемента,
   а вверх с верхнего элемента экрана фокус уходит во вкладки приложения.
4. В `LazyRow`/`LazyColumn`/`LazyVerticalGrid` у элементов всегда `key = card.url` (или другой стабильный ключ).
5. Подгрузка: когда фокус на одном из 5 последних элементов, вызывай `loadMore()`.
6. «Назад» уже обрабатывает приложение. Свой `BackHandler` ставь, только если задача этого требует.

## Производительность (слабая приставка)
- Никаких `Modifier.blur`, тяжёлых теней и своих анимаций (кроме встроенного увеличения при фокусе).
- Большая картинка на экране одна (фон или баннер). В рядах и сетках только `PosterCard`.
- Не держи в памяти больше, чем видно, и не запускай загрузки в цикле при рекомпозиции.

## Стиль
- Все тексты интерфейса на русском.
- Kotlin official style, короткие комментарии на русском только там, где неочевидно.
- Ты не можешь запустить Gradle: в песочнице нет сети. Пиши аккуратно под версии библиотек выше,
  проверяй импорты и сигнатуры. Сборку запускает Claude.
- В конце работы перечисли изменённые файлы и всё, в чём не уверен.
