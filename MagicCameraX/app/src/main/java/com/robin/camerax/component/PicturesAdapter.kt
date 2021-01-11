package com.robin.camerax.component

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import com.robin.camerax.R
import com.robin.camerax.databinding.ItemPictureBinding
import com.robin.camerax.databinding.ItemPictureEmptyBinding
import com.robin.camerax.entity.MediaFile
import com.robin.camerax.entity.mapper.EntityPresentationMapper
import com.robin.libutil.LogUtil
import java.io.File
import java.util.*

class PicturesAdapter(private val callback: ((Boolean, Uri) -> Unit)) : DataBindingAdapter<MediaFile, ViewDataBinding>(
        diffCallback = object : DiffUtil.ItemCallback<MediaFile>() {
    override fun areItemsTheSame(oldItem: MediaFile, newItem: MediaFile): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: MediaFile, newItem: MediaFile): Boolean {
        return oldItem.file.absolutePath == oldItem.file.absolutePath
    }
}) {
    private val mFiles: ArrayList<MediaFile> = arrayListOf()

    fun setFiles(files: ArrayList<File>?) {
        val newData: ArrayList<MediaFile> =
            EntityPresentationMapper.transFile2MediaFile(files)
        mFiles.clear()
        mFiles.addAll(newData)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (mFiles.isEmpty()) {
            TYPE_EMPTY
        } else {
            TYPE_NORMAL
        }
    }

    override fun getItemCount(): Int {
        return if (mFiles.isEmpty()) 1 else mFiles.size
    }

    override fun createBinding(parent: ViewGroup, viewType: Int): ViewDataBinding {
        return if (viewType == TYPE_EMPTY) {
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_picture_empty,
                parent,
                false
            )
        } else {
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_picture,
                parent,
                false
            )
        }
    }

    override fun createViewHolder(binding: ViewDataBinding): DataBindingViewHolder<ViewDataBinding> {
        return if (binding is ItemPictureEmptyBinding) {
            EmptyViewHolder(binding as ItemPictureEmptyBinding)
        } else if (binding is ItemPictureBinding) {
            PicturesViewHolder(binding as ItemPictureBinding)
        } else {
            DataBindingViewHolder<ViewDataBinding>(binding)
        }
    }

    override fun bind(binding: ViewDataBinding, item: MediaFile?) {
        if (binding is ItemPictureEmptyBinding) {
            //do nothing
        } else if (binding is ItemPictureBinding) {
            item?.apply {
                (binding as ItemPictureBinding).item = item
                (binding as ItemPictureBinding).imagePreview.setOnClickListener {
                    callback.invoke(item.isVideo, Uri.parse(item.file.absolutePath))
                }
            }
        }
    }

    override fun getItemByPos(position: Int): MediaFile? {
        return if (mFiles.isEmpty()) null else mFiles[position]
    }

    fun shareImage(currentPage: Int, callback: ((File) -> Unit)) {
        if (currentPage < mFiles.size) {
            callback.invoke(mFiles[currentPage].file)
        }
    }

    fun deleteImage(currentPage: Int, callback: ((File) -> Unit)) {
        if (currentPage < mFiles.size) {
            val picture: File = mFiles[currentPage].file
            if (picture.exists() && picture.delete()) {
                mFiles.removeAt(currentPage)
                notifyItemRemoved(currentPage)
                callback.invoke(picture)
            } else {
                LogUtil.e("delete file failed:" + picture.absolutePath)
            }
            if (mFiles.isEmpty()) {
                notifyDataSetChanged()
            }
        }
    }

    private inner class EmptyViewHolder(binding: ItemPictureEmptyBinding) : DataBindingViewHolder<ItemPictureEmptyBinding>(binding)
    private inner class PicturesViewHolder(binding: ItemPictureBinding) : DataBindingViewHolder<ItemPictureBinding>(binding)

    companion object {
        const val TYPE_NORMAL = 1
        const val TYPE_EMPTY = 2
    }

}