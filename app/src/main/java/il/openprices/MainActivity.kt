package il.openprices

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import il.openprices.ui.screens.BasketScreen
import il.openprices.ui.screens.CityPickerScreen
import il.openprices.ui.screens.HomeScreen
import il.openprices.ui.screens.SettingsScreen
import il.openprices.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dark = when (vm.themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
            LaunchedEffect(dark) {
                val bars = if (dark) SystemBarStyle.dark(AColor.TRANSPARENT)
                else SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            AppTheme(vm.themeMode) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        LaunchedEffect(Unit) { if (vm.repo.hasData()) vm.checkForUpdate() }
                        BackHandler(enabled = vm.stack.size > 1 || (vm.isTab && vm.current != Screen.Home) ||
                            (vm.current == Screen.Home && (vm.product != null || vm.query.isNotEmpty()))) {
                            when {
                                vm.current == Screen.Home && vm.product != null -> vm.onQuery("")
                                vm.current == Screen.Home && vm.query.isNotEmpty() -> vm.onQuery("")
                                !vm.back() -> vm.selectTab(Screen.Home)
                            }
                        }
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f).fillMaxWidth().imePadding()) {
                                AnimatedContent(
                                    targetState = vm.current,
                                    transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                                    label = "nav",
                                ) { screen ->
                                    when (screen) {
                                        Screen.Home -> HomeScreen(vm)
                                        Screen.Basket -> BasketScreen(vm)
                                        Screen.Settings -> SettingsScreen(vm)
                                        Screen.CityPicker -> CityPickerScreen(vm)
                                    }
                                }
                            }
                            if (vm.isTab) BottomBar(vm)
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun BottomBar(vm: AppViewModel) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        val colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        NavigationBarItem(
            selected = vm.current == Screen.Home, onClick = { vm.selectTab(Screen.Home) },
            icon = { Icon(Icons.Rounded.Search, null) }, label = { Text("חיפוש") }, colors = colors,
        )
        NavigationBarItem(
            selected = vm.current == Screen.Basket, onClick = { vm.selectTab(Screen.Basket) },
            icon = {
                BadgedBox(badge = { if (vm.list.isNotEmpty()) Badge { Text("${vm.list.size}") } }) {
                    Icon(Icons.Rounded.ShoppingBasket, null)
                }
            },
            label = { Text("הסל שלי") }, colors = colors,
        )
        NavigationBarItem(
            selected = vm.current == Screen.Settings, onClick = { vm.selectTab(Screen.Settings) },
            icon = { Icon(Icons.Rounded.Settings, null) }, label = { Text("הגדרות") }, colors = colors,
        )
    }
}
