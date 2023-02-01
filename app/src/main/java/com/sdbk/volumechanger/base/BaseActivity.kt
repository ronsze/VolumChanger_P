package com.sdbk.volumechanger.base

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sdbk.volumechanger.R
import java.util.Locale

abstract class BaseActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setLanguage()
    }

    protected fun startActivitySlide(intent: Intent) {
        startActivity(intent)
        overridePendingTransition(R.anim.anim_slide_right, R.anim.anim_slide_left)
    }

    private fun setLanguage() {
        val locale = if (Build.VERSION.SDK_INT >= 21) Locale.getDefault()
        else Locale.US

        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    protected fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}