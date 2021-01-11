package com.robin.camerax.entity

import com.robin.camerax.STR_MP4
import java.io.File

data class MediaFile(var file: File) {
    val isVideo: Boolean
        get() = STR_MP4 == (file.extension.toUpperCase())
}