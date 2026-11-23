package il.openprices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import il.openprices.ui.screens.BasketScreen
import il.openprices.ui.screens.CityPickerScreen
import il.openprices.ui.screens.HomeScreen
import il.openprices.ui.screens.ProductScreen
import il.openprices.ui.screens.SettingsScreen
import il.openprices.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme(vm.themeMode) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        LaunchedEffect(Unit) { if (vm.repo.hasData()) vm.checkForUpdate() }
                        BackHandler(enabled = vm.stack.size > 1) { vm.back() }
                        AnimatedContent(
                            targetState = vm.current,
                            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                            label = "nav",
                        ) { screen ->
                            when (screen) {
                                Screen.Home -> HomeScreen(vm)
                                is Screen.ProductDetails -> ProductScreen(vm)
                                Screen.Basket -> BasketScreen(vm)
                                Screen.Settings -> SettingsScreen(vm)
                                Screen.CityPicker -> CityPickerScreen(vm)
                            }
                        }
                    }
                }
            }
        }
    }
}
