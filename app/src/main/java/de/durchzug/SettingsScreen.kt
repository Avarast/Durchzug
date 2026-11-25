package de.durchzug

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section("Standort") {
                OutlinedTextField(
                    value = vm.query,
                    onValueChange = { vm.query = it },
                    label = { Text("Ort suchen") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.search() }),
                    trailingIcon = {
                        TextButton(onClick = { vm.search() }, enabled = vm.query.isNotBlank()) { Text("Suchen") }
                    },
                )
                if (vm.searching) LinearProgressIndicator(Modifier.fillMaxWidth())
                vm.searchError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                vm.places.forEach { p ->
                    Text(p.label, Modifier.fillMaxWidth().clickable { vm.select(p); onBack() }.padding(vertical = 10.dp))
                }
                vm.place?.let { Text("Gewählt: ${it.label}", fontWeight = FontWeight.SemiBold) }
            }

            Section("Wetterdaten") {
                SwitchRow(
                    title = "Beim Start aktualisieren",
                    description = "Ruft beim App-Start neue Wetterdaten ab, wenn die gespeicherten älter als eine Stunde sind. " +
                        "Jeder Abruf zählt gegen das API-Kontingent.",
                    checked = vm.autoRefresh,
                    onCheckedChange = { vm.autoRefresh = it },
                )
            }
        }
    }
}

@Composable
internal fun SwitchRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
