package de.durchzug

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Match the window background to a forced app theme to avoid a flash before Compose draws.
        val forcedBackground = when (Storage(this).themeMode.value) {
            ThemeMode.DARK -> Color.BLACK
            ThemeMode.LIGHT -> Color.WHITE
            ThemeMode.SYSTEM -> null
        }
        forcedBackground?.let { window.setBackgroundDrawable(ColorDrawable(it)) }
        setContent {
            val vm: MainViewModel = viewModel()
            val dark = vm.themeMode.isDark()
            // Keep system bar icons readable when the app theme differs from the system theme.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            DurchzugTheme(dark = dark, pureBlack = vm.pureBlack) {
                Surface { App(vm) }
            }
        }
    }
}

@Composable
fun App(vm: MainViewModel = viewModel()) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showSettings) { showSettings = false }

    if (showSettings) {
        SettingsScreen(vm, onBack = { showSettings = false })
    } else {
        MainScreen(vm, onOpenSettings = { showSettings = true })
    }
}
