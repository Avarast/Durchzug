package de.durchzug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { Surface { App() } } }
    }
}

@Composable
fun App() {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
        Text("Durchzug", style = MaterialTheme.typography.headlineMedium)
    }
}
