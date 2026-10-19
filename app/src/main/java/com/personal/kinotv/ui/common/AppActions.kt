package com.personal.kinotv.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import com.personal.kinotv.data.Card
import com.personal.kinotv.data.PlayerSource

/** Действия, доступные любому экрану через LocalAppActions.current. */
interface AppActions {
    /** Открыть экран тайтла. */
    fun openDetails(card: Card)

    /** Запустить просмотр. Без source — берётся первый плеер со страницы тайтла. */
    fun play(card: Card, source: PlayerSource? = null)

    /** Проверить обновления приложения вручную (показывает результат сам). */
    fun checkUpdates()
}

val LocalAppActions = staticCompositionLocalOf<AppActions> { error("AppActions не задан") }
