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
import com.sdbk.domain.Constants.LOCATION
import com.sdbk.domain.location.LocationEntity
import com.sdbk.volumechanger.util.getSerializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    companion object {
        val TAG: String = GeofenceBroadcastReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
        }

        if (geofencingEvent?.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val location = intent.getSerializable<LocationEntity>(LOCATION)
            location?.run { volumeChange(context, this) }
        }
    }

    private fun volumeChange(context: Context, location: LocationEntity) {

        val audioManager: AudioManager = context.applicationContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        CoroutineScope(Dispatchers.Main).launch {
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