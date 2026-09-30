package com.lamz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lamz.compressly.core.common.BatchProcessingSummary
import com.lamz.compressly.feature.home.HomeScreen
import com.lamz.compressly.feature.imagecompressor.ImageCompressorScreen
import com.lamz.compressly.feature.imagecompressor.ImageCompressorViewModel
import com.lamz.compressly.feature.imageresizer.ImageResizerScreen
import com.lamz.compressly.feature.imageresizer.ImageResizerViewModel
import com.lamz.compressly.feature.pdfcompressor.PdfCompressorScreen
import com.lamz.compressly.feature.pdfcompressor.PdfCompressorViewModel
import com.lamz.compressly.feature.result.ResultScreen
import com.lamz.compressly.feature.settings.SettingsScreen
import com.lamz.compressly.feature.settings.SettingsViewModel
import com.lamz.ui.theme.CompresslyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()

            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            CompresslyTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    var lastSummary by remember { mutableStateOf<BatchProcessingSummary?>(null) }

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                onNavigateToImageCompressor = { navController.navigate("image_compressor") },
                                onNavigateToPdfCompressor = { navController.navigate("pdf_compressor") },
                                onNavigateToImageResizer = { navController.navigate("image_resizer") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }

                        composable("image_compressor") {
                            val imageCompressorViewModel: ImageCompressorViewModel = viewModel()
                            ImageCompressorScreen(
                                viewModel = imageCompressorViewModel,
                                onBack = { navController.popBackStack() },
                                onCompleted = { summary ->
                                    lastSummary = summary
                                    navController.navigate("result")
                                }
                            )
                        }

                        composable("image_resizer") {
                            val imageResizerViewModel: ImageResizerViewModel = viewModel()
                            ImageResizerScreen(
                                viewModel = imageResizerViewModel,
                                onBack = { navController.popBackStack() },
                                onCompleted = { summary ->
                                    lastSummary = summary
                                    navController.navigate("result")
                                }
                            )
                        }

                        composable("pdf_compressor") {
                            val pdfCompressorViewModel: PdfCompressorViewModel = viewModel()
                            PdfCompressorScreen(
                                viewModel = pdfCompressorViewModel,
                                onBack = { navController.popBackStack() },
                                onCompleted = { summary ->
                                    lastSummary = summary
                                    navController.navigate("result")
                                }
                            )
                        }

                        composable("result") {
                            val summary = lastSummary
                            if (summary != null) {
                                ResultScreen(
                                    summary = summary,
                                    onDone = {
                                        navController.popBackStack("home", inclusive = false)
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    navController.popBackStack("home", inclusive = false)
                                }
                            }
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
