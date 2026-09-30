package com.lamz.compressly.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompresslyTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun PrivacyBadge(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Private and secure",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "100% On-Device • Files never leave your phone",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProgressOverlayDialog(
    currentIndex: Int,
    totalCount: Int,
    fileName: String,
    percentage: Int,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Compressing...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { percentage.coerceIn(0, 100) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$currentIndex / $totalCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("cancel_processing_button")
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun InteractiveComparisonSlider(
    originalModel: Any?,
    compressedModel: Any?,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }
    val animatedPosition by animateFloatAsState(targetValue = sliderPosition, label = "slider")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val newPos = (change.position.x / size.width).coerceIn(0.05f, 0.95f)
                    sliderPosition = newPos
                }
            }
    ) {
        val context = LocalContext.current

        // 1. Compressed Image (Background layer)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(compressedModel)
                .crossfade(true)
                .build(),
            contentDescription = "Compressed Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Original Image (Clipped layer on the left)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(originalModel)
                .crossfade(true)
                .build(),
            contentDescription = "Original Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val clipWidth = size.width * animatedPosition
                    clipRect(right = clipWidth) {
                        this@drawWithContent.drawContent()
                    }
                }
        )

        // 3. Divider Line & Handle
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val dividerX = maxWidth * animatedPosition

            // Vertical line
            Box(
                modifier = Modifier
                    .offset(x = dividerX - 1.dp)
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(Color.White)
            )

            // Circular handle
            Box(
                modifier = Modifier
                    .offset(x = dividerX - 16.dp, y = (maxHeight / 2) - 16.dp)
                    .size(32.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⇄",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Labels
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = "Original (Before)",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Text(
                text = "Compressed (After)",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
