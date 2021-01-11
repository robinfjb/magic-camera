package com.robin.camerax.component

import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

abstract class DataBindingAdapter<T, V : ViewDataBinding>(diffCallback: DiffUtil.ItemCallback<T>)
    : ListAdapter<T, DataBindingViewHolder<V>>(AsyncDifferConfig.Builder<T>(diffCallback).build()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBindingViewHolder<V> {
        val binding = createBinding(parent, viewType)
        return createViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: DataBindingViewHolder<V>,
        position: Int
    ) {
        bind(holder.binding, getItemByPos(position))
        holder.binding.executePendingBindings()
    }

    protected abstract fun createBinding(parent: ViewGroup, viewType: Int): ViewDataBinding
    protected abstract fun createViewHolder(binding: ViewDataBinding): DataBindingViewHolder<V>
    protected abstract fun bind(binding: ViewDataBinding, item: T?)
    protected abstract fun getItemByPos(position: Int): T?
}