package com.sdbk.volumechanger.features.map

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Looper
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.sdbk.domain.Constants.LOCATION_LIST
import com.sdbk.domain.location.LocationEntity
import com.sdbk.domain.location.LocationListWrapper
import com.sdbk.volumechanger.R
import com.sdbk.volumechanger.base.BaseActivity
import com.sdbk.volumechanger.databinding.ActivityMapBinding
import com.sdbk.volumechanger.features.map.dialog.AddLocationDialog
import com.sdbk.volumechanger.module.GeofenceModule
import com.sdbk.volumechanger.util.getRandomColor
import com.sdbk.volumechanger.util.getSerializable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MapActivity : BaseActivity<ActivityMapBinding, MapViewModel>(), OnMapReadyCallback {
    companion object {
        private const val DEFAULT_ZOOM = 15.5f
    }
    override val bindingInflater: (LayoutInflater) -> ActivityMapBinding = ActivityMapBinding::inflate
    override val viewModel: MapViewModel by viewModels()

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private val geofenceModule by lazy {
        GeofenceModule(this)
    }
    private lateinit var googleMap: GoogleMap
    private lateinit var userMarker: Marker

    private var currentLocation = LatLng(0.0, 0.0)

    private val markerList = ArrayList<Marker?>()
    private val circleList = ArrayList<Circle>()

    override fun initData() {
        viewModel.setData(intent.getSerializable<LocationListWrapper>(LOCATION_LIST).locationList)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun observeViewModel() {
        viewModel.locationSearchEvent.observe(this) {
            setSearchMarker(it)
            moveCamera(it)
        }

        viewModel.showDeleteDialogEvent.observe(this) {
            showDeleteDialog(it)
        }

        viewModel.addGeofenceEvent.observe(this) {
            geofenceModule.addGeofence(it, {
                createMarker(it)
                createCircle(it)
                showToast(getString(R.string.add_location))
            })
        }

        viewModel.removeGeofenceEvent.observe(this) {
            geofenceModule.removeGeofence(listOf(it.toString()), {
                deleteMarker(it)
                deleteCircle(it)
                showToast(getString(R.string.location_removed))
            })
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setOnMapLongClickListener(onLongClickMap)
        googleMap.setOnMarkerClickListener(onClickMarker)

        initUserMarker()
        initLocationMarkers()
        setFusedLocationListener()
    }

    private val onLongClickMap = OnMapLongClickListener {
        showAddLocationDialog(it)
    }

    private fun initUserMarker() {
        val bitmapDraw = ContextCompat.getDrawable(this, R.drawable.ic_me_dot) as BitmapDrawable
        val icon = Bitmap.createScaledBitmap(bitmapDraw.bitmap, 40, 40, false)
        userMarker = googleMap.addMarker(
            MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(currentLocation)
                .title("user")
        )!!
    }

    private fun initLocationMarkers() {
        viewModel.locationList.let {
            it.forEach { location ->
                createMarker(location)
                createCircle(location)
            }
        }
    }

    private fun setFusedLocationListener() {
        val listener = LocationListener { location ->
            userMarker.position = LatLng(location.latitude, location.longitude)
            currentLocation = LatLng(location.latitude, location.longitude)
        }

        val request = LocationRequest.Builder(5000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(request, listener, Looper.getMainLooper())
        }
    }

    private fun showAddLocationDialog(latLng: LatLng) {
        AddLocationDialog(latLng) {
            viewModel.addLocation(it)
        }.show(supportFragmentManager, "")
    }

    private fun showDeleteDialog(location: LocationEntity) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_location_text))
            .setMessage(location.name)
            .setPositiveButton(getString(R.string.just_yes)) { _, _ -> viewModel.deleteLocation(location) }
            .setNegativeButton(getString(R.string.just_No)) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun moveCamera(coordinate: LatLng, zoom: Float = DEFAULT_ZOOM) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinate, zoom))
    }

    private fun createMarker(location: LocationEntity) {
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(location.latitude, location.longitude))
                .title(location.name)
        )
        marker?.tag = location.id
        markerList.add(marker)
    }

    private fun deleteMarker(id: Int) {
        try { markerList.first { it?.tag == id }?.remove() }
        catch (e: java.util.NoSuchElementException) { e.printStackTrace() }
    }

    private fun createCircle(location: LocationEntity) {
        val circle = googleMap.addCircle(
            CircleOptions()
                .clickable(false)
                .center(LatLng(location.latitude, location.longitude))
                .radius(location.range.toDouble() / 2)
                .strokeColor(Color.BLACK)
                .strokeWidth(2.0f)
                .fillColor(getRandomColor(this))
        )
        circle.tag = location.id
        circleList.add(circle)
    }

    private fun deleteCircle(id: Int) {
        try { circleList.first { it.tag == id }.remove() }
        catch (e: java.util.NoSuchElementException) { e.printStackTrace() }
    }

    private val onClickMarker = OnMarkerClickListener {
        if (it.tag != null) viewModel.findDeleteLocation(it.tag as Int)
        false
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

    override fun setClickEvents() {
        binding.run {
            searchButtonLayout.setOnClickListener {
                viewModel.searchAddress(addressEdit.text.toString())
                hideKeyboard()
            }

            moveMeLayout.setOnClickListener {
                moveCamera(currentLocation, DEFAULT_ZOOM)
                hideKeyboard()
            }

            addressEdit.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    viewModel.searchAddress(binding.addressEdit.text.toString())
                    hideKeyboard()
                }
                false
            }
        }
    }

    private fun hideKeyboard() {
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.addressEdit.windowToken, 0);
    }

    override fun finish() {
        val data = Intent()
        setResult(RESULT_OK, data)
        super.finish()
    }
}