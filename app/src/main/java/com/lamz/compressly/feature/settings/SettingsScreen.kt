package com.lamz.compressly.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lamz.compressly.core.ui.CompresslyTopBar
import com.lamz.compressly.core.ui.PrivacyBadge

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val defaultPreset by viewModel.defaultPreset.collectAsState()
    val defaultFormat by viewModel.defaultFormat.collectAsState()
    val stripExif by viewModel.stripExif.collectAsState()
    val defaultPdfStrategy by viewModel.defaultPdfStrategy.collectAsState()

    Scaffold(
        topBar = {
            CompresslyTopBar(
                title = "Settings",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Privacy information card
            PrivacyBadge(modifier = Modifier.fillMaxWidth())

            // Theme Setting Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "App Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val themes = listOf(
                        "SYSTEM" to "System Default",
                        "LIGHT" to "Light",
                        "DARK" to "Dark"
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themes.forEachIndexed { index, (key, label) ->
                            SegmentedButton(
                                selected = themeMode == key,
                                onClick = { viewModel.setThemeMode(key) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = themes.size)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Image Compression Defaults
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Photo, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Image Defaults", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Default Compression Preset", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))

                    val presets = listOf(
                        "BALANCED" to "Balanced (Recommended)",
                        "HIGH" to "High Quality",
                        "MAXIMUM" to "Maximum Quality",
                        "SMALL" to "Small Size"
                    )

                    presets.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setDefaultPreset(key) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = defaultPreset == key,
                                onClick = { viewModel.setDefaultPreset(key) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Default Output Format", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))

                    val formats = listOf(
                        "AUTO" to "Smart Auto",
                        "JPEG" to "JPEG",
                        "WEBP" to "WebP",
                        "PNG" to "PNG"
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        formats.forEach { (key, label) ->
                            FilterChip(
                                selected = defaultFormat == key,
                                onClick = { viewModel.setDefaultFormat(key) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Strip Sensitive EXIF", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(text = "Removes location and camera metadata for privacy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = stripExif,
                            onCheckedChange = { viewModel.setStripExif(it) }
                        )
                    }
                }
            }

            // PDF Defaults
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "PDF Defaults", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val pdfStrats = listOf(
                        "SMART" to "Smart (Auto-detect text vs scan)",
                        "NATIVE" to "Native Optimization (Keep text selectable)",
                        "SCANNED" to "Scanned Document Mode"
                    )

                    pdfStrats.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setDefaultPdfStrategy(key) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = defaultPdfStrategy == key,
                                onClick = { viewModel.setDefaultPdfStrategy(key) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // About & Privacy Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "About & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Compressly v1.0.0\nBuilt with Kotlin, Jetpack Compose, and Material 3.\n\nAll image and PDF transformations are computed locally on your device's CPU/GPU. No servers, no tracking, and no external uploads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
