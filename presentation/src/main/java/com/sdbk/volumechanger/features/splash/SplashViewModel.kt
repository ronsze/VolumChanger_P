package com.sdbk.volumechanger.features.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.sdbk.domain.dao.location.LocationDao
import com.sdbk.domain.location.LocationEntity
import com.sdbk.volumechanger.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.akaai.oxford_android.mvvm.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val locationDao: LocationDao
): BaseViewModel() {
    private val _navigateToMainEvent = SingleLiveEvent<Void>()
    val navigateToMainEvent: LiveData<Void> get() = _navigateToMainEvent

    val locationList = ArrayList<LocationEntity>()

    fun loadData() {
        viewModelScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) { locationList.addAll(locationDao.getAll()) }
            _navigateToMainEvent.call()
        }
    }
}