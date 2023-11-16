package com.sdbk.volumechanger.features.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.sdbk.domain.location.LocationEntity
import com.sdbk.domain.usecase.DeleteLocationUseCase
import com.sdbk.domain.usecase.GetLocationUseCase
import com.sdbk.volumechanger.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kr.akaai.oxford_android.mvvm.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getLocationUseCase: GetLocationUseCase,
    private val deleteLocationUseCase: DeleteLocationUseCase
): BaseViewModel() {
    private val _locationList: ArrayList<LocationEntity> = ArrayList()
    val locationList: List<LocationEntity> get() = _locationList

    private val _removeGeofenceEvent = SingleLiveEvent<Int>()
    val removeGeofenceEvent: LiveData<Int> get() = _removeGeofenceEvent

    fun setData(locationList: ArrayList<LocationEntity>) {
        Log.e("qweqwe", "${locationList}")
        _locationList.addAll(locationList)
    }

    fun updateList() {
        getLocationUseCase(viewModelScope, {
            _locationList.clear()
            _locationList.addAll(it)
        }, handleBaseError())
    }

    fun deleteLocation(location: LocationEntity) {
        deleteLocationUseCase(location, viewModelScope, {
            _locationList.remove(location)
            _removeGeofenceEvent.postValue(location.id.toInt())
        }, handleBaseError())
    }
}