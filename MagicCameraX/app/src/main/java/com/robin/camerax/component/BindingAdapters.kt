package com.robin.camerax.component

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.robin.camerax.STR_MP4
import java.io.File

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("image_src")
    fun bindImage(view: ImageView, @DrawableRes src: Int) {
        if (src != 0) {
            Glide.with(view.context).load(src).into(view)
        }
    }

    @JvmStatic
    @BindingAdapter(value = ["image_url", "url_error"], requireAll = false)
    fun bindImage(
        view: ImageView,
        url: String?,
        error: Drawable?
    ) {
        if (error == null) {
            Glide.with(view.context).load(url).into(view)
        } else {
            Glide.with(view.context).load(url).error(error).into(view)
        }
    }

    @JvmStatic
    @BindingAdapter(value = [ "file", "file_error"], requireAll = false)
    fun bindImage(
        view: ImageView,
        file: File,
        error: Drawable?
    ) {
        if (file.extension?.toUpperCase() == STR_MP4) {
            Glide.with(view.context)
                .load(file)
                .error(error)
                .into(view)
        } else {
            Glide.with(view.context).load(file).error(error).into(view)
        }
    }
}