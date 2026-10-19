package com.personal.kinotv.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.personal.kinotv.KinoApp
import com.personal.kinotv.ui.common.Dimens
import com.personal.kinotv.ui.common.LocalAppActions
import com.personal.kinotv.ui.theme.KinoColors
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val actions = LocalAppActions.current
    val scope = rememberCoroutineScope()
    val siteFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var siteUrl by remember { mutableStateOf(KinoApp.settings.siteUrl) }
    var siteEditorExpanded by remember { mutableStateOf(false) }
    var clearingCache by remember { mutableStateOf(false) }
    var cacheMessage by remember { mutableStateOf<String?>(null) }
    var clearingHistory by remember { mutableStateOf(false) }
    val versionName = remember(context) {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName.orEmpty()
    }

    fun closeSiteEditor() {
        keyboard?.hide()
        siteFocus.requestFocus()
        siteEditorExpanded = false
        siteUrl = KinoApp.settings.siteUrl
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        siteFocus.requestFocus()
    }
    LaunchedEffect(cacheMessage) {
        if (cacheMessage != null) {
            delay(2_000)
            cacheMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPadding, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsItem(
                title = "Адрес сайта",
                value = siteUrl,
                modifier = Modifier.focusRequester(siteFocus),
                onClick = { siteEditorExpanded = !siteEditorExpanded },
            )
            if (siteEditorExpanded) {
                SiteUrlEditor(
                    initialUrl = siteUrl,
                    onSave = { text ->
                        KinoApp.settings.siteUrl = text
                        KinoApp.repository.clearMemoryCache()
                        closeSiteEditor()
                    },
                    onReset = {
                        KinoApp.settings.resetSiteUrl()
                        KinoApp.repository.clearMemoryCache()
                        closeSiteEditor()
                    },
                )
            }
            SettingsItem(
                title = "Проверить обновления",
                onClick = { actions.checkUpdates() },
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsItem(
                    title = "Очистить кэш",
                    onClick = {
                        if (!clearingCache) {
                            clearingCache = true
                            cacheMessage = null
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        KinoApp.repository.clearMemoryCache()
                                        clearDirectoryContents(File(context.cacheDir, "images"))
                                        clearDirectoryContents(File(context.cacheDir, "http"))
                                    }
                                    cacheMessage = "Кэш очищен"
                                } catch (_: IOException) {
                                    cacheMessage = "Не удалось очистить кэш"
                                } catch (_: SecurityException) {
                                    cacheMessage = "Не удалось очистить кэш"
                                } finally {
                                    clearingCache = false
                                }
                            }
                        }
                    },
                )
                val message = if (clearingCache) "Очистка кэша…" else cacheMessage
                if (message != null) {
                    Text(message, color = KinoColors.TextSecondary)
                }
            }
            SettingsItem(
                title = "Очистить историю просмотров",
                onClick = {
                    if (!clearingHistory) {
                        clearingHistory = true
                        val history = KinoApp.library.history.value
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    history.forEach { KinoApp.library.removeFromHistory(it.url) }
                                }
                            } finally {
                                clearingHistory = false
                            }
                        }
                    }
                },
            )
            Text(
                "KinoTV $versionName",
                color = KinoColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
) {
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        headlineContent = { Text(title) },
        trailingContent = if (value == null) null else {
            {
                Text(
                    value,
                    modifier = Modifier.widthIn(max = 320.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}

@Composable
private fun SiteUrlEditor(
    initialUrl: String,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
) {
    var text by remember { mutableStateOf(initialUrl) }
    var focused by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }
    val shape = RoundedCornerShape(8.dp)

    LaunchedEffect(Unit) {
        withFrameNanos { }
        fieldFocus.requestFocus()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = SurfaceDefaults.colors(containerColor = KinoColors.Surface),
            border = Border(
                BorderStroke(2.dp, if (focused) KinoColors.FocusBorder else KinoColors.SurfaceVariant),
                shape = shape,
            ),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(fieldFocus)
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = "Адрес сайта" }
                    .padding(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = KinoColors.TextPrimary),
                cursorBrush = SolidColor(KinoColors.TextPrimary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSave(text) }),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { onSave(text) }) { Text("Сохранить") }
            Button(onClick = onReset) { Text("По умолчанию") }
        }
    }
}

private fun clearDirectoryContents(directory: File) {
    if (!directory.exists()) return
    val files = directory.listFiles() ?: throw IOException("Не удалось прочитать каталог кэша")
    files.forEach { file ->
        if (!file.deleteRecursively()) throw IOException("Не удалось удалить файл кэша")
    }
}
