package com.robin.camerax.entity.mapper

import com.robin.camerax.entity.MediaFile
import java.io.File
import java.util.*

object EntityPresentationMapper {

    fun transArray2List(file: Array<File>?): ArrayList<File> {
        val result = ArrayList<File>()
        if (file != null) {
            for (f in file) {
                result.add(f)
            }
        }
        return result
    }

    fun transFile2MediaFile(files: ArrayList<File>?): ArrayList<MediaFile> {
        val result = arrayListOf<MediaFile>()
        if (files != null) {
            for (f in files) {
                result.add(MediaFile(f))
            }
        }
        return result
    }
}