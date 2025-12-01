package com.iydheko.palabrowser.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun extractDomain(url: String): String {
    return try {
        java.net.URL(url).host.removePrefix("www.")
    } catch (_: Exception) {
        url // fallback to full url if it's not a valid URL
    }
}

fun isDeepLink(url: String): Boolean {
    return try {
        val path = java.net.URL(url).path
        path.isNotEmpty() && path != "/"
    } catch (_: Exception) {
        false
    }
}

fun getBaseUrl(url: String): String {
    return try {
        val u = java.net.URL(url)
        "${u.protocol}://${u.host}"
    } catch (_: Exception) {
        url
    }
}

fun isPhotoPickerAvailable(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

fun createImageFile(context: Context): File? {
    return try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        File.createTempFile(imageFileName, ".jpg", storageDir)
    } catch (_: Exception) {
        // Handle exception
        null
    }
}
