package com.robin.camerax.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.robin.libutil.LogUtil.d

open class BaseViewModel(protected var mContext: Context) : ViewModel() {
    open fun onResume() {
        d("vm onResume")
    }

    open fun onPause() {
        d("vm onPause")
    } /*@Override
    protected void onCleared() {
        mUserCase.dispose();
        mSingleUseCase.dispose();
        super.onCleared();
    }*/

}