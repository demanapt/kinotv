package com.personal.kinotv.ui.search

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.personal.kinotv.ui.theme.KinoColors

@Composable
internal fun SearchInput(
    text: String,
    onTextChange: (String) -> Unit,
    inputFocus: FocusRequester,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val editorFocus = remember { FocusRequester() }
    var editing by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    LaunchedEffect(editing) {
        if (editing) {
            withFrameNanos { }
            editorFocus.requestFocus()
            withFrameNanos { }
            keyboard?.show()
        }
    }

    Surface(
        onClick = {
            if (editing) {
                editorFocus.requestFocus()
                keyboard?.show()
            } else {
                editing = true
            }
        },
        modifier = modifier
            .focusRequester(inputFocus)
            .onFocusChanged {
                hasFocus = it.hasFocus
                if (!it.hasFocus && editing) {
                    editing = false
                    keyboard?.hide()
                }
            }
            .border(
                width = 2.dp,
                color = if (hasFocus) KinoColors.FocusBorder else KinoColors.SurfaceVariant,
                shape = shape,
            ),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = KinoColors.Surface,
            contentColor = KinoColors.TextPrimary,
            focusedContainerColor = KinoColors.SurfaceVariant,
            focusedContentColor = KinoColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .focusRequester(editorFocus)
                .focusProperties { canFocus = editing }
                .onPreviewKeyEvent {
                    // В редакторе ↑/↓ перемещают фокус, а не текстовый курсор.
                    if (it.type == KeyEventType.KeyDown) {
                        when (it.key) {
                            Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                            Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
                            else -> false
                        }
                    } else {
                        false
                    }
                },
            textStyle = MaterialTheme.typography.titleMedium.copy(color = KinoColors.TextPrimary),
            cursorBrush = SolidColor(KinoColors.TextPrimary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                showKeyboardOnFocus = false,
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboard?.hide()
                    onSearch()
                    editing = false
                },
                onDone = {
                    keyboard?.hide()
                    onSearch()
                    editing = false
                },
            ),
            decorationBox = { field ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = "Название фильма, сериала, аниме…",
                            color = KinoColors.TextSecondary,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                    }
                    field()
                }
            },
        )
    }
}
