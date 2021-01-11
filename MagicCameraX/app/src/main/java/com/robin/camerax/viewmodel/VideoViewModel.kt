package com.robin.camerax.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.net.Uri
import android.util.DisplayMetrics
import android.view.View
import androidx.camera.core.*
import androidx.camera.core.VideoCapture.OnVideoSavedCallback

import com.robin.camerax.R
import com.robin.camerax.STR_MP4
import com.robin.camerax.SingleEventLiveData
import com.robin.camerax.createFile
import com.robin.camerax.databinding.FragmentVideoBinding
import com.robin.camerax.repo.CameraRepositoryImpl
import com.robin.libutil.LogUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class VideoViewModel(context: Context) : BaseCameraViewModel<FragmentVideoBinding>(context) {
    private var videoCapture: VideoCapture? = null
    private var isRecording = false
    var videoAnimateLive: SingleEventLiveData<Boolean> = SingleEventLiveData()
    private val onVideoSavedCallback = OnVideoSavedCallbackImpl()

    private inner class OnVideoSavedCallbackImpl : OnVideoSavedCallback {
        private var videoFile: WeakReference<File>? = null
        fun setVideoFile(file: File) {
            videoFile = WeakReference(file)
        }

        override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
            LogUtil.e("onVideoSaved Thread:" + Thread.currentThread().name)
            var savedUri = outputFileResults.savedUri ?: Uri.fromFile(videoFile!!.get())
            LogUtil.d("Photo capture succeeded:$savedUri")
            setGalleryThumbnail(savedUri)
            val savedUriTmp = savedUri
            disposes.add(Single.create<Uri> {
                CameraRepositoryImpl().onImageSaved(savedUriTmp)
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe())
        }

        override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
            LogUtil.e("Photo capture failed:", cause)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCamerCaptureClick(view: View) {
        if (videoCapture == null) {
            LogUtil.e("videoCapture is null, maybe not init")
            return
        }
        if (outputDirectory == null) {
            LogUtil.e("outputDirectory is null, maybe not ready")
            return
        }
        if (cameraExecutor == null || cameraExecutor!!.isShutdown) {
            LogUtil.e("cameraExecutor is not ready")
            return
        }
        if (isRecording) {
            videoCapture!!.stopRecording()
            //停止摄像效果
            videoAnimateLive.setValue(false)
        } else {
            tryStartCountDown {
                val photoFile: File = outputDirectory!!.createFile(FILENAME, STR_MP4.toLowerCase())
                val outputOptions = VideoCapture.OutputFileOptions.Builder(photoFile).build()
                onVideoSavedCallback.setVideoFile(photoFile)
                videoCapture!!.startRecording(outputOptions, cameraExecutor!!, onVideoSavedCallback)
                //摄像效果
                videoAnimateLive.postValue(true)
            }
        }
        isRecording = !isRecording
    }

    override fun closeFlashAndSelect(flashMode: Int) {
        super.closeFlashAndSelect(flashMode)
        if (flashMode == ImageCapture.FLASH_MODE_ON) {
            camera?.cameraControl?.enableTorch(true)
        } else {
            camera?.cameraControl?.enableTorch(false)
        }
    }

    override fun onPause() {
        super.onPause()
        camera?.cameraControl?.enableTorch(false)
    }

    @SuppressLint("RestrictedApi")
    override fun bindCameraUseCases() {
        if (binding == null) {
            LogUtil.e("binding is null, do you forget to setIPreviewView")
            return
        }
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics()
        binding!!.viewFinder.display.getRealMetrics(metrics)
        LogUtil.d("Screen metrics: " + metrics.widthPixels + " x " + metrics.heightPixels)
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        LogUtil.d("Preview aspect ratio: $screenAspectRatio")
        val rotation: Int = binding!!.viewFinder.display.rotation

        // CameraProvider
        checkNotNull(cameraProvider) { mContext.getString(R.string.init_error) }

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview
        preview = Preview.Builder() // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio) // Set initial target rotation
                .setTargetRotation(rotation)
                .build()
        videoCapture = VideoCapture.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .setVideoFrameRate(24)
                .build()

        // Must unbind the use-cases before rebinding them
        cameraProvider!!.unbindAll()
        try {
            //未设置imageAnalyzer，原因为设置imageAnalyzer后，在RK3399板子上略卡
            camera = binding!!.lifecycleOwner?.let { cameraProvider!!.bindToLifecycle(it, cameraSelector, preview, videoCapture) }
            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(binding!!.viewFinder.surfaceProvider)
        } catch (e: Exception) {
            LogUtil.e("TAG", "Use case binding failed", e)
        }
    }

    fun linearZoom(progress: Int) {
        camera?.cameraControl?.setLinearZoom(progress / 100.0f)
    }

    fun preViewClick(x: Float, y: Float) {
        binding?.let {
            camera?.let {
                cameraExecutor?.let {
                    if (lensFacing == CameraSelector.LENS_FACING_BACK && !cameraExecutor!!.isShutdown) {
                        val factory: MeteringPointFactory = binding!!.viewFinder.meteringPointFactory
                        val point = factory.createPoint(x, y)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                            .build()
                        binding!!.funtionMiddle.focusView.startFocus(Point(x.toInt(), y.toInt()))
                        val future = camera!!.cameraControl.startFocusAndMetering(action)
                        future.addListener(Runnable {
                            try {
                                val result = future.get()
                                if (result.isFocusSuccessful) {
                                    binding!!.funtionMiddle.focusView.onFocusSuccess()
                                } else {
                                    binding!!.funtionMiddle.focusView.onFocusFailed()
                                }
                            } catch (e: Exception) {
                            }
                        }, cameraExecutor)
                    }
                }
            }
        }
    }

    fun doubleClick(x: Float, y: Float) {}
    fun longClick(x: Float, y: Float) {}

    companion object {
        private val TAG = VideoViewModel::class.java.simpleName
    }
}