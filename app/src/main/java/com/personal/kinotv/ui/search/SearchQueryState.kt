package com.personal.kinotv.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

// AppShell удаляет вкладку из композиции; сохраняем только текст запроса.
internal class SearchQueryState(private val savedState: SavedStateHandle) : ViewModel() {
    var query: String
        get() = savedState["query"] ?: ""
        set(value) {
            savedState["query"] = value
        }
}
