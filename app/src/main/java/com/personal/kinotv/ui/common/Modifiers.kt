package com.personal.kinotv.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged

/** Вызывает action, когда элемент получает фокус. */
fun Modifier.onFocusGained(action: () -> Unit): Modifier =
    onFocusChanged { if (it.isFocused) action() }
