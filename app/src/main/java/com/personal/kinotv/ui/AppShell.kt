package com.personal.kinotv.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.personal.kinotv.data.Card
import com.personal.kinotv.data.PlayerSource
import com.personal.kinotv.data.Section
import com.personal.kinotv.data.Sections
import com.personal.kinotv.ui.category.SectionScreen
import com.personal.kinotv.ui.common.AppActions
import com.personal.kinotv.ui.common.LocalAppActions
import com.personal.kinotv.ui.details.DetailsScreen
import com.personal.kinotv.ui.home.HomeScreen
import com.personal.kinotv.ui.library.LibraryScreen
import com.personal.kinotv.ui.search.SearchScreen
import com.personal.kinotv.ui.settings.SettingsScreen
import com.personal.kinotv.ui.theme.KinoColors

/** Вкладки сверху, как в Netflix. */
private sealed class TabItem(val title: String) {
    data object Home : TabItem("Главная")
    class Category(val section: Section, title: String = section.title) : TabItem(title)
    data object Search : TabItem("Поиск")
    data object Library : TabItem("Моё")
    data object Settings : TabItem("Настройки")
}

private val tabs = listOf(
    TabItem.Home,
    TabItem.Category(Sections.MOVIES),
    TabItem.Category(Sections.SERIES),
    TabItem.Category(Sections.ANIME),
    TabItem.Category(Sections.CARTOONS),
    TabItem.Library,
    TabItem.Search,
    TabItem.Settings,
)

object Routes {
    const val MAIN = "main"
    const val DETAILS = "details?url={url}"
    fun details(url: String) = "details?url=${Uri.encode(url)}"
}

/** Корень интерфейса: главный экран со вкладками и экран тайтла поверх него. */
@Composable
fun AppNavigation(
    play: (Card, PlayerSource?) -> Unit,
    checkUpdates: () -> Unit,
    onExitRequest: () -> Unit,
) {
    val nav = rememberNavController()
    val actions = remember(nav) {
        object : AppActions {
            override fun openDetails(card: Card) = nav.navigate(Routes.details(card.url))
            override fun play(card: Card, source: PlayerSource?) = play(card, source)
            override fun checkUpdates() = checkUpdates()
        }
    }

    CompositionLocalProvider(LocalAppActions provides actions) {
        NavHost(
            navController = nav,
            startDestination = Routes.MAIN,
            modifier = Modifier.fillMaxSize().background(KinoColors.Background),
        ) {
            composable(Routes.MAIN) { MainTabs(onExitRequest) }
            composable(
                Routes.DETAILS,
                arguments = listOf(navArgument("url") { type = NavType.StringType }),
            ) { entry ->
                DetailsScreen(url = entry.arguments?.getString("url").orEmpty())
            }
        }
    }
}

@Composable
private fun MainTabs(onExitRequest: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    // «Назад» на любой вкладке, кроме главной, возвращает на главную; на главной — выход
    BackHandler { if (selected != 0) selected = 0 else onExitRequest() }

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selected,
            modifier = Modifier.padding(start = 40.dp, top = 16.dp, bottom = 8.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == selected,
                    onFocus = { selected = index },
                    onClick = { selected = index },
                ) {
                    Text(tab.title, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            when (val tab = tabs[selected]) {
                TabItem.Home -> HomeScreen()
                is TabItem.Category -> SectionScreen(tab.section)
                TabItem.Search -> SearchScreen()
                TabItem.Library -> LibraryScreen()
                TabItem.Settings -> SettingsScreen()
            }
        }
    }
}
