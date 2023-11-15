package com.sdbk.volumechanger.module

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.sdbk.domain.location.LocationEntity
import com.sdbk.volumechanger.broadcast.GeofenceBroadcastReceiver

class GeofenceModule(
    private val activity: Activity
) {
    private val geofencingClient = LocationServices.getGeofencingClient(activity)

    private val geofencePendingIntent by lazy {
        val intent = Intent(activity, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun getGeofence(location: LocationEntity): Geofence =
        Geofence.Builder()
            .setRequestId(location.id.toString())
            .setCircularRegion(
                location.latitude,
                location.longitude,
                location.range.toFloat()
            )
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

    fun getGeofencingRequest(geofence: Geofence) =
        GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
            addGeofence(geofence)
        }.build()

    fun getGeofencingRequest(geofence: List<Geofence>) =
        GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
            addGeofences(geofence)
        }.build()

    fun addGeofence(request: GeofencingRequest, pendingIntent: PendingIntent, successEvent: () -> Unit, failureEvent: () -> Unit = {}) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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