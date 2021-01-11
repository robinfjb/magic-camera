package com.robin.camerax.repo

import android.content.Intent
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import com.robin.camerax.EXTENSION_WHITELIST
import com.robin.camerax.uri2File
import com.robin.libutil.AppUtil
import com.robin.libutil.LogUtil
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.io.File
import java.util.*

class CameraRepositoryImpl : CameraRepository{
    private val thumbnailSubject = BehaviorSubject.create<Uri>()

    override fun getThumnailPhoto(directory: File): Observable<Uri> {
        val mediaFiles = directory.listFiles()
        if (mediaFiles != null && mediaFiles.isNotEmpty()) {
            val thumbnail = mediaFiles.filter { file ->
                EXTENSION_WHITELIST.any { str ->
                    str == file.extension.toUpperCase(Locale.ROOT)
                }
            }.minBy {
                it.lastModified()
            }
            thumbnail?.apply {
                if(this.exists()) {
                    LogUtil.d("onNext:" + thumbnail.absolutePath)
                    thumbnailSubject.onNext(Uri.fromFile(thumbnail))
                } else {
                    thumbnailSubject.onError(NullPointerException())
                }
            } ?: thumbnailSubject.onError(NullPointerException())
        } else {
            thumbnailSubject.onError(NullPointerException())
        }
        return thumbnailSubject
    }

    override fun onImageSaved(savedUri: Uri) {
        // Implicit broadcasts will be ignored for devices running API level >= 24
        // so if you only target API level 24+ you can remove this statement

        // Implicit broadcasts will be ignored for devices running API level >= 24
        // so if you only target API level 24+ you can remove this statement
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            AppUtil.getApp().sendBroadcast(Intent(Camera.ACTION_NEW_PICTURE, savedUri))
        }
        // If the folder selected is an external media directory, this is
        // unnecessary but otherwise other apps will not be able to access our
        // images unless we scan them using [MediaScannerConnection]
        val savedFile: File = savedUri.uri2File()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(savedFile.extension)
        MediaScannerConnection.scanFile(
                AppUtil.getApp(), arrayOf(savedFile.absolutePath), arrayOf(mimeType)) { _: String?, uri: Uri -> LogUtil.d("Image capture scanned into media store: $uri") }
    }
}