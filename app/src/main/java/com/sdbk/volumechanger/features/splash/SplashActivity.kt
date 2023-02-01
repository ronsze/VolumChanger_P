package com.sdbk.volumechanger.features.splash

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.sdbk.volumechanger.base.BaseActivity
import com.sdbk.volumechanger.databinding.ActivitySplashBinding
import com.sdbk.volumechanger.features.main.MainActivity
import com.sdbk.volumechanger.room.location.Location
import com.sdbk.volumechanger.room.location.LocationDao
import com.sdbk.volumechanger.room.location.LocationDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    private lateinit var binding: ActivitySplashBinding
    @Inject
    lateinit var locationDao: LocationDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestFineLocationPermission()
    }

    private fun requestFineLocationPermission() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        ActivityCompat.requestPermissions(this, permissions, 100)
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= 29) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 101)
        } else {
            loadData()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            100 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestBackgroundLocationPermission()
                } else {
                    exitProcess(0)
                }
            }
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadData()
                } else {
                    exitProcess(0)
                }
            }
            102 -> {

            }
        }
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.Main) {
            val locationList = withContext(Dispatchers.IO) {
                delay(2000)
                locationDao.getAll()
            }

            goToMain(ArrayList(locationList))
        }
    }

    private fun goToMain(locationList: ArrayList<Location>) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.LOCATION_LIST, locationList)
        startActivitySlide(intent)
        finish()
    }
}