package com.personal.kinotv.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.personal.kinotv.KinoApp
import com.personal.kinotv.ui.common.Dimens
import com.personal.kinotv.ui.common.LocalAppActions
import com.personal.kinotv.ui.theme.KinoColors

@Composable
fun LibraryScreen() {
    val favorites by KinoApp.library.favorites.collectAsState()
    val history by KinoApp.library.history.collectAsState()
    val actions = LocalAppActions.current
    var initialFocusHandled by remember { mutableStateOf(false) }

    if (favorites.isEmpty() && history.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(Dimens.ScreenPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Здесь будут твои избранные и недавно просмотренные",
                style = MaterialTheme.typography.headlineMedium,
                color = KinoColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "favorites") {
            LibraryRow(
                title = "Избранное",
                cards = favorites,
                emptyText = "Пока пусто. Добавляй тайтлы кнопкой «★ В избранное»",
                requestInitialFocus = !initialFocusHandled && favorites.isNotEmpty(),
                onInitialFocusHandled = { initialFocusHandled = true },
                onClick = { actions.openDetails(it) },
            )
        }
        item(key = "history") {
            LibraryRow(
                title = "Продолжить просмотр",
                cards = history,
                emptyText = "Здесь появится то, что ты начнёшь смотреть",
                requestInitialFocus = !initialFocusHandled && favorites.isEmpty(),
                onInitialFocusHandled = { initialFocusHandled = true },
                onClick = { actions.openDetails(it) },
            )
        }
    }
}
