package com.lamz.compressly

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class CompresslyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            PDFBoxResourceLoader.init(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
