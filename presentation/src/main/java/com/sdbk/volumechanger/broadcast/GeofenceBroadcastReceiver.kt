package com.sdbk.volumechanger.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import androidx.room.Room
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.sdbk.volumechanger.features.map.dialog.AddLocationDialog
import com.sdbk.data.database.LocationDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    companion object {
        val TAG: String = GeofenceBroadcastReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) {
            return
        }
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
        }

        val geofenceTransaction = geofencingEvent?.geofenceTransition

        if (geofenceTransaction == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransaction == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val transitionMsg = when (geofenceTransaction) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> "Enter"
                Geofence.GEOFENCE_TRANSITION_EXIT -> "Exit"
                else -> "-"
            }
            triggeringGeofences?.forEach {
                if (transitionMsg == "Enter") {
                    Log.e("Geofence", "Enter")
                    volumeChange(it.requestId, context)
                } else if (transitionMsg == "Exit") {
                    Log.e("Geofence", "Exit")
                }
            }
        } else {
            return
        }
    }

    private fun volumeChange(id: String, context: Context) {
        val locationDao = Room.databaseBuilder(context, com.sdbk.data.database.LocationDatabase::class.java, "location_database").build().locationDao()
        val audioManager: AudioManager = context.applicationContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        CoroutineScope(Dispatchers.Main).launch {
            val location = withContext(Dispatchers.IO) {
                locationDao.getAll().first { it.latLng == id }
            }

            when (location.bellVolume) {
                AddLocationDialog.VOLUME_MUTE -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                }
                AddLocationDialog.VOLUME_VIBRATION -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
                else -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_RING,
                        (audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) * (location.bellVolume / 100.0)).toInt(),
                        AudioManager.FLAG_PLAY_SOUND
                    )
                }
            }

            when (location.mediaVolume) {
                AddLocationDialog.VOLUME_MUTE -> {
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI
                    )
                }
                else -> {
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * (location.mediaVolume / 100.0)).toInt(),
                        AudioManager.FLAG_SHOW_UI
                    )
                }
            }
        }
    }
}