package com.sdbk.volumechanger.features.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.sdbk.volumechanger.BuildConfig
import com.sdbk.volumechanger.R
import com.sdbk.volumechanger.base.BaseActivity
import com.sdbk.volumechanger.databinding.ActivityMapBinding
import com.sdbk.volumechanger.features.geofence.GeofenceBroadcastReceiver
import com.sdbk.volumechanger.features.main.DeleteLocationDialog
import com.sdbk.volumechanger.room.location.Location
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

@SuppressLint("UnspecifiedImmutableFlag")
@AndroidEntryPoint
class MapActivity : BaseActivity(), OnMapReadyCallback {
    companion object {
        const val LAT_LNG = "lat_lng"
        private const val DEFAULT_LAT_LNG = "-33.852,151.211"
        private const val DEFAULT_ZOOM = 15.5f
    }

    private lateinit var binding: ActivityMapBinding
    override val viewModel: MapViewModel by viewModels()

    private lateinit var googleMap: GoogleMap
    private val context: Context = this

    private lateinit var adContainerView: FrameLayout
    private var initialLayoutComplete = false

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var userMarker: Marker
    private val markerList = ArrayList<Marker?>()
    private val circleList = ArrayList<Circle>()

    private lateinit var currentLocation: String
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val geoPending: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        if (Build.VERSION.SDK_INT >= 31) {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun initData() {
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        currentLocation = intent.getStringExtra("current_location") ?: DEFAULT_LAT_LNG
        setViewEvents()
        loadAdv()

        geofencingClient = LocationServices.getGeofencingClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun observeViewModel() {
        viewModel.initViewEvent.observe(this) {
            initMarkers()
            initCameraLocation()
        }

        viewModel.addLocationEvent.observe(this) { location ->
            createMarker(location)
            createCircle(location)
            addGeofence(location)
        }

        viewModel.deleteLocationEvent.observe(this) { location ->
            deleteMarker(location)
            deleteCircle(location)
            removeGeofences()
        }

        viewModel.showErrorToastEvent.observe(this) {
            showErrorToast(it)
        }

        viewModel.moveCameraEvent.observe(this) {
            changeCameraLocation(it.latitude, it.longitude)
            setSearchMarker(it)
        }

        viewModel.searchDoneEvent.observe(this) {
            binding.circleProgress.visibility = View.INVISIBLE
            binding.icSearch.visibility = View.VISIBLE
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setOnMapLongClickListener(onLongClickMap)
        googleMap.setOnMarkerClickListener(onClickMarker)

        initUserMarker()
        startLocationListener()
        viewModel.loadData()
        loadAdv()
    }

    private fun initUserMarker() {
        val location = viewModel.getLatLngFromString(currentLocation)
        val bitmapDraw = ContextCompat.getDrawable(this, R.drawable.ic_me_dot) as BitmapDrawable
        val icon = Bitmap.createScaledBitmap(bitmapDraw.bitmap, 40, 40, false)
        userMarker = googleMap.addMarker(
            MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(location)
                .title("user")
        )!!
    }

    private fun loadAdv() {
        adContainerView = findViewById(R.id.adView)
        val adView = AdManagerAdView(this)
        adContainerView.addView(adView)

        adContainerView.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialLayoutComplete) {
                initialLayoutComplete = true

                adView.adUnitId =
                    if (BuildConfig.DEBUG) getString(R.string.admob_unit_id_test)
                    else getString(R.string.admob_unit_id)
                if (Build.VERSION.SDK_INT >= 30) adView.setAdSizes(adSize, AdSize.BANNER)
                else adView.setAdSize(AdSize.BANNER)


                val adRequest = AdRequest.Builder().build()

                adView.loadAd(adRequest)
            }
        }
    }

    private fun initMarkers() {
        viewModel.locationList.let {
            it.forEach { location ->
                createMarker(location)
                createCircle(location)
            }
        }
    }

    private fun createMarker(location: Location) {
        val latLng = viewModel.getLatLngFromString(location.latLng)
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
        val latLng = viewModel.getLatLngFromString(location.latLng)
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
        val latLng =
            viewModel.getLatLngFromString(intent.getStringExtra(LAT_LNG) ?: currentLocation)
        changeCameraLocation(latLng.latitude, latLng.longitude)
    }

    private fun changeCameraLocation(lat: Double, lng: Double) {
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(lat, lng), DEFAULT_ZOOM
            )
        )
    }

    private fun showAddLocationDialog(latLng: LatLng) {
        AddLocationDialog(latLng) {
            val overlapList = viewModel.getOverlapList(it)
            if (overlapList.isEmpty()) {
                viewModel.addLocation(it)
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
        DeleteLocationDialog(location, viewModel::deleteLocation).show(supportFragmentManager, "")
    }

    private val onLongClickMap = OnMapLongClickListener {
        showAddLocationDialog(it)
    }

    private val onClickMarker = OnMarkerClickListener {
        if (it.tag != null) showDeleteDialog(it.tag as Location)
        false
    }

    private fun startLocationListener() {
        val listener = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                val location = res.locations.first()
                userMarker.position = LatLng(location.latitude, location.longitude)
                currentLocation = "${location.latitude},${location.longitude}"
            }
        }

        val request =
            LocationRequest.Builder(1000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()


        if (ActivityCompat.checkSelfPermission(this@MapActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this@MapActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) { return }
        fusedLocationClient.requestLocationUpdates(
            request, listener, Looper.getMainLooper()
        )
    }


    private fun setSearchMarker(latLng: LatLng) {
        lifecycleScope.launch(Dispatchers.Main) {
            val bitmapDraw = ContextCompat.getDrawable(this@MapActivity, R.drawable.ic_search) as BitmapDrawable
            val icon = Bitmap.createScaledBitmap(bitmapDraw.bitmap, 80, 80, false)
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(icon))
                    .position(LatLng(latLng.latitude, latLng.longitude))
            )

            withContext(Dispatchers.Default) { delay(3000) }
            marker?.remove()
        }
    }

    private fun setViewEvents() {
        binding.searchButtonLayout.setOnClickListener {
            binding.icSearch.visibility = View.INVISIBLE
            binding.circleProgress.visibility = View.VISIBLE

            viewModel.searchAddress(binding.addressEdit.text.toString())
            binding.addressEdit.setText("")
            hideKeyboard()
        }

        binding.moveMeLayout.setOnClickListener {
            moveCameraToMe()
        }

        binding.addressEdit.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.searchAddress(binding.addressEdit.text.toString())
                binding.addressEdit.setText("")
                hideKeyboard()
            }
            false
        }
    }

    private fun moveCameraToMe() {
        val location = viewModel.getLatLngFromString(currentLocation)
        changeCameraLocation(location.latitude, location.longitude)
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
        val geofence = getGeofence(
            location.latLng,
            viewModel.getLatLngFromString(location.latLng),
            location.range.toFloat()
        )
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            geofencingClient.addGeofences(getGeofencingRequest(geofence), geoPending).run {
                addOnSuccessListener {}
                addOnFailureListener {}
            }
        }
    }

    private fun addGeofences() {
        val geofenceList = ArrayList<Geofence>()

        viewModel.locationList.forEach {
            geofenceList.add(
                getGeofence(it.latLng, viewModel.getLatLngFromString(it.latLng), it.range.toFloat())
            )
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (geofenceList.isNotEmpty()) {
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
            addOnFailureListener { }
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

    private fun showErrorToast(errCode: Int) {
        val text = when (errCode) {
            MapViewModel.SEARCH_ERROR_NOT_MATCHING -> getString(R.string.address_no_matching)
            MapViewModel.SEARCH_ERROR_EMPTY -> getString(R.string.empty_address)
            else -> return
        }

        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    override fun finish() {
        val data = Intent()
        setResult(RESULT_OK, data)
        super.finish()
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
            val adWidth = (adWidthPixels / density).toInt()

            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    private fun hideKeyboard() {
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.addressEdit.windowToken, 0);
    }
}