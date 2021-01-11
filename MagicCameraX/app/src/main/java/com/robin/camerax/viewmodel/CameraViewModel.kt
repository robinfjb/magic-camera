package com.robin.camerax.viewmodel

import android.content.Context
import android.graphics.Point
import android.net.Uri
import android.util.DisplayMetrics
import android.view.View
import androidx.camera.core.*
import com.robin.camerax.R
import com.robin.camerax.STR_JPG
import com.robin.camerax.SingleEventLiveData
import com.robin.camerax.createFile
import com.robin.camerax.databinding.FragmentCameraBinding
import com.robin.camerax.repo.CameraRepositoryImpl
import com.robin.libutil.LogUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class CameraViewModel(context: Context) : BaseCameraViewModel<FragmentCameraBinding>(context) {
    var slashLive: SingleEventLiveData<Any> = SingleEventLiveData()
    private var imageCapture: ImageCapture? = null
    private val onImageSavedCallback = OnImageSavedCallbackImpl()

    private inner class OnImageSavedCallbackImpl : ImageCapture.OnImageSavedCallback {
        private var photoFile: WeakReference<File>? = null
        fun setPhotoFile(file: File) {
            photoFile = WeakReference(file)
        }

        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            LogUtil.e("onImageSaved Thread:${Thread.currentThread().name}")
            var savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile?.get())
            LogUtil.d("Photo capture succeeded:$savedUri")
            setGalleryThumbnail(savedUri)
            val savedUriTmp = savedUri
            disposes.add(Single.create<Uri> {
                CameraRepositoryImpl().onImageSaved(savedUriTmp)
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe())
        }

        override fun onError(exception: ImageCaptureException) {
            LogUtil.e("Photo capture failed:", exception)
        }
    }

    override fun onCamerCaptureClick(view: View) {
        if (imageCapture == null) {
            LogUtil.e("imageCapture is null, maybe not init")
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
        tryStartCountDown {
            // Create output file to hold the image
            val photoFile: File = outputDirectory!!.createFile(FILENAME, STR_JPG.toLowerCase())

            // Setup image capture metadata
            val metadata = ImageCapture.Metadata()
            metadata.isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                    .setMetadata(metadata)
                    .build()

            // Setup image capture listener which is triggered after photo has been taken
            onImageSavedCallback.setPhotoFile(photoFile)
            imageCapture!!.takePicture(outputOptions, cameraExecutor!!, onImageSavedCallback)

            // 闪烁效果
            slashLive.postValue(Any())

        }
    }

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

        // ImageCapture
        imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) //                .setTargetResolution(new Size(2560, 1440))
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(screenAspectRatio) // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .setFlashMode(flashMode)
                .build()

        // Must unbind the use-cases before rebinding them
        cameraProvider!!.unbindAll()
        try {
            //未设置imageAnalyzer，原因为设置imageAnalyzer后，在RK3399板子上略卡
            camera = binding!!.lifecycleOwner?.let { cameraProvider!!.bindToLifecycle(it, cameraSelector, preview, imageCapture) }
            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(binding!!.viewFinder.surfaceProvider)
        } catch (e: Exception) {
            LogUtil.e("Use case binding failed", e)
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
        private val TAG = CameraViewModel::class.java.simpleName
    }
}