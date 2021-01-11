package com.robin.camerax.viewmodel

import android.content.Context
import android.net.Uri
import android.view.View
import com.robin.camerax.SingleEventLiveData
import com.robin.camerax.repo.PhotoRepository
import com.robin.camerax.repo.PhotoRepositoryImpl
import com.robin.camerax.ui.PhotoFragmentArgs
import com.robin.libutil.LogUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.util.*

class PhotoViewModel(context: Context?) : BaseViewModel(context!!) {
    var onBackLive: SingleEventLiveData<Any> = SingleEventLiveData()
    var onShareLive: SingleEventLiveData<Any> = SingleEventLiveData()
    var onDeleteLive: SingleEventLiveData<Any> = SingleEventLiveData()
    var dataLive: SingleEventLiveData<ArrayList<File>> = SingleEventLiveData()
    private var outputDirectory: File? = null
    private val mRepo: PhotoRepository = PhotoRepositoryImpl()
    private val disposes: ArrayList<Disposable> = arrayListOf()

    fun onViewCreated(args: PhotoFragmentArgs) {
        outputDirectory = File(args.rootDirectory)

        outputDirectory?.let {
            disposes.add(mRepo.getListFiles(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { result ->
                                val arrayList = arrayListOf<File>()
                                arrayList.addAll(result)
                                dataLive.postValue(arrayList)
                            },
                            {
                                dataLive.postValue(arrayListOf())
                                LogUtil.e("get directory file error:", it)
                            }
                    ))

        }
    }

    fun onBackPress(view: View) {
        onBackLive.value = Any()
    }

    fun onSharePress(view: View) {
        onShareLive.value = Any()
    }

    fun onDeletePress(view: View) {
        onDeleteLive.value = Any()
    }

    /**
     * 删除照片后需要更新广播
     */
    fun resetScan(file: File) {
        disposes.add(Single.create<Uri> {
            mRepo.onPhotoDeleted(file)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe())
    }

    override fun onCleared() {
        disposes.forEach {
            it?.dispose()
        }
        super.onCleared()
    }
}