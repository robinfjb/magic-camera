package com.robin.camerax.repo

import io.reactivex.rxjava3.core.Observable
import java.io.File

interface PhotoRepository {
    fun getListFiles(directory: File): Observable<List<File>>
    fun onPhotoDeleted(file: File)
}