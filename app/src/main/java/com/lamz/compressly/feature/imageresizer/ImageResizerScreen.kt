package com.lamz.compressly.feature.imageresizer

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
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import com.lamz.compressly.domain.model.OutputImageFormat
import com.lamz.compressly.domain.model.ResizeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageResizerScreen(
    viewModel: ImageResizerViewModel,
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
                title = "Image Resizer",
                onBack = onBack,
                actions = {
                    if (selectedImages.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearImages() },
                            modifier = Modifier.testTag("clear_resize_images_button")
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
                            onClick = { viewModel.startResize() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_resize_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Crop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Resize ${selectedImages.size} Image${if (selectedImages.size > 1) "s" else ""}",
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
                        .testTag("resize_picker_card")
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
                            text = "Select Images to Resize",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Downsample, scale by percentage, or custom dimensions",
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

            // Resize Mode Tabs
            Text(
                text = "Resize Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = config.mode == ResizeMode.PRESET,
                    onClick = { viewModel.updateMode(ResizeMode.PRESET) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("Preset", style = MaterialTheme.typography.labelMedium)
                }
                SegmentedButton(
                    selected = config.mode == ResizeMode.PERCENTAGE,
                    onClick = { viewModel.updateMode(ResizeMode.PERCENTAGE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("Percent", style = MaterialTheme.typography.labelMedium)
                }
                SegmentedButton(
                    selected = config.mode == ResizeMode.CUSTOM_DIMENSIONS,
                    onClick = { viewModel.updateMode(ResizeMode.CUSTOM_DIMENSIONS) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("W × H", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preset Resolution Mode
            if (config.mode == ResizeMode.PRESET) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Target Maximum Dimension",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val presets = listOf(
                            "4K (4096px)" to 4096,
                            "2K (2560px)" to 2560,
                            "Full HD (1920px)" to 1920,
                            "HD (1280px)" to 1280,
                            "Square / Web (1080px)" to 1080,
                            "Small (720px)" to 720
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { (label, dim) ->
                                FilterChip(
                                    selected = config.targetMaxDimension == dim,
                                    onClick = { viewModel.updatePresetMaxDimension(dim) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // Percentage Mode
            if (config.mode == ResizeMode.PERCENTAGE) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Scale Percentage",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${config.percentage}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = config.percentage.toFloat(),
                            onValueChange = { viewModel.updatePercentage(it.toInt()) },
                            valueRange = 10f..100f,
                            steps = 17,
                            modifier = Modifier.testTag("resize_percentage_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(25, 50, 75).forEach { pct ->
                                FilterChip(
                                    selected = config.percentage == pct,
                                    onClick = { viewModel.updatePercentage(pct) },
                                    label = { Text("$pct%") }
                                )
                            }
                        }
                    }
                }
            }

            // Custom Dimensions Mode
            if (config.mode == ResizeMode.CUSTOM_DIMENSIONS) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Width and Height",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        var widthText by remember(config.targetWidth) { mutableStateOf(config.targetWidth.toString()) }
                        var heightText by remember(config.targetHeight) { mutableStateOf(config.targetHeight.toString()) }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = widthText,
                                onValueChange = {
                                    widthText = it.filter { ch -> ch.isDigit() }
                                    val w = widthText.toIntOrNull() ?: 1
                                    viewModel.updateDimensions(w, config.targetHeight)
                                },
                                label = { Text("Width") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("resize_width_input")
                            )

                            IconButton(
                                onClick = { viewModel.updateMaintainAspectRatio(!config.maintainAspectRatio) },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (config.maintainAspectRatio) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Toggle aspect ratio lock",
                                    tint = if (config.maintainAspectRatio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedTextField(
                                value = heightText,
                                onValueChange = {
                                    heightText = it.filter { ch -> ch.isDigit() }
                                    val h = heightText.toIntOrNull() ?: 1
                                    viewModel.updateDimensions(config.targetWidth, h)
                                },
                                label = { Text("Height") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("resize_height_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (config.maintainAspectRatio) "Aspect ratio locked" else "Free scaling",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quality
            Text(
                text = "Resampling Quality: ${config.quality}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = config.quality.toFloat(),
                onValueChange = { viewModel.updateQuality(it.toInt()) },
                valueRange = 50f..100f,
                modifier = Modifier.testTag("resize_quality_slider")
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (uiState is CompressionUiState.Processing) {
            val state = uiState as CompressionUiState.Processing
            ProgressOverlayDialog(
                currentIndex = state.currentIndex,
                totalCount = state.totalCount,
                fileName = state.currentFileName,
                percentage = state.progressPercentage,
                onCancel = { viewModel.cancelResize() }
            )
        }
    }
}
