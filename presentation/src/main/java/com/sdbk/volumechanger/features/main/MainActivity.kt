package com.sdbk.volumechanger.features.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.location.*
import com.sdbk.volumechanger.BuildConfig
import com.sdbk.volumechanger.R
import com.sdbk.volumechanger.base.BaseActivity
import com.sdbk.volumechanger.data.LocationData
import com.sdbk.volumechanger.databinding.ActivityMainBinding
import com.sdbk.volumechanger.features.geofence.GeofenceBroadcastReceiver
import com.sdbk.volumechanger.features.map.MapActivity
import com.sdbk.volumechanger.room.location.Location
import com.sdbk.volumechanger.room.location.LocationDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    override val viewModel: MainViewModel by viewModels()

    private val context: Context = this
    @Inject
    lateinit var locationDao: LocationDao
    private lateinit var geofencingClient: GeofencingClient

    private lateinit var locationListAdapter: LocationListAdapter
    private lateinit var deleteLocationDialog: DeleteLocationDialog

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLocation: String

    private lateinit var adContainerView: FrameLayout
    private var initialLayoutComplete = false

    private val mapResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.updateList()
            showInterstitialAdv()
            setCurrentLocation()
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

    override fun initData() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.locationList =
            (intent.getSerializableExtra(LOCATION_LIST) as? LocationData)?.locationList
                ?: arrayListOf()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setCurrentLocation()

        geofencingClient = LocationServices.getGeofencingClient(this)
        setClickEvent()
        initView()
    }

    override fun observeViewModel() {
        viewModel.updateListEvent.observe(this) {
            locationListAdapter.updateList()
        }
    }

    private fun initView() {
        locationListAdapter = LocationListAdapter(
            viewModel.locationList,
            this::onClickItem,
            this::onLongClickItem
        )
        binding.locationRecyclerView.adapter = locationListAdapter
        loadAdv()
    }

    private fun setCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) { return }
        fusedLocationClient.lastLocation.addOnCompleteListener {
            try {
                if (it.isSuccessful) {
                    currentLocation = "${it.result.latitude},${it.result.longitude}"
                }
            } catch (e: java.lang.NullPointerException) {

            }
        }
    }

    private fun setClickEvent() {
        binding.addButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                val anim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.anim_opacity)
                binding.addButton.startAnimation(anim)

                withContext(Dispatchers.Default) { delay(50) }

                goToMap()
            }
        }
    }

    private fun deleteItem(location: Location) {
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                locationDao.deleteLocationBy(location)
            }
            removeGeofences()
            viewModel.updateList()
        }
    }

    private fun onClickItem(location: Location) {
        goToMap(location)
    }

    private fun onLongClickItem(location: Location) {
        deleteLocationDialog = DeleteLocationDialog(location) {
            deleteItem(location)
        }
        deleteLocationDialog.show(supportFragmentManager, deleteLocationDialog.tag)
    }

    private fun addGeofences() {
        lifecycleScope.launch(Dispatchers.Main) {
            val geofenceList = ArrayList<Geofence>()
            val locationList = withContext(Dispatchers.IO) {
                locationDao.getAll()
            }

            locationList.forEach {
                geofenceList.add(
                    viewModel.getGeofence(
                        it.latLng,
                        viewModel.getLatLngFromString(it.latLng),
                        it.range.toFloat()
                    )
                )
            }

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                geofencingClient.addGeofences(getGeofencingRequest(geofenceList), geoPending).run {
                    addOnSuccessListener {}
                    addOnFailureListener {}
                }
            }
        }
    }

    private fun getGeofencingRequest(list: ArrayList<Geofence>): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(list)
        }.build()
    }

    private fun removeGeofences() {
        geofencingClient.removeGeofences(geoPending).run {
            addOnSuccessListener {
                addGeofences()
            }
            addOnFailureListener { }
        }
    }

    private fun goToMap(location: Location? = null) {
        val intent = Intent(this, MapActivity::class.java)
        location?.run { intent.putExtra(MapActivity.LAT_LNG, location.latLng) }
        if (this::currentLocation.isInitialized) intent.putExtra("current_location", currentLocation)
        mapResultLauncher.launch(intent)
        overridePendingTransition(R.anim.anim_slide_right, R.anim.anim_slide_left)
    }

    private fun showInterstitialAdv() {
        var mInterstitialAd: InterstitialAd? = null

        var adRequest = AdRequest.Builder().build()

        val unitId =
            if (BuildConfig.DEBUG) getString(R.string.admob_interstitial_id_test)
            else getString(R.string.admob_interstitial_id)

        InterstitialAd.load(this, unitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(p0: LoadAdError) {
                mInterstitialAd = null
            }

            override fun onAdLoaded(p0: InterstitialAd) {
                mInterstitialAd = p0
                mInterstitialAd?.show(this@MainActivity)
            }
        })
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

}