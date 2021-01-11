package com.robin.camerax.repo

import android.net.Uri
import io.reactivex.rxjava3.core.Observable
import java.io.File

interface CameraRepository {
    fun getThumnailPhoto(directory: File): Observable<Uri>
    fun onImageSaved(savedUri: Uri)
}