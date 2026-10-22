package com.repforge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repforge.*
import com.repforge.ui.components.ThemedHapticCheckbox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Beállítások", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Témaválasztó
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Alkalmazás Téma", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val themes = listOf("DARK" to "Sötét", "AMOLED" to "AMOLED Fekete", "MIDNIGHT" to "Éjféli Kék", "EMERALD" to "Smaragdzöld")
                    themes.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = state.selectedTheme.equals(key, ignoreCase = true),
                                onClick = { viewModel.setTheme(key) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            }

            // v1.3.71: Egységes tematikus haptikus jelzés kapcsoló
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Haptikus Visszajelzés", fontWeight = FontWeight.Bold)
                        Text("Rezgés gombok és sorozatok teljesítésekor", style = MaterialTheme.typography.bodySmall)
                    }
                    ThemedHapticCheckbox(
                        checked = state.hapticFeedbackEnabled,
                        onCheckedChange = { viewModel.toggleHaptics(it) }
                    )
                }
            }

            // Nyelvválasztó (HU, EN, DE, RO)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nyelv / Language", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val langs = listOf("hu" to "Magyar", "en" to "English", "de" to "Deutsch", "ro" to "Română")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        langs.forEach { (code, title) ->
                            FilterChip(
                                selected = state.selectedLanguage == code,
                                onClick = { viewModel.setLanguage(code) },
                                label = { Text(title) }
                            )
                        }
                    }
                }
            }
        }
    }
}
