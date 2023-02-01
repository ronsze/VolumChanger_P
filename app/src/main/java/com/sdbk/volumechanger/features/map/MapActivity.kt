package com.sdbk.volumechanger.features.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.sdbk.volumechanger.R
import com.sdbk.volumechanger.base.BaseActivity
import com.sdbk.volumechanger.databinding.ActivityMapBinding
import com.sdbk.volumechanger.features.geofence.GeofenceBroadcastReceiver
import com.sdbk.volumechanger.features.main.DeleteLocationDialog
import com.sdbk.volumechanger.room.location.Location
import com.sdbk.volumechanger.room.location.LocationDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

@SuppressLint("UnspecifiedImmutableFlag")
@AndroidEntryPoint
class MapActivity: BaseActivity(), OnMapReadyCallback {
    companion object {
        const val LAT_LNG = "lat_lng"
        private const val DEFAULT_LAT_LNG = "-33.852,151.211"
        private const val DEFAULT_ZOOM = 14.5f
    }

    private val context: Context = this
    private lateinit var binding: ActivityMapBinding
    @Inject lateinit var locationDao: LocationDao
    private lateinit var googleMap: GoogleMap
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    private val PERMISSIONS_REQUEST_CODE = 100

    private lateinit var adContainerView: FrameLayout
    private var initialLayoutComplete = false

    private lateinit var geofencingClient: GeofencingClient
    private val globalLocationList = ArrayList<Location>()
    private val markerList = ArrayList<Marker?>()
    private val circleList = ArrayList<Circle>()

    private val geoPending: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        if (Build.VERSION.SDK_INT >= 31) {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadAdv()

        geofencingClient = LocationServices.getGeofencingClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setOnMapLongClickListener(onLongClickMap)
        googleMap.setOnMarkerClickListener(onClickMarker)
        initMarkers()
        initCameraLocation()
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

    private fun initMarkers() {
        lifecycleScope.launch(Dispatchers.Main) {
            val locationList = withContext(Dispatchers.IO) {
                locationDao.getAll()
            }

            locationList.let {
                globalLocationList.clear()
                globalLocationList.addAll(it)

                it.forEach { location ->
                    createMarker(location)
                    createCircle(location)
                }
            }
        }
    }

    private fun createMarker(location: Location) {
        val latLng = getLatLngFromString(location.latLng)
        val lat = latLng.latitude
        val lng = latLng.longitude

        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(lat, lng))
                .title(location.name)
        )
        marker?.tag = location
        markerList.add(marker)
    }

    private fun createCircle(location: Location) {
        val latLng = getLatLngFromString(location.latLng)
        val lat = latLng.latitude
        val lng = latLng.longitude

        val circle = googleMap.addCircle(
            CircleOptions()
                .clickable(false)
                .center(LatLng(lat, lng))
                .radius(location.range.toDouble() / 2)
                .strokeColor(Color.BLACK)
                .strokeWidth(2.0f)
                .fillColor(getRandomColor())
        )
        circle.tag = location
        circleList.add(circle)
    }

    private fun addLocation(location: Location) {
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                locationDao.insertLocation(location)
            }
            globalLocationList.add(location)
            createMarker(location)
            createCircle(location)
            addGeofence(location)
        }
    }

    private fun deleteLocation(location: Location) {
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                locationDao.deleteLocationBy(location)
            }
            globalLocationList.remove(globalLocationList.first { it.latLng == location.latLng })
            deleteMarker(location)
            deleteCircle(location)
            removeGeofences()
        }
    }

    private fun deleteMarker(tag: Location) {
        try {
            markerList.first { it?.tag == tag }?.remove()
        } catch (e: java.util.NoSuchElementException) {
            e.printStackTrace()
        }
    }

    private fun deleteCircle(tag: Location) {
        try {
            circleList.first { it.tag == tag }.remove()
        } catch (e: java.util.NoSuchElementException) {
            e.printStackTrace()
        }
    }

    private fun initCameraLocation() {
        val latLng = getLatLngFromString(intent.getStringExtra(LAT_LNG) ?: getCurrentLocation())
        changeCameraLocation(latLng.latitude, latLng.longitude)
    }

    private fun getCurrentLocation(): String {
        val userLocation: android.location.Location? = getCurrentLatLng()
        return if (userLocation != null) {
            val lat = userLocation.latitude
            val lng = userLocation.longitude
            "$lat,$lng"
        } else {
            DEFAULT_LAT_LNG
        }
    }

    private fun getCurrentLatLng(): android.location.Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        var currentLatLng: android.location.Location? = null
        var hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)
        var hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION)

        currentLatLng = if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
            hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            val locationProvider = LocationManager.GPS_PROVIDER
            locationManager?.getLastKnownLocation(locationProvider)
        } else{
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])){
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
            }else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
            }
            getCurrentLatLng()
        }
        return currentLatLng
    }

    private fun changeCameraLocation(lat: Double, lng: Double) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
            LatLng(lat, lng), DEFAULT_ZOOM
        ))
    }

    private fun showAddLocationDialog(latLng: LatLng) {
        AddLocationDialog(latLng) {
            val overlapList = getOverlapList(it)
            if (overlapList.isEmpty()) {
                addLocation(it)
            } else {
                var list = ""
                overlapList.forEachIndexed { index, location ->
                    list += if (index == 0) location.name
                    else ", ${location.name}"
                }
                showToast("${getString(R.string.overlap_other_place)}\n$list")
            }
        }.show(supportFragmentManager, "")
    }

    private fun showDeleteDialog(location: Location) {
        DeleteLocationDialog(location, this::deleteLocation).show(supportFragmentManager, "")
    }

    private val onLongClickMap = OnMapLongClickListener {
        showAddLocationDialog(it)
    }

    private val onClickMarker = OnMarkerClickListener {
        showDeleteDialog(it.tag as Location)
        false
    }

    private fun getOverlapList(location: Location): ArrayList<Location> {
        val overlapList = ArrayList<Location>()

        val newLatLng = getLatLngFromString(location.latLng)
        val newLocation = android.location.Location("new").apply {
            latitude = newLatLng.latitude
            longitude = newLatLng.longitude
        }

        globalLocationList.forEach {
            val storedLatLng = getLatLngFromString(it.latLng)
            val storedLocation = android.location.Location("stored").apply {
                latitude = storedLatLng.latitude
                longitude = storedLatLng.longitude
            }

            val distance = newLocation.distanceTo(storedLocation)
            val maxDistance = (location.range + it.range) / 2.0

            if (distance < maxDistance) {
                overlapList.add(it)
            }
        }

        return overlapList
    }

    private fun getLatLngFromString(str: String): LatLng {
        val locationString = str.split(",")
        val lat = locationString[0].toDouble()
        val lng = locationString[1].toDouble()
        return LatLng(lat, lng)
    }

    private fun getRandomColor(): Int {
        val colorId = when (Random.nextInt(8)) {
            0 -> R.color.translucent_black
            1 -> R.color.translucent_blue
            2 -> R.color.translucent_white
            3 -> R.color.translucent_purple
            4 -> R.color.translucent_yellow
            5 -> R.color.translucent_green
            6 -> R.color.translucent_sky_blue
            else -> R.color.translucent_red
        }
        return ContextCompat.getColor(this, colorId)
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

    override fun finish() {
        val data = Intent()
        setResult(RESULT_OK, data)
        super.finish()
    }
}