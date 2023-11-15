package com.sdbk.volumechanger.module

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.sdbk.domain.Constants.DATA
import com.sdbk.domain.Constants.LOCATION
import com.sdbk.domain.location.LocationEntity
import com.sdbk.volumechanger.broadcast.GeofenceBroadcastReceiver
import java.io.Serializable

class GeofenceModule(
    private val activity: Activity
) {
    private val geofencingClient = LocationServices.getGeofencingClient(activity)

    private fun geofencePendingIntent(data: Bundle): PendingIntent {
        val intent = Intent(activity, GeofenceBroadcastReceiver::class.java)
        intent.putExtra(DATA, data)
        return PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getGeofence(location: LocationEntity): Geofence =
        Geofence.Builder()
            .setRequestId(location.id.toString())
            .setCircularRegion(
                location.latitude,
                location.longitude,
                location.range.toFloat()
            )
            .setLoiteringDelay(60000)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

    private fun getGeofencingRequest(geofence: Geofence) =
        GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
            addGeofence(geofence)
        }.build()

    fun addGeofence(location: LocationEntity, successEvent: () -> Unit, failureEvent: () -> Unit = {}) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val geofence = getGeofence(location)
            val request = getGeofencingRequest(geofence)
            val pendingIntent = geofencePendingIntent(bundleOf(LOCATION to location))

            geofencingClient.addGeofences(request, pendingIntent).run {
                addOnSuccessListener { successEvent() }
                addOnFailureListener { failureEvent() }
            }
        }
    }

    fun removeGeofence(requestIds: List<String>, successEvent: () -> Unit, failureEvent: () -> Unit = {}) {
        geofencingClient.removeGeofences(requestIds).run {
            addOnSuccessListener { successEvent() }
            addOnFailureListener { failureEvent() }
        }
    }
}