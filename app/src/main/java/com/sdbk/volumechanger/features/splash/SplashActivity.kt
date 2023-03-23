package com.sdbk.volumechanger.features.splash

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.sdbk.volumechanger.base.BaseActivity
import com.sdbk.volumechanger.data.LocationData
import com.sdbk.volumechanger.databinding.ActivitySplashBinding
import com.sdbk.volumechanger.features.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlin.system.exitProcess

@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    private lateinit var binding: ActivitySplashBinding
    override val viewModel: SplashViewModel by viewModels()

    private val requestPermissionDialog = RequestPermissionDialog({exitProcess(0)}, this::requestFineLocationPermission)
    override fun initData() {
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
    }

    override fun observeViewModel() {
        viewModel.goToMainEvent.observe(this) {
            goToMain()
        }
    }

    private fun checkPermissions() {
        if (getPermissionsGranted()) {
            viewModel.loadData()
        } else {
            requestPermissionDialog.show(supportFragmentManager, requestPermissionDialog.tag)
        }
    }

    private fun getPermissionsGranted(): Boolean {
        var permissionsGranted =
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && (this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).isNotificationPolicyAccessGranted
        if (Build.VERSION.SDK_INT >= 29) {
            permissionsGranted = permissionsGranted
                    && checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        return permissionsGranted
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
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                101
            )
        } else {
            viewModel.loadData()
        }
    }

    private val volumePermissionResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        requestVolumeChangePermission()
    }

    private fun requestVolumeChangePermission() {
        val notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            volumePermissionResultLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        } else {
            viewModel.loadData()
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
                    requestVolumeChangePermission()
                } else {
                    exitProcess(0)
                }
            }
        }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.LOCATION_LIST, LocationData(viewModel.locationList))
        startActivitySlide(intent)
        finish()
    }
}