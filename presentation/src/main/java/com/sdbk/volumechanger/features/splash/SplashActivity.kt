package com.sdbk.volumechanger.features.splash

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.sdbk.domain.Constants.LOCATION_LIST
import com.sdbk.domain.location.LocationListWrapper
import com.sdbk.volumechanger.R
import com.sdbk.volumechanger.base.BaseActivity
import com.sdbk.volumechanger.databinding.ActivitySplashBinding
import com.sdbk.volumechanger.features.main.MainActivity
import com.sdbk.volumechanger.features.splash.dialog.RequestPermissionDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlin.system.exitProcess

@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding, SplashViewModel>() {
    companion object {
        private const val FINE_LOCATION_REQUEST_CODE = 100
        private const val BACKGROUND_LOCATION_REQUEST_CODE = 101
    }

    override val bindingInflater: (LayoutInflater) -> ActivitySplashBinding = ActivitySplashBinding::inflate
    override val viewModel: SplashViewModel by viewModels()

    private val requestPermissionDialog = RequestPermissionDialog()

    private val volumePermissionResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        requestVolumeChangePermission()
    }

    override fun initData() {
        checkPermissions()
    }

    override fun observeViewModel() {
        viewModel.navigateToMainEvent.observe(this) {
            navigateToMain()
        }
    }

    override fun setFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener(getString(R.string.click_ok), this) { _, _ ->
            requestFineLocationPermission()
        }
    }

    private fun checkPermissions() {
        if (getPermissionsGranted()) viewModel.loadData()
        else requestPermissionDialog.show(supportFragmentManager, requestPermissionDialog.tag)
    }

    private fun requestFineLocationPermission() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        ActivityCompat.requestPermissions(this, permissions, FINE_LOCATION_REQUEST_CODE)
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= 29) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                BACKGROUND_LOCATION_REQUEST_CODE
            )
        } else {
            requestVolumeChangePermission()
        }
    }

    private fun requestVolumeChangePermission() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            volumePermissionResultLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        } else {
            viewModel.loadData()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(LOCATION_LIST, LocationListWrapper(viewModel.locationList))
        startActivitySlide(intent)
        finish()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            FINE_LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestBackgroundLocationPermission()
                } else {
                    exitProcess(0)
                }
            }
            BACKGROUND_LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestVolumeChangePermission()
                } else {
                    exitProcess(0)
                }
            }
        }
    }
}