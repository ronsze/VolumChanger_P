package com.sdbk.volumechanger.features.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.sdbk.volumechanger.R
import com.sdbk.volumechanger.base.BaseActivity
import com.sdbk.volumechanger.databinding.ActivityMainBinding
import com.sdbk.volumechanger.features.geofence.GeofenceBroadcastReceiver
import com.sdbk.volumechanger.features.map.MapActivity
import com.sdbk.volumechanger.room.location.Location
import com.sdbk.volumechanger.room.location.LocationDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@SuppressLint("UnspecifiedImmutableFlag")
@AndroidEntryPoint
class MainActivity : BaseActivity() {
    companion object {
        const val LOCATION_LIST = "location_list"
    }

    private lateinit var binding: ActivityMainBinding
    private val context: Context = this
    @Inject lateinit var locationDao: LocationDao
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var adContainerView: FrameLayout
    private var initialLayoutComplete = false


    private lateinit var locationListAdapter: LocationListAdapter
    private lateinit var deleteLocationDialog: DeleteLocationDialog

    private val adSize: AdSize
        @RequiresApi(Build.VERSION_CODES.R)
        get() {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds

            var adWidthPixels = adContainerView.width.toFloat()

            if (adWidthPixels == 0f) {
                adWidthPixels = bounds.width().toFloat()
            }

            val density = resources.displayMetrics.density
            val adWidth = (adWidthPixels/ density).toInt()

            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    private val mapResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            updateList()
        }
    }

    private val geoPending: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        if (Build.VERSION.SDK_INT >= 31) {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadAdv()

        geofencingClient = LocationServices.getGeofencingClient(this)
        initView()
        setClickEvent()
    }

    private fun loadAdv() {
        adContainerView = findViewById(R.id.adView)
        val adView = AdManagerAdView(this)
        adContainerView.addView(adView)

        adContainerView.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialLayoutComplete) {
                initialLayoutComplete = true

                adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"
                if (Build.VERSION.SDK_INT >= 30) adView.setAdSizes(adSize, AdSize.BANNER)
                else adView.setAdSize(AdSize.BANNER)


                val adRequest = AdRequest.Builder().build()

                adView.loadAd(adRequest)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun initView() {
        val locationList =
            if (Build.VERSION.SDK_INT < 33) intent.getSerializableExtra(LOCATION_LIST) as ArrayList<Location>
            else intent.getSerializableExtra(LOCATION_LIST, ArrayList::class.java)

        locationListAdapter = LocationListAdapter(
            locationList as? ArrayList<Location> ?: arrayListOf(),
            this::onClickItem,
            this::onLongClickItem
        )
        binding.locationRecyclerView.adapter = locationListAdapter
    }

    private fun setClickEvent() {
        binding.addButton.setOnClickListener {
            val anim = AnimationUtils.loadAnimation(this, R.anim.anim_opacity)
            binding.addButton.startAnimation(anim)
            goToMap()
        }
    }

    private fun updateList() {
        lifecycleScope.launch(Dispatchers.Main) {
            val list = withContext(Dispatchers.IO) {
                locationDao.getAll()
            }

            locationListAdapter.updateList(ArrayList(list))
            locationListAdapter.notifyDataSetChanged()
        }
    }

    private fun deleteItem(location: Location) {
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                locationDao.deleteLocationBy(location)
            }
            removeGeofences()
            updateList()
        }
    }

    private fun goToMap(location: Location? = null) {
        val intent = Intent(this, MapActivity::class.java)
        location?.run { intent.putExtra(MapActivity.LAT_LNG, location.latLng) }
        mapResultLauncher.launch(intent)
        overridePendingTransition(R.anim.anim_slide_right, R.anim.anim_slide_left)
    }

    private fun onClickItem(location: Location) {
        goToMap(location)
    }

    private fun onLongClickItem(location: Location) {
        deleteLocationDialog = DeleteLocationDialog(location) {
            deleteItem(location)
        }
        deleteLocationDialog.show(supportFragmentManager, "")
    }

    private fun addGeofence(location: Location) {
        val geofence = getGeofence(location.latLng, getLatLngFromString(location.latLng), location.range.toFloat())
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(getGeofencingRequest(geofence), geoPending).run {
                addOnSuccessListener {}
                addOnFailureListener {}
            }
        }
    }

    private fun addGeofences() {
        lifecycleScope.launch(Dispatchers.Main) {
            val geofenceList = ArrayList<Geofence>()
            val locationList = withContext(Dispatchers.IO) {
                locationDao.getAll()
            }

            locationList.forEach {
                geofenceList.add(
                    getGeofence(it.latLng, getLatLngFromString(it.latLng), it.range.toFloat())
                )
            }

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                geofencingClient.addGeofences(getGeofencingRequest(geofenceList), geoPending).run {
                    addOnSuccessListener {}
                    addOnFailureListener {}
                }
            }
        }
    }

    private fun getGeofence(reqId: String, latLng: LatLng, radius: Float): Geofence {
        return Geofence.Builder()
            .setRequestId(reqId)
            .setCircularRegion(latLng.latitude, latLng.longitude, radius / 2)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setLoiteringDelay(60000)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER
                        or Geofence.GEOFENCE_TRANSITION_EXIT
            ).build()
    }

    private fun removeGeofences() {
        geofencingClient.removeGeofences(geoPending).run {
            addOnSuccessListener {
                addGeofences()
            }
            addOnFailureListener {  }
        }
    }

    private fun getGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geofence)
        }.build()
    }

    private fun getGeofencingRequest(list: ArrayList<Geofence>): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(list)
        }.build()
    }

    private fun getLatLngFromString(str: String): LatLng {
        val locationString = str.split(",")
        val lat = locationString[0].toDouble()
        val lng = locationString[1].toDouble()
        return LatLng(lat, lng)
    }
}