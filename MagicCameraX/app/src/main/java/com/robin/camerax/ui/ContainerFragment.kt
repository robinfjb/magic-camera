package com.robin.camerax.ui

import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.robin.camerax.R
import com.robin.camerax.databinding.FragmentContainerBinding
import com.robin.camerax.viewmodel.ContainerViewModel

class ContainerFragment : BaseFragment<FragmentContainerBinding, ContainerViewModel>() {
    private lateinit var pagerAdapter: FragmentStateAdapter
    private lateinit var onPageChangeCallback: OnPageChangeCallback
    private val fragments: SparseArray<Fragment> = SparseArray()

    public override fun binding(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): FragmentContainerBinding {
        return DataBindingUtil.inflate(inflater, R.layout.fragment_container, container, false)
    }

    override fun onViewCreatedInner() {
        fragments.append(CAMERA_POS, CameraFragment())
        fragments.append(VIDEO_POS, VideoFragment())
        pagerAdapter = ScreenSlidePagerAdapter(this)
        binding.pager.adapter = pagerAdapter
        onPageChangeCallback = OnPageChangeCallbackImpl()
        binding.pager.registerOnPageChangeCallback(onPageChangeCallback)
    }

    override fun permissons(): Array<String>? {
        return null
    }

    override fun afterHasPermisson() {}

    private inner class OnPageChangeCallbackImpl : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {}
    }

    private inner class ScreenSlidePagerAdapter(fa: Fragment) : FragmentStateAdapter(fa!!) {
        override fun createFragment(position: Int): Fragment {
            return fragments[position]!!
        }

        override fun getItemCount(): Int {
            return fragments.size()
        }
    }

    companion object {
        private const val CAMERA_POS = 0
        private const val VIDEO_POS = 1
    }
}