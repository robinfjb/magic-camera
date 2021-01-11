package com.robin.camerax.ui

import android.animation.ObjectAnimator
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.camera.core.ImageCapture
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.robin.camerax.CameraTimerEnum
import com.robin.camerax.PERMISSIONS_REQUIRED_VIDEO
import com.robin.camerax.R
import com.robin.camerax.component.CameraXPreviewView.CustomTouchListener
import com.robin.camerax.databinding.FragmentVideoBinding
import com.robin.camerax.viewmodel.VideoViewModel
import com.robin.libutil.LogUtil.d

class VideoFragment : BaseFragment<FragmentVideoBinding, VideoViewModel>() {
    private var objectAnimator: ObjectAnimator? = null
    public override fun binding(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): FragmentVideoBinding {
        return DataBindingUtil.inflate(inflater, R.layout.fragment_video, container, false)
    }

    override fun onViewCreatedInner() {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        observeEvent()
        initUI()
    }

    override fun permissons(): Array<String>? {
        return PERMISSIONS_REQUIRED_VIDEO
    }

    override fun afterHasPermisson() {
        binding.viewFinder.post { viewModel.onViewCreated(binding) }
    }

    override fun onResume() {
        super.onResume()
        binding.viewFinder.post { viewModel.onResume() }
        //视频模式闪光灯只有开和关
        binding.funtionTop.buttonFlashAuto.visibility = View.GONE
        binding.seekBar.progress = 0
    }

    override fun onDestroyView() {
        viewModel.onDestroyView()
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun observeEvent() {
        viewModel.thumbnailLive.observe(viewLifecycleOwner, Observer { uri ->
            // Remove thumbnail padding
            val padding = resources.getDimension(R.dimen.spacing_small).toInt()
            binding.photoViewButton.setPadding(padding, padding, padding, padding)
            if (uri == Uri.EMPTY) {
                Glide.with(binding.photoViewButton)
                        .load(resources.getDrawable(R.drawable.ic_photo))
                        .apply(RequestOptions.circleCropTransform())
                        .into(binding.photoViewButton)
            } else {
                // Load thumbnail into circular button using Glide
                Glide.with(binding.photoViewButton)
                        .load(uri)
                        .apply(RequestOptions.circleCropTransform())
                        .into(binding.photoViewButton)
            }
        })
        viewModel.switchBtnStatsLive.observe(viewLifecycleOwner, Observer{ enable ->
            d("switch status=$enable")
            binding.videoSwitchButton.isEnabled = enable
        })
        viewModel.videoAnimateLive.observe(viewLifecycleOwner, Observer { start ->
            if (start) {
                if (objectAnimator == null) {
                    objectAnimator = ObjectAnimator.ofFloat(binding.videoCaptureButton, View.ALPHA, 1f, 0.5f)
                    objectAnimator!!.repeatMode = ObjectAnimator.REVERSE
                    objectAnimator!!.repeatCount = ObjectAnimator.INFINITE
                }
                objectAnimator!!.start()
            } else {
                objectAnimator?.cancel()
                binding!!.videoCaptureButton.alpha = 1f
            }
        })
        viewModel.toggleTimerLive.observe(viewLifecycleOwner, Observer{  binding.funtionTop.layoutTimerOptions.visibility = View.VISIBLE })
        viewModel.toggleFlashLive.observe(viewLifecycleOwner, Observer { binding.funtionTop.layoutFlashOptions.visibility = View.VISIBLE })
        viewModel.closeTimerAndSelectLive.observe(viewLifecycleOwner, Observer { cameraTimerEnum ->
            binding.funtionTop.layoutTimerOptions.visibility = View.GONE
            val drawable = when (cameraTimerEnum) {
                CameraTimerEnum.S3 -> R.drawable.ic_timer_3
                CameraTimerEnum.OFF -> R.drawable.ic_timer_off
                CameraTimerEnum.S10 -> R.drawable.ic_timer_10
                else -> R.drawable.ic_timer_off
            }
            binding.funtionTop.buttonTimer.setImageResource(drawable)
        })
        viewModel.closeFlashAndSelectLive.observe(viewLifecycleOwner, Observer { flashMode ->
            binding.funtionTop.layoutFlashOptions.visibility = View.GONE
            val drawable = when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> R.drawable.ic_flash_off
                ImageCapture.FLASH_MODE_ON -> R.drawable.ic_flash_on
                else -> R.drawable.ic_flash_off
            }
            binding.funtionTop.buttonFlash.setImageResource(drawable)
        })
        viewModel.countDownTxtLive.observe(viewLifecycleOwner, Observer{ count ->
            if (count >= 0) {
                binding.funtionMiddle.textCountDown.setText(java.lang.String.valueOf(count))
                binding.funtionMiddle.textCountDown.visibility = View.VISIBLE
            } else {
                binding.funtionMiddle.textCountDown.visibility = View.GONE
            }
        })
    }

    private fun initUI() {
        binding.viewFinder.setCustomTouchListener(object : CustomTouchListener {
            override fun zoom(delta: Float) {}
            override fun click(x: Float, y: Float) {
                viewModel.preViewClick(x, y)
            }

            override fun doubleClick(x: Float, y: Float) {
                viewModel.doubleClick(x, y)
            }

            override fun longClick(x: Float, y: Float) {
                viewModel.longClick(x, y)
            }
        })
        binding.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                viewModel.linearZoom(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }
}