# Compressly

Compressly is a modern, production-ready, offline-first Android application designed to compress and resize images and PDFs with high visual fidelity and maximum privacy.

## Features

1. **Image Compressor**
   - Single & batch compression using Android Photo Picker.
   - Presets: Maximum Quality, High Quality, Balanced, Small Size, Target File Size (KB/MB adaptive binary search), and Custom.
   - EXIF orientation auto-correction with privacy-preserving metadata stripping.
   - Transparent image alpha preservation (WebP / PNG) and solid background fallback for JPEG.
   - Out-of-memory prevention via sampled decode and stream processing.

2. **PDF Compressor**
   - **Strategy A — Native PDF Optimization**: Retains selectable text, vectors, and embedded fonts while downscaling and recompressing embedded raster images.
   - **Strategy B — Scanned Document Mode**: High-speed DPI-targeted rasterization and JPEG compression for multi-page invoices, documents, and book scans.
   - **Smart Detection**: Automatically analyzes document text density to pick the optimal strategy.

3. **Image Resizer**
   - Percentage scaling (e.g., 25%, 50%, 75%).
   - Preset resolutions (4K, 2K, 1080p Full HD, 720p HD).
   - Custom Width × Height with aspect ratio locking.

4. **Interactive Comparison & Results**
   - Real-time before/after interactive draggable comparison slider.
   - Detailed metric breakdown: Original Size, Compressed Size, Saved Percentage, and Resolution changes.
   - Scoped storage export to `Pictures/Compressly` and `Documents/Compressly`.
   - FileProvider-based native sharing.

5. **100% On-Device & Privacy Focused**
   - All compression runs locally on-device. No cloud dependencies or internet connections required.

---

## Project Structure

```
app/src/main/java/com/example/
├── MainActivity.kt                      // Navigation Host & Edge-to-Edge setup
├── compressly/
│   ├── CompresslyApplication.kt         // Application initialization (PDFBox loader)
│   ├── core/
│   │   ├── common/                      // Result & state models (ProcessingItemResult, BatchSummary)
│   │   ├── storage/                     // Scoped storage, MediaStore & DataStore preferences
│   │   ├── ui/                          // Reusable M3 top bar, dialogs, slider component
│   │   └── util/                        // FileUtils, byte formatting, unique file naming
│   ├── data/
│   │   ├── compressor/                  // ImageCompressorEngine (adaptive binary search, decode bounds)
│   │   ├── pdf/                         // PdfCompressorEngine (Native PDFBox & PdfRenderer Scanned)
│   │   └── resizer/                     // ImageResizerEngine (aspect ratio scaling)
│   ├── domain/
│   │   └── model/                       // Config models (Presets, Format, Strategies)
│   └── feature/
│       ├── home/                        // Modern Material 3 Home Dashboard
│       ├── imagecompressor/             // Image Compressor screen & ViewModel
│       ├── imageresizer/                // Image Resizer screen & ViewModel
│       ├── pdfcompressor/               // PDF Compressor screen & ViewModel
│       ├── result/                      // Result screen with Before/After comparison slider
│       └── settings/                    // Preferences screen (Theme, defaults, privacy)
└── ui/theme/                            // Material 3 Dynamic Light & Dark Color Schemes
```

---

## Build Requirements

- JDK 17 or JDK 21
- Android Studio Ladybug | 2024.2+ or Android SDK Build Tools 36
- Min SDK: 24 (Android 7.0)
- Target / Compile SDK: 36

---

## Building the Project

### Debug Build
```bash
./gradlew assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build with Keystore
1. Copy `keystore.properties.example` to `keystore.properties`:
   ```bash
   cp keystore.properties.example keystore.properties
   ```
2. Fill in your release keystore credentials:
   ```properties
   storeFile=/path/to/my-release-key.jks
   storePassword=YOUR_STORE_PASSWORD
   keyAlias=YOUR_KEY_ALIAS
   keyPassword=YOUR_KEY_PASSWORD
   ```
3. Run release build tasks:
   ```bash
   ./gradlew assembleRelease
   ```
   or generate an Android App Bundle (AAB):
   ```bash
   ./gradlew bundleRelease
   ```

### Output Locations
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **Release AAB**: `app/build/outputs/bundle/release/app-release.aab`
- **R8 Proguard Mapping**: `app/build/outputs/mapping/release/mapping.txt`

### R8 & Minification
Release builds have `isMinifyEnabled = true`, `isShrinkResources = true`, and `isDebuggable = false`. Rules in `app/proguard-rules.pro` retain PDFBox, Coroutines, and DataStore components while shrinking and obfuscating release binaries.
