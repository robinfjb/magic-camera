package com.robin.camerax.repo

import android.media.MediaScannerConnection
import com.robin.camerax.EXTENSION_WHITELIST
import com.robin.libutil.AppUtil
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.io.File
import java.util.*

class PhotoRepositoryImpl : PhotoRepository{
    private val fileSubject = BehaviorSubject.create<List<File>>()

    override fun getListFiles(directory: File): Observable<List<File>> {
        directory?.let {
            if (directory.exists() && directory.isDirectory) {
                val mediaFiles = directory.listFiles()
                mediaFiles?.let {
                    if (mediaFiles.isNotEmpty()) {
                        val sortedFiles = mediaFiles.filter {file ->
                            EXTENSION_WHITELIST.any {str -> str == file.extension.toUpperCase(Locale.ROOT)}
                        }.sortedBy {
                            it.lastModified()
                        }
                        fileSubject.onNext(sortedFiles)
                    } else {
                        fileSubject.onError(IllegalStateException("directory is empty"))
                    }
                }
            } else {
                fileSubject.onError(IllegalStateException("directory not exist or not as a directory"))
            }
        }
        return fileSubject
    }

    override fun onPhotoDeleted(file: File) {
        MediaScannerConnection.scanFile(
                AppUtil.getApp(), arrayOf(file.absolutePath),
                null,
                null)
    }
}