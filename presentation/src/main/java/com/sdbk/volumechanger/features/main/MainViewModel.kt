package com.sdbk.volumechanger.features.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.Geofence
import com.google.android.gms.maps.model.LatLng
import com.sdbk.volumechanger.base.BaseViewModel
import com.sdbk.volumechanger.room.location.Location
import com.sdbk.volumechanger.room.location.LocationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.akaai.oxford_android.mvvm.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationDao: LocationDao
): BaseViewModel() {
    var locationList: ArrayList<Location> = ArrayList()

    private val _updateListEvent = SingleLiveEvent<Void>()
    val updateListEvent: LiveData<Void> get() = _updateListEvent

    fun updateList() {
        viewModelScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                locationList.clear()
                locationList.addAll(locationDao.getAll())
            }

            _updateListEvent.call()
        }
    }

    fun getGeofence(reqId: String, latLng: LatLng, radius: Float): Geofence {
        return Geofence.Builder()
            .setRequestId(reqId)
            .setCircularRegion(latLng.latitude, latLng.longitude, radius / 2)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setLoiteringDelay(60000)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER
                        or Geofence.GEOFENCE_TRANSITION_EXIT
            ).build()
    }

    fun getLatLngFromString(str: String): LatLng {
        val locationString = str.split(",")
        val lat = locationString[0].toDouble()
        val lng = locationString[1].toDouble()
        return LatLng(lat, lng)
    }
}