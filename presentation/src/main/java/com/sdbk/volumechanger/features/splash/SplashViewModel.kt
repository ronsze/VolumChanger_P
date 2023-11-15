package com.sdbk.volumechanger.features.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.sdbk.domain.location.LocationEntity
import com.sdbk.domain.usecase.GetLocationUseCase
import com.sdbk.volumechanger.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kr.akaai.oxford_android.mvvm.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getLocationUseCase: GetLocationUseCase
): BaseViewModel() {
    private val _navigateToMainEvent = SingleLiveEvent<Void>()
    val navigateToMainEvent: LiveData<Void> get() = _navigateToMainEvent

    val locationList = ArrayList<LocationEntity>()

    fun loadData() {
        getLocationUseCase(viewModelScope, {
            _navigateToMainEvent.call()
        }, handleBaseError())
    }
}