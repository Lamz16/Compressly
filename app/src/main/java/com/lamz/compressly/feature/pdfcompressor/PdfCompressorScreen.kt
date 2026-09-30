package com.lamz.compressly.feature.pdfcompressor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lamz.compressly.core.common.BatchProcessingSummary
import com.lamz.compressly.core.common.CompressionUiState
import com.lamz.compressly.core.ui.CompresslyTopBar
import com.lamz.compressly.core.ui.ProgressOverlayDialog
import com.lamz.compressly.core.util.FileUtils
import com.lamz.compressly.domain.model.PdfDpiPreset
import com.lamz.compressly.domain.model.PdfStrategy

@Composable
fun PdfCompressorScreen(
    viewModel: PdfCompressorViewModel,
    onBack: () -> Unit,
    onCompleted: (BatchProcessingSummary) -> Unit
) {
    val selectedPdf by viewModel.selectedPdf.collectAsState()
    val config by viewModel.config.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.setSelectedPdf(uri)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is CompressionUiState.Completed) {
            onCompleted((uiState as CompressionUiState.Completed).summary)
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            CompresslyTopBar(
                title = "PDF Compressor",
                onBack = onBack,
                actions = {
                    if (selectedPdf != null) {
                        TextButton(
                            onClick = { viewModel.clearPdf() },
                            modifier = Modifier.testTag("clear_pdf_button")
                        ) {
                            Text("Clear")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedPdf != null) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startCompression() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_pdf_compression_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Compress, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compress PDF Document",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Pick PDF Document Card
            if (selectedPdf == null) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        }
                        .testTag("pdf_picker_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "Pick PDF document",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Select PDF Document",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Native optimization keeps text searchable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Choose PDF File")
                        }
                    }
                }
            } else {
                // PDF Selected Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedPdf!!.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${FileUtils.formatBytes(selectedPdf!!.sizeBytes)} • ${selectedPdf!!.pageCount} Page${if (selectedPdf!!.pageCount > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { viewModel.clearPdf() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Strategy Selector
            Text(
                text = "Optimization Strategy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            PdfStrategy.entries.forEach { strat ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (config.strategy == strat) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.updateStrategy(strat) }
                        .testTag("strategy_${strat.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.strategy == strat,
                            onClick = { viewModel.updateStrategy(strat) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strat.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = strat.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // DPI / Quality Presets
            Text(
                text = "Compression Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            PdfDpiPreset.entries.forEach { preset ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (config.dpiPreset == preset) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.updateDpiPreset(preset) }
                        .testTag("dpi_preset_${preset.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.dpiPreset == preset,
                            onClick = { viewModel.updateDpiPreset(preset) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (uiState is CompressionUiState.Processing) {
            val state = uiState as CompressionUiState.Processing
            ProgressOverlayDialog(
                currentIndex = state.currentIndex,
                totalCount = state.totalCount,
                fileName = state.currentFileName,
                percentage = state.progressPercentage,
                onCancel = { viewModel.cancelCompression() }
            )
        }
    }
}
