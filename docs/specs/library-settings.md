# Экраны «Моё» и «Настройки»

## «Моё» — `ui/library/LibraryScreen.kt`, сигнатура `@Composable fun LibraryScreen()`
- Два ряда (`LazyColumn` с `LazyRow` внутри, как на главной):
  1. «Избранное» — `KinoApp.library.favorites`.
  2. «Продолжить просмотр» — `KinoApp.library.history`.
- Пустой ряд — текст-подсказка серым: «Пока пусто. Добавляй тайтлы кнопкой «★ В избранное»» /
  «Здесь появится то, что ты начнёшь смотреть».
- Нажатие на постер → `openDetails(card)`.
- Если оба списка пусты, по центру крупно: «Здесь будут твои избранные и недавно просмотренные».
- Фокус при открытии на первом постере первого непустого ряда.

## «Настройки» — `ui/settings/SettingsScreen.kt`, сигнатура `@Composable fun SettingsScreen()`
Вертикальный список крупных пунктов (`androidx.tv.material3.ListItem`, если есть в tv-material 1.0.0,
иначе `Surface(onClick)` на всю ширину с фокусом), шириной ~600dp, слева:
1. **«Адрес сайта»**, справа текущий `KinoApp.settings.siteUrl`. Нажатие раскрывает под пунктом поле
   ввода (BasicTextField в Surface) с кнопками «Сохранить» и «По умолчанию».
   - «Сохранить» → `KinoApp.settings.siteUrl = текст` и `KinoApp.repository.clearMemoryCache()`;
   - «По умолчанию» → `KinoApp.settings.resetSiteUrl()` и то же.
2. **«Проверить обновления»** → `LocalAppActions.current.checkUpdates()`.
3. **«Очистить кэш»** → `KinoApp.repository.clearMemoryCache()` и удаление содержимого
   `context.cacheDir/images` и `context.cacheDir/http` (в фоне, `Dispatchers.IO`), затем текст
   «Кэш очищен» под пунктом на 2 секунды.
4. **«Очистить историю просмотров»** → по очереди `removeFromHistory` для всех из `history`.
5. Внизу серым: «KinoTV {versionName}» (из `context.packageManager.getPackageInfo(...)`).
- Фокус при открытии на первом пункте.

Файлы этих экранов — только в своих пакетах `ui/library/` и `ui/settings/`.
