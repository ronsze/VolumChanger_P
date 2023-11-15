package com.sdbk.volumechanger.features.map

import android.app.Application
import android.location.Address
import android.location.Geocoder
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.sdbk.volumechanger.base.BaseViewModel
import com.sdbk.domain.location.Location
import com.sdbk.domain.dao.location.LocationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.akaai.oxford_android.mvvm.SingleLiveEvent
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val application: Application,
    private val locationDao: com.sdbk.domain.dao.location.LocationDao
): BaseViewModel() {
    companion object {
        const val SEARCH_ERROR_NOT_MATCHING = 0
        const val SEARCH_ERROR_EMPTY = 1
    }

    var locationList = ArrayList<com.sdbk.domain.location.Location>()

    private val _initViewEvent = SingleLiveEvent<Void>()
    val initViewEvent: LiveData<Void> get() = _initViewEvent

    private val _addLocationEvent = SingleLiveEvent<com.sdbk.domain.location.Location>()
    val addLocationEvent: LiveData<com.sdbk.domain.location.Location> get() = _addLocationEvent

    private val _deleteLocationEvent = SingleLiveEvent<com.sdbk.domain.location.Location>()
    val deleteLocationEvent: LiveData<com.sdbk.domain.location.Location> get() = _deleteLocationEvent

    private val _showErrorToastEvent = SingleLiveEvent<Int>()
    val showErrorToastEvent: LiveData<Int> get() = _showErrorToastEvent

    private val _moveCameraEvent = SingleLiveEvent<LatLng>()
    val moveCameraEvent: LiveData<LatLng> get() = _moveCameraEvent

    private val _searchDoneEvent = SingleLiveEvent<LatLng>()
    val searchDoneEvent: LiveData<LatLng> get() = _searchDoneEvent

    fun loadData() {
        viewModelScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                locationList.clear()
                locationList.addAll(locationDao.getAll())
            }

            _initViewEvent.call()
        }
    }

    fun addLocation(location: com.sdbk.domain.location.Location) {
        viewModelScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                locationList.add(location)
                locationDao.insertLocation(location)
            }

            _addLocationEvent.postValue(location)
        }
    }

    fun deleteLocation(location: com.sdbk.domain.location.Location) {
        viewModelScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                locationList.remove(location)
                locationDao.deleteLocationBy(location)
            }

            _deleteLocationEvent.postValue(location)
        }
    }

    fun getOverlapList(location: com.sdbk.domain.location.Location): ArrayList<com.sdbk.domain.location.Location> {
        val overlapList = ArrayList<com.sdbk.domain.location.Location>()

        val newLatLng = getLatLngFromString(location.latLng)
        val newLocation = android.location.Location("new").apply {
            latitude = newLatLng.latitude
            longitude = newLatLng.longitude
        }

        locationList.forEach {
            val storedLatLng = getLatLngFromString(it.latLng)
            val storedLocation = android.location.Location("stored").apply {
                latitude = storedLatLng.latitude
                longitude = storedLatLng.longitude
            }

            val distance = newLocation.distanceTo(storedLocation)
            val maxDistance = (location.range + it.range) / 2.0

            if (distance < maxDistance) {
                overlapList.add(it)
            }
        }

        return overlapList
    }

    fun searchAddress(strAddress: String) {
        if (strAddress.isEmpty()) {
            _showErrorToastEvent.postValue(SEARCH_ERROR_EMPTY)
            _searchDoneEvent.call()
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            var location: Address? = null
            withContext(Dispatchers.IO) {
                val coder = Geocoder(application, Locale.getDefault())
                val address = strAddress.split(" ").joinToString("", "", "")

                try {
                    location = coder.getFromLocationName(address, 1)?.get(0) ?: return@withContext
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }

            if (location != null) {
                _moveCameraEvent.postValue(LatLng(location!!.latitude, location!!.longitude))
            } else {
                _showErrorToastEvent.postValue(SEARCH_ERROR_NOT_MATCHING)
            }
            _searchDoneEvent.call()
        }
    }

    fun getLatLngFromString(str: String): LatLng {
        val locationString = str.split(",")
        val lat = locationString[0].toDouble()
        val lng = locationString[1].toDouble()
        return LatLng(lat, lng)
    }
}