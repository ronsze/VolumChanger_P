package com.sdbk.volumechanger.features.map

import android.app.Application
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sdbk.domain.location.LocationEntity
import com.sdbk.domain.usecase.DeleteLocationUseCase
import com.sdbk.domain.usecase.GetLocationUseCase
import com.sdbk.domain.usecase.InsertLocationUseCase
import com.sdbk.volumechanger.R
import com.sdbk.volumechanger.base.BaseViewModel
import com.sdbk.volumechanger.util.addressToCoordinate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kr.akaai.oxford_android.mvvm.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val application: Application,
    private val getLocationUseCase: GetLocationUseCase,
    private val insertLocationUseCase: InsertLocationUseCase,
    private val deleteLocationUseCase: DeleteLocationUseCase
): BaseViewModel() {
    private val _locationSearchEvent = SingleLiveEvent<LatLng>()
    val locationSearchEvent: LiveData<LatLng> get() = _locationSearchEvent

    private val _showDeleteDialogEvent = SingleLiveEvent<LocationEntity>()
    val showDeleteDialogEvent: LiveData<LocationEntity> get() = _showDeleteDialogEvent

    private val _addGeofenceEvent = SingleLiveEvent<LocationEntity>()
    val addGeofenceEvent: LiveData<LocationEntity> get() = _addGeofenceEvent

    private val _removeGeofenceEvent = SingleLiveEvent<Int>()
    val removeGeofenceEvent: LiveData<Int> get() = _removeGeofenceEvent

    private val _locationList: ArrayList<LocationEntity> = arrayListOf()
    val locationList: List<LocationEntity> get() = _locationList

    fun setData(locationList: ArrayList<LocationEntity>) {
        _locationList.addAll(locationList)
    }

    fun addLocation(location: LocationEntity) {
        if (!isOverlapping(location)) {
            insertLocationUseCase(location, viewModelScope, {
                _locationList.add(it)
                _addGeofenceEvent.postValue(it)
            }, handleBaseError())
        } else {
            showToast(application.getString(R.string.overlap_other_place))
        }
    }

    fun deleteLocation(location: LocationEntity) {
        deleteLocationUseCase(location, viewModelScope, {
            _locationList.remove(location)
            _removeGeofenceEvent.postValue(location.id.toInt())
        }, handleBaseError())
    }

    fun findDeleteLocation(id: Int) {
        getLocationUseCase(id, viewModelScope, {
            _showDeleteDialogEvent.postValue(it)
        }, handleBaseError())
    }

    fun searchAddress(inputAddress: String) {
        if (inputAddress.isEmpty()) {
            showToast(application.getString(R.string.empty_address))
            return
        }

        viewModelScope.launch {
            val coordinate = addressToCoordinate(application, inputAddress)
            coordinate?.run {
                _locationSearchEvent.postValue(LatLng(coordinate.latitude, coordinate.longitude))
            } ?: showToast(application.getString(R.string.address_no_matching))
        }
    }

    private fun isOverlapping(location: LocationEntity): Boolean {
        val newLocation = Location("new").apply {
            latitude = location.latitude
            longitude = location.longitude
        }

        locationList.forEach {
            val storedLocation = Location("stored").apply {
                latitude = it.latitude
                longitude = it.longitude
            }

            val distance = newLocation.distanceTo(storedLocation)
            val maxDistance = (location.range + it.range) / 2.0

            if (distance < maxDistance) return true
        }

        return false
    }
}