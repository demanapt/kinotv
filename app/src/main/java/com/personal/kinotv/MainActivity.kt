package com.personal.kinotv

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.personal.kinotv.data.Card
import com.personal.kinotv.data.PlayerSource
import com.personal.kinotv.player.PlayerActivity
import com.personal.kinotv.ui.AppNavigation
import com.personal.kinotv.ui.theme.KinoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var updater: Updater
    private var lastBackPress = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updater = Updater(this)

        setContent {
            KinoTheme {
                AppNavigation(
                    play = ::play,
                    checkUpdates = { updater.check(silent = false) },
                    onExitRequest = ::onExitRequest,
                )
            }
        }

        if (intent?.action == Updater.ACTION_INSTALL_STATUS) updater.handleInstallStatus(intent)
        if (savedInstanceState == null) updater.check(silent = true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Updater.ACTION_INSTALL_STATUS) updater.handleInstallStatus(intent)
    }

    /** Запуск просмотра: запоминаем в «Продолжить просмотр» и открываем плеер сайта. */
    private fun play(card: Card, source: PlayerSource?) {
        lifecycleScope.launch {
            val player = source ?: runCatching { KinoApp.repository.details(card.url).players.firstOrNull() }.getOrNull()
            if (player == null) {
                toast("Не нашёл плеер для «${card.title}»")
                return@launch
            }
            KinoApp.library.addToHistory(card)
            PlayerActivity.start(this@MainActivity, player.url, card.title)
        }
    }

    private fun onExitRequest() {
        val now = SystemClock.uptimeMillis()
        if (now - lastBackPress < 2000) finish()
        else {
            lastBackPress = now
            toast("Нажми «Назад» ещё раз, чтобы выйти")
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
