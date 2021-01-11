package com.robin.camerax.viewmodel

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.CountDownTimer
import android.view.View
import androidx.annotation.MainThread
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ViewDataBinding
import androidx.navigation.Navigation
import com.robin.camerax.CameraTimerEnum
import com.robin.camerax.R
import com.robin.camerax.SingleEventLiveData
import com.robin.camerax.getMediaOutputDirectory
import com.robin.camerax.repo.CameraRepositoryImpl
import com.robin.camerax.ui.CameraFragmentDirections
import com.robin.libutil.LogUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

abstract class BaseCameraViewModel<T : ViewDataBinding>(context: Context) : BaseViewModel(context) {
    protected var lensFacing = CameraSelector.LENS_FACING_BACK
    var thumbnailLive: SingleEventLiveData<Uri> = SingleEventLiveData()
    var switchBtnStatsLive: SingleEventLiveData<Boolean> = SingleEventLiveData()
    var toggleTimerLive: SingleEventLiveData<Any> = SingleEventLiveData()
    var toggleFlashLive: SingleEventLiveData<Any> = SingleEventLiveData()
    var closeTimerAndSelectLive: SingleEventLiveData<CameraTimerEnum> = SingleEventLiveData()
    var closeFlashAndSelectLive: SingleEventLiveData<Int> = SingleEventLiveData()
    var countDownTxtLive: SingleEventLiveData<Int> = SingleEventLiveData()
    protected var binding: T? = null
    protected var outputDirectory: File? = null
    protected var preview: Preview? = null
    protected var camera: Camera? = null
    protected var cameraProvider: ProcessCameraProvider? = null
    protected var cameraExecutor: ExecutorService? = null
    var hasGrid = ObservableBoolean()
    protected var selectedTimer: CameraTimerEnum = CameraTimerEnum.OFF
    protected var flashMode = ImageCapture.FLASH_MODE_OFF
    private var countDownTimer: CountDownTimer? = null
    protected val disposes: ArrayList<Disposable> = arrayListOf()

    @MainThread
    fun onCamerSwitchClick(view: View) {
        view.isEnabled = false
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        // Re-bind use cases to update selected camera
        bindCameraUseCases()
        updateCameraSwitchButton()
    }

    protected abstract fun bindCameraUseCases()

    @MainThread
    abstract fun onCamerCaptureClick(view: View)

    @MainThread
    fun onPhotoViewClick(view: View) {
        outputDirectory?.apply {
            if (outputDirectory!!.listFiles() != null && outputDirectory!!.length() > 0) {
                Navigation.findNavController((mContext as Activity), R.id.fragment_container).navigate(
                        CameraFragmentDirections.actionCameraToPhoto(outputDirectory!!.absolutePath))
            }
        }

    }

    @MainThread
    fun selectTimer() {
        toggleTimerLive.setValue(Any())
    }

    @MainThread
    fun toggleGrid() {
        //TODO 持久化存储
        hasGrid.set(!hasGrid.get())
    }

    @MainThread
    fun selectFlash() {
        toggleFlashLive.setValue(Any())
    }

    @MainThread
    fun closeTimerAndSelect(timerEnum: CameraTimerEnum) {
        selectedTimer = timerEnum
        closeTimerAndSelectLive.setValue(timerEnum)
    }

    @MainThread
    open fun closeFlashAndSelect(flashMode: Int) {
        this.flashMode = flashMode
        closeFlashAndSelectLive.setValue(flashMode)
    }

    fun onViewCreated(binding: T) {
        this.binding = binding
        if (cameraExecutor == null || cameraExecutor!!.isShutdown) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
    }

    fun onDestroyView() {
        cameraExecutor?.shutdown()
    }

    /**
     * view准备完毕后触发
     */
    override fun onResume() {
        super.onResume()
        outputDirectory = mContext.getMediaOutputDirectory(mContext.resources.getString(R.string.app_name))
        //更新缩略图

        outputDirectory?.let {
            disposes.add(CameraRepositoryImpl().getThumnailPhoto(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { result ->
                                setGalleryThumbnail(result)
                                LogUtil.d("get thumbnail data:$result")
                            },
                            {
                                setGalleryThumbnail(null)
                                LogUtil.e("get thumbnail data error:", it)
                            }
                    ))

        }
        //经测试，页面切换后需重新设置才能保证功能有效
        setUpCamera()
    }

    override fun onPause() {
        super.onPause()
        countDownTimer?.cancel()
    }

    protected fun setGalleryThumbnail(uri: Uri?) {
        if (uri != null) {
            thumbnailLive.postValue(uri)
        } else {
            thumbnailLive.postValue(Uri.EMPTY)
        }
    }

    private fun updateCameraSwitchButton() {
        switchBtnStatsLive.setValue(hasBackCamera() && hasFrontCamera())
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(mContext)
        cameraProviderFuture.addListener(Runnable {
            // CameraProvider
            try {
                cameraProvider = cameraProviderFuture.get()
                if (hasBackCamera()) {
                    lensFacing = CameraSelector.LENS_FACING_BACK
                    LogUtil.d("lensFacing: CameraSelector.LENS_FACING_BACK")
                } else if (hasFrontCamera()) {
                    lensFacing = CameraSelector.LENS_FACING_FRONT
                    LogUtil.d("lensFacing: CameraSelector.LENS_FACING_FRONT")
                } else {
                    throw IllegalStateException(mContext.getString(R.string.lens_error))
                }
                updateCameraSwitchButton()
                // Build and bind the camera use cases
                bindCameraUseCases()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(mContext))
    }

    /**
     * Returns true if the device has an available back camera. False otherwise
     */
    private fun hasBackCamera(): Boolean {
        try {
            return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
        } catch (e: CameraInfoUnavailableException) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Returns true if the device has an available front camera. False otherwise
     */
    private fun hasFrontCamera(): Boolean {
        try {
            return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
        } catch (e: CameraInfoUnavailableException) {
            e.printStackTrace()
        }
        return false
    }

    protected fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height).toDouble()
        return if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            AspectRatio.RATIO_4_3
        } else AspectRatio.RATIO_16_9
    }

    protected fun tryStartCountDown(callback: (() -> Unit)?) {
        val count = when (selectedTimer) {
            CameraTimerEnum.OFF -> -1
            CameraTimerEnum.S3 -> 3
            CameraTimerEnum.S10 -> 10
        }
        if (count >= 0) {
            countDownTimer = object : CountDownTimer((count * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val count = (millisUntilFinished / 1000.0).roundToInt()
                    LogUtil.d("CountDownTimer count=$count")
                    countDownTxtLive.setValue(count)
                }

                override fun onFinish() {
                    LogUtil.d("CountDownTimer onFinish")
                    countDownTxtLive.setValue(-1)
                    callback?.invoke()
                }
            }
            countDownTimer!!.start()
        } else {
            callback?.invoke()
        }
    }

    override fun onCleared() {
        disposes.forEach {
            it?.dispose()
        }
        super.onCleared()
    }

    companion object {
        const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val RATIO_4_3_VALUE = 4.0 / 3.0
        const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}