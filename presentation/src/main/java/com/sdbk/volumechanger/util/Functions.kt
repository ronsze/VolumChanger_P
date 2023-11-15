package com.sdbk.volumechanger.util

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.sdbk.domain.exceptions.NoExtraFoundMatchingKeyException
import com.sdbk.volumechanger.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.util.Locale
import kotlin.random.Random

inline fun <reified T: Serializable> Intent.getSerializable(key: String): T =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(key, T::class.java) ?: throw NoExtraFoundMatchingKeyException()
    } else {
        getSerializableExtra(key) as T
    }

suspend fun coordinateToAddress(context: Context, latitude: Double, longitude: Double) =
    withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())

        val addressList = geocoder.getFromLocation(latitude, longitude, 1)
        addressList?.first()?.getAddressLine(0)
    } ?: ""

suspend fun addressToCoordinate(context: Context, inputAddress: String): LatLng? {
    return withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        val address = inputAddress.split(" ").joinToString("", "", "")

        val coordinate = geocoder.getFromLocationName(address, 3)?.get(0)
        coordinate?.run {
            LatLng(coordinate.latitude, coordinate.longitude)
        }
    }
}

fun getRandomColor(context: Context): Int {
    val colors = arrayOf(
        R.color.translucent_black, R.color.translucent_blue, R.color.translucent_white, R.color.translucent_purple,
        R.color.translucent_yellow, R.color.translucent_green, R.color.translucent_sky_blue, R.color.translucent_red
    )
    return ContextCompat.getColor(context, colors[Random.nextInt(8)])
}