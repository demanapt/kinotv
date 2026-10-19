package com.personal.kinotv.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text


/** Заглушка. Реализация — по docs/specs/settings.md */
@Composable
fun SettingsScreen() {
    Text("Настройки", modifier = Modifier.fillMaxSize().padding(48.dp))
}
