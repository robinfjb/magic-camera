package com.robin.camerax.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.camera.core.ImageCapture
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.robin.camerax.*
import com.robin.camerax.component.CameraXPreviewView
import com.robin.camerax.databinding.FragmentCameraBinding
import com.robin.camerax.viewmodel.CameraViewModel
import com.robin.libutil.LogUtil

class CameraFragment : BaseFragment<FragmentCameraBinding, CameraViewModel>() {
    private lateinit var broadcastManager: LocalBroadcastManager
    private val volumeDownReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            if (intent.getIntExtra(
                    KEY_EVENT_EXTRA,
                    KeyEvent.KEYCODE_UNKNOWN
                ) == KeyEvent.KEYCODE_VOLUME_DOWN
            ) {
                binding.cameraCaptureButton.simulateClick()
            }
        }
    }

    public override fun binding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentCameraBinding {
        return DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)
    }

    override fun onViewCreatedInner() {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        registerBroadcastReceiver()
        observeEvent()
        initUI()
    }

    override fun permissons(): Array<String>? {
        return PERMISSIONS_REQUIRED_IMAGE
    }

    override fun afterHasPermisson() {
        binding.viewFinder.post{
            viewModel.onViewCreated(binding)
        }
    }

    override fun onDestroyView() {
        viewModel.onDestroyView()
        unregisterBroadcastReceiver()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        binding.viewFinder.post{
            viewModel.onResume()
        }
        binding.seekBar.progress = 0
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
            if (uri.equals(Uri.EMPTY)) {
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
        viewModel.switchBtnStatsLive.observe(viewLifecycleOwner, Observer { enable ->
            LogUtil.d("switch status=$enable")
            binding.cameraSwitchButton.isEnabled = enable
        })

        viewModel.slashLive.observe(viewLifecycleOwner, Observer{
            binding.cameraUiContainer.postDelayed({
                binding.cameraUiContainer.foreground = ColorDrawable(Color.WHITE)
                binding.cameraUiContainer.postDelayed(
                    { binding.cameraUiContainer.foreground = null },
                    ANIMATION_FAST_MILLIS
                )
            }, ANIMATION_SLOW_MILLIS)
        })

        viewModel.toggleTimerLive.observe(viewLifecycleOwner, Observer {
            binding.funtionTop.layoutTimerOptions.visibility = View.VISIBLE
        })
        viewModel.toggleFlashLive.observe(viewLifecycleOwner, Observer {
            binding.funtionTop.layoutFlashOptions.setVisibility(View.VISIBLE)
        })
        viewModel.closeTimerAndSelectLive.observe(viewLifecycleOwner, Observer{ cameraTimerEnum ->
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
                ImageCapture.FLASH_MODE_AUTO -> R.drawable.ic_flash_auto
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
        binding.viewFinder.setCustomTouchListener(object : CameraXPreviewView.CustomTouchListener {
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
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                viewModel.linearZoom(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun registerBroadcastReceiver() {
        broadcastManager = LocalBroadcastManager.getInstance(requireContext())
        val filter = IntentFilter()
        filter.addAction(KEY_EVENT_ACTION)
        broadcastManager.registerReceiver(volumeDownReceiver, filter)
    }

    private fun unregisterBroadcastReceiver() {
        broadcastManager.unregisterReceiver(volumeDownReceiver)
    }

    companion object {
        private val TAG = CameraFragment::class.java.simpleName
        private const val ANIMATION_FAST_MILLIS = 50L
        private const val ANIMATION_SLOW_MILLIS = 100L
    }
}