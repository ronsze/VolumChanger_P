package com.sdbk.volumechanger.features.main

import androidx.lifecycle.viewModelScope
import com.sdbk.domain.dao.location.LocationDao
import com.sdbk.domain.location.LocationEntity
import com.sdbk.volumechanger.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationDao: LocationDao
): BaseViewModel() {
    private val _locationList: ArrayList<LocationEntity> = ArrayList()
    val locationList: List<LocationEntity> get() = _locationList

    fun setData(locationList: ArrayList<LocationEntity>) {
        _locationList.addAll(locationList)
    }

    fun updateList() {
        viewModelScope.launch(Dispatchers.IO) {
            _locationList.clear()
            _locationList.addAll(locationDao.getAll())
        }
    }
}