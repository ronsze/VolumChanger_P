package com.sdbk.volumechanger.base

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import kr.akaai.oxford_android.mvvm.SingleLiveEvent


abstract class BaseViewModel: ViewModel() {
    private val _showToastEvent = SingleLiveEvent<String>()
    val showToastEvent: LiveData<String> get() = _showToastEvent

    protected fun showToast(message: String) = _showToastEvent.postValue(message)
    protected fun handleBaseError(event: (Throwable) -> Unit = {}): (Throwable) -> Unit = {
        event(it)
        Log.e("error", it.message.toString())
    }
}