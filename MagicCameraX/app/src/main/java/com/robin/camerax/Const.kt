package com.robin.camerax

import android.Manifest

const val KEY_EVENT_ACTION = "key_event_action"
const val KEY_EVENT_EXTRA = "key_event_extra"
const val MP4_VIDEO = "video/mp4"
const val STR_JPG = "JPG"
const val STR_MP4 = "MP4"

@JvmField val EXTENSION_WHITELIST = arrayOf(STR_JPG, STR_MP4)
@JvmField val PERMISSIONS_REQUIRED_IMAGE = arrayOf(Manifest.permission.CAMERA)
@JvmField val PERMISSIONS_REQUIRED_VIDEO = arrayOf(Manifest.permission.RECORD_AUDIO)
@JvmField val PERMISSIONS_REQUIRED_PHOTO = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)