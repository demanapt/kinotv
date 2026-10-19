package com.personal.kinotv.ui.common

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * true, пока фокус на вкладках сверху. Экраны под вкладками не должны забирать фокус себе
 * при открытии, иначе нельзя пройти стрелкой по вкладкам: внутрь экрана заходят кнопкой ↓.
 */
val LocalTabsFocused = staticCompositionLocalOf<State<Boolean>> { mutableStateOf(false) }
