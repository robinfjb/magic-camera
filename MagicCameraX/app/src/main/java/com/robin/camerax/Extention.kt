package com.robin.camerax

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.view.View
import androidx.fragment.app.Fragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val ANIMATION_FAST_MILLIS = 50L

fun File.createFile(format: String, extension: String): File {
    return File(this, SimpleDateFormat(format, Locale.ROOT).format(System.currentTimeMillis()) + "." + extension)
}

private fun substringAfterLast(original : String , delimiter : Char , missingDelimiterValue : String ) :String{
    val index = original.lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else original.substring(index + 1)
}

fun View.simulateClick() {
    performClick()
    isPressed = true
    invalidate()
    postDelayed(Runnable {
        invalidate()
        isPressed = false
    }, ANIMATION_FAST_MILLIS)
}

fun Fragment.share(file: File, title: String) {
    var title = title
    if (TextUtils.isEmpty(title)) {
        title = getString(R.string.share_default)
    }
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.type = "image/*"
    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(file.absolutePath))
    startActivity(Intent.createChooser(shareIntent, title))
}

fun Context.getMediaOutputDirectory(appName: String): File? {
    val appContext = applicationContext
    val files = appContext.externalMediaDirs
    var mediaDir: File? = null
    if (files != null && files.isNotEmpty()) {
        mediaDir = File(files[0], appName)
        mediaDir.mkdirs()
    }
    return if (mediaDir != null && mediaDir.exists()) {
        mediaDir
    } else {
        appContext.filesDir
    }
}

/**
 * Uri转File
 * @param uri
 * @return
 */
fun Uri.uri2File(): File {
    check(!("file" !== this.scheme)) { "Uri lacks 'file' scheme:$this" }
    checkNotNull(this.path) { "Uri path is null:$this" }
    return File(this.path)
}