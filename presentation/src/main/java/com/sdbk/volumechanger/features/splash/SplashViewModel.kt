package com.sdbk.volumechanger.features.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.sdbk.volumechanger.base.BaseViewModel
import com.sdbk.volumechanger.room.location.Location
import com.sdbk.volumechanger.room.location.LocationDao
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
    private val _goToMainEvent = SingleLiveEvent<Void>()
    val goToMainEvent: LiveData<Void> get() = _goToMainEvent

    val locationList = ArrayList<Location>()

    fun loadData() {
        viewModelScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                locationList.addAll(locationDao.getAll())
            }

            _goToMainEvent.call()
        }
    }
}