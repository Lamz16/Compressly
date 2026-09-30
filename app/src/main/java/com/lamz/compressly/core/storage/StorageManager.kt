package com.lamz.compressly.core.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class StorageManager(private val context: Context) {

    fun getProcessingDir(): File {
        val dir = File(context.cacheDir, "compressly_work")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getOutputDir(): File {
        val dir = File(context.filesDir, "compressed_outputs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun exportToPublicStorage(file: File, isPdf: Boolean): Uri? {
        val resolver = context.contentResolver
        val mimeType = if (isPdf) "application/pdf" else {
            when (file.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (isPdf) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/Compressly")
                } else {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Compressly")
                }
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val collection = if (isPdf) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }

            val itemUri = resolver.insert(collection, contentValues)
            if (itemUri != null) {
                try {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        FileInputStream(file).use { input ->
                            input.copyTo(out)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                    itemUri
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else {
                null
            }
        } else {
            // Legacy external storage
            val targetDir = if (isPdf) {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Compressly")
            } else {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Compressly")
            }
            if (!targetDir.exists()) targetDir.mkdirs()
            val destFile = File(targetDir, file.name)
            try {
                FileInputStream(file).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(destFile)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun getShareableUri(file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun createShareIntent(file: File, isPdf: Boolean): Intent {
        val uri = getShareableUri(file)
        val mimeType = if (isPdf) "application/pdf" else {
            when (file.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun cleanTempFiles() {
        try {
            getProcessingDir().listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
