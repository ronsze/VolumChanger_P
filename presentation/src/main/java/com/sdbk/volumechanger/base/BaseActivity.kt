package com.sdbk.volumechanger.base

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sdbk.volumechanger.R
import java.util.*

abstract class BaseActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLanguage()
        initData()
        observeViewModel()
    }
    abstract val viewModel: BaseViewModel
    abstract fun initData()
    abstract fun observeViewModel()

    protected fun startActivitySlide(intent: Intent) {
        startActivity(intent)
        overridePendingTransition(R.anim.anim_slide_right, R.anim.anim_slide_left)
    }

    private fun setLanguage() {
        val locale = Locale.getDefault()

        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    protected fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}