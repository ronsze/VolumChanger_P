package com.sdbk.volumechanger

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.maps.MapsInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BaseApp: Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        MobileAds.initialize(this) {}
    }
}