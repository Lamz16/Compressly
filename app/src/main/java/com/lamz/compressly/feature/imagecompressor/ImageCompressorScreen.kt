package com.lamz.compressly.feature.imagecompressor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lamz.compressly.core.common.BatchProcessingSummary
import com.lamz.compressly.core.common.CompressionUiState
import com.lamz.compressly.core.ui.CompresslyTopBar
import com.lamz.compressly.core.ui.ProgressOverlayDialog
import com.lamz.compressly.core.util.FileUtils
import com.lamz.compressly.domain.model.ImagePreset
import com.lamz.compressly.domain.model.OutputImageFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageCompressorScreen(
    viewModel: ImageCompressorViewModel,
    onBack: () -> Unit,
    onCompleted: (BatchProcessingSummary) -> Unit
) {
    val selectedImages by viewModel.selectedImages.collectAsState()
    val config by viewModel.config.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.setSelectedImages(uris)
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
                title = "Image Compressor",
                onBack = onBack,
                actions = {
                    if (selectedImages.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearImages() },
                            modifier = Modifier.testTag("clear_images_button")
                        ) {
                            Text("Clear")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedImages.isNotEmpty()) {
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
                                .testTag("start_compression_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Compress, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compress ${selectedImages.size} Image${if (selectedImages.size > 1) "s" else ""}",
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
            // Pick Image Card
            if (selectedImages.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("image_picker_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Pick images",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Select Images to Compress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "JPEG, PNG, WebP supported • Single or Batch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Choose Photos")
                        }
                    }
                }
            } else {
                // Selected images preview list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected Images (${selectedImages.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Text("+ Add More")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(selectedImages) { item ->
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Remove button
                            IconButton(
                                onClick = { viewModel.removeImage(item) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        RoundedCornerShape(bottomStart = 8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Size label
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = FileUtils.formatBytes(item.sizeBytes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Compression Presets Section
            Text(
                text = "Compression Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            ImagePreset.entries.forEach { preset ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (config.preset == preset) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.updatePreset(preset) }
                        .testTag("preset_${preset.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.preset == preset,
                            onClick = { viewModel.updatePreset(preset) }
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

            // Target Size Configuration (if Target Size is selected)
            if (config.preset == ImagePreset.TARGET_SIZE) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Target File Size",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick size chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val chips = listOf(
                                "300 KB" to 300L,
                                "500 KB" to 500L,
                                "1 MB" to 1024L,
                                "2 MB" to 2048L
                            )
                            chips.forEach { (label, kb) ->
                                val isSelected = config.targetSizeBytes == kb * 1024L
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateTargetSize(kb) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        var customKbText by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = customKbText,
                            onValueChange = { str ->
                                customKbText = str.filter { it.isDigit() }
                                val kb = customKbText.toLongOrNull()
                                if (kb != null && kb > 0) {
                                    viewModel.updateTargetSize(kb)
                                }
                            },
                            label = { Text("Custom Target Size (KB)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_target_kb_input")
                        )
                    }
                }
            }

            // Custom Quality / Max Dim Configuration (if Custom is selected)
            if (config.preset == ImagePreset.CUSTOM) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Quality",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${config.manualQuality}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = config.manualQuality.toFloat(),
                            onValueChange = { viewModel.updateManualQuality(it.toInt()) },
                            valueRange = 20f..100f,
                            steps = 15,
                            modifier = Modifier.testTag("custom_quality_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Max Dimension",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${config.manualMaxDimension}px",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = config.manualMaxDimension.toFloat(),
                            onValueChange = { viewModel.updateManualMaxDimension(it.toInt()) },
                            valueRange = 720f..4096f,
                            modifier = Modifier.testTag("custom_max_dim_slider")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Output Format Selector
            Text(
                text = "Output Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutputImageFormat.entries.forEach { format ->
                    FilterChip(
                        selected = config.format == format,
                        onClick = { viewModel.updateFormat(format) },
                        label = { Text(format.title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keep EXIF Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Preserve EXIF Metadata",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Orientation is always corrected. Disabling saves extra bytes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.keepExif,
                    onCheckedChange = { viewModel.updateKeepExif(it) },
                    modifier = Modifier.testTag("keep_exif_switch")
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Overlay dialog during active processing
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
