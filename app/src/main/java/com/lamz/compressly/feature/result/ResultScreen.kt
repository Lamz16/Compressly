package com.lamz.compressly.feature.result

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lamz.compressly.core.common.BatchProcessingSummary
import com.lamz.compressly.core.storage.StorageManager
import com.lamz.compressly.core.ui.CompresslyTopBar
import com.lamz.compressly.core.ui.InteractiveComparisonSlider
import com.lamz.compressly.core.util.FileUtils

@Composable
fun ResultScreen(
    summary: BatchProcessingSummary,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val storageManager = remember { StorageManager(context) }
    var selectedItemIndex by remember { mutableIntStateOf(0) }
    val results = summary.results
    val currentItem = results.getOrNull(selectedItemIndex)

    var savedMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CompresslyTopBar(
                title = "Compression Result",
                onBack = onDone,
                actions = {
                    IconButton(
                        onClick = onDone,
                        modifier = Modifier.testTag("result_done_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Process again")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentItem?.outputFile != null) {
                                val isPdf = currentItem.outputFormat.equals("PDF", ignoreCase = true)
                                val shareIntent = storageManager.createShareIntent(currentItem.outputFile, isPdf)
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Compressed File"))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("share_result_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }

                    Button(
                        onClick = {
                            if (currentItem?.outputFile != null) {
                                val isPdf = currentItem.outputFormat.equals("PDF", ignoreCase = true)
                                val uri = storageManager.exportToPublicStorage(currentItem.outputFile, isPdf)
                                if (uri != null) {
                                    Toast.makeText(context, "Saved to ${if (isPdf) "Documents/Compressly" else "Pictures/Compressly"}", Toast.LENGTH_SHORT).show()
                                    savedMessage = "File saved successfully!"
                                } else {
                                    Toast.makeText(context, "Could not save to public storage.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp)
                            .testTag("save_result_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Device")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Overall reduction banner
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "${summary.averageSavedPercentage}% Smaller",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "Saved ${FileUtils.formatBytes(summary.totalSavedBytes)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${FileUtils.formatBytes(summary.totalOriginalBytes)}  →  ${FileUtils.formatBytes(summary.totalOutputBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // If image: show interactive Before / After slider
            if (currentItem != null && currentItem.outputFile != null && !currentItem.outputFormat.equals("PDF", ignoreCase = true)) {
                item {
                    Text(
                        text = "Interactive Comparison",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Drag the slider to compare original vs compressed detail",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    InteractiveComparisonSlider(
                        originalModel = currentItem.originalUri,
                        compressedModel = currentItem.outputFile,
                        modifier = Modifier.testTag("interactive_comparison_slider")
                    )
                }
            }

            // Current item stats card
            if (currentItem != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = currentItem.originalName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatBox(label = "Original", value = FileUtils.formatBytes(currentItem.originalSizeBytes))
                                StatBox(label = "Compressed", value = FileUtils.formatBytes(currentItem.outputSizeBytes))
                                StatBox(label = "Saved", value = "${currentItem.savedPercentage}%")
                            }

                            if (currentItem.originalWidth > 0 && currentItem.outputWidth > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Resolution",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${currentItem.originalWidth} × ${currentItem.originalHeight}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .padding(horizontal = 2.dp)
                                        )
                                        Text(
                                            text = "${currentItem.outputWidth} × ${currentItem.outputHeight}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (currentItem.outputFormat.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Format",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${currentItem.originalFormat} → ${currentItem.outputFormat}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // If batch: list of all items
            if (results.size > 1) {
                item {
                    Text(
                        text = "All Processed Files (${results.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(results) { item ->
                    val isSelected = results.indexOf(item) == selectedItemIndex
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedItemIndex = results.indexOf(item)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.originalName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${FileUtils.formatBytes(item.originalSizeBytes)} → ${FileUtils.formatBytes(item.outputSizeBytes)} (${item.savedPercentage}% saved)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
