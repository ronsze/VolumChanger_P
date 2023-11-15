package com.sdbk.volumechanger.util

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.maps.model.LatLng
import com.sdbk.data.exceptions.NoExtraFoundMatchingKeyException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.util.Locale

inline fun <reified T: Serializable> Intent.getSerializable(key: String): T =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(key, T::class.java) ?: throw NoExtraFoundMatchingKeyException()
    } else {
        getSerializableExtra(key) as T
    }

suspend fun latLngToAddress(context: Context, latitude: Double, longitude: Double) =
    withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())

        val addressList = geocoder.getFromLocation(latitude, longitude, 1)
        addressList?.first()?.getAddressLine(0)
    } ?: ""