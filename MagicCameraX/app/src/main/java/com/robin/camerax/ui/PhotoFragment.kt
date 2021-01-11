package com.robin.camerax.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.robin.camerax.MP4_VIDEO
import com.robin.camerax.PERMISSIONS_REQUIRED_PHOTO
import com.robin.camerax.R
import com.robin.camerax.component.PicturesAdapter
import com.robin.camerax.databinding.FragmentPhotoBinding
import com.robin.camerax.share
import com.robin.camerax.viewmodel.PhotoViewModel
import java.io.File

class PhotoFragment : BaseFragment<FragmentPhotoBinding, PhotoViewModel>() {
    private lateinit var fragmentArgs: PhotoFragmentArgs
    private lateinit var picturesAdapter: PicturesAdapter
    private var currentPage = 0
    public override fun binding(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): FragmentPhotoBinding {
        return DataBindingUtil.inflate(inflater, R.layout.fragment_photo, container, false)
    }

    override fun onViewCreatedInner() {
        binding.viewModel = viewModel
        fragmentArgs = PhotoFragmentArgs.fromBundle(arguments!!)
        picturesAdapter = PicturesAdapter{ isVideo, uri ->
            if (!isVideo) {
                if (binding.groupPreviewActions.visibility == View.VISIBLE) {
                    binding.groupPreviewActions.visibility = View.GONE;
                } else {
                    binding.groupPreviewActions.visibility = View.VISIBLE;
                }
            } else {
                val playIntent = Intent(Intent.ACTION_VIEW, uri)
                playIntent.setDataAndType(uri, MP4_VIDEO);
                startActivity(playIntent);
            }
        }
        binding.pagerPhotos.adapter = picturesAdapter
        binding.pagerPhotos.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
            }
        })
        observeEvent()
    }

    override fun permissons(): Array<String>? {
        return PERMISSIONS_REQUIRED_PHOTO
    }

    override fun afterHasPermisson() {
        viewModel.onViewCreated(fragmentArgs)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun observeEvent() {
        viewModel.onBackLive.observe(viewLifecycleOwner, Observer {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).popBackStack()
        })

        viewModel.onShareLive.observe(viewLifecycleOwner, Observer{
            picturesAdapter.shareImage(currentPage) { file: File? ->
                file?.apply {
                    share(file, getString(R.string.share_default))
                }
            }
        })
        viewModel.onDeleteLive.observe(viewLifecycleOwner, Observer {
            picturesAdapter.deleteImage(currentPage) { file: File ->
                viewModel.resetScan(file) }
        })
        viewModel.dataLive.observe(viewLifecycleOwner,Observer { data ->
            data?.apply {
                picturesAdapter.setFiles(data)
            }
        })
    }

    companion object {
        private val TAG = PhotoFragment::class.java.simpleName
    }
}