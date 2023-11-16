package com.sdbk.volumechanger.features.main

import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import androidx.activity.viewModels
import com.sdbk.domain.Constants.LOCATION
import com.sdbk.domain.Constants.LOCATION_LIST
import com.sdbk.domain.location.LocationEntity
import com.sdbk.domain.location.LocationListWrapper
import com.sdbk.volumechanger.R
import com.sdbk.volumechanger.base.BaseActivity
import com.sdbk.volumechanger.databinding.ActivityMainBinding
import com.sdbk.volumechanger.features.main.adapter.LocationListAdapter
import com.sdbk.volumechanger.features.map.MapActivity
import com.sdbk.volumechanger.module.GeofenceModule
import com.sdbk.volumechanger.util.getSerializable
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {
    override val bindingInflater: (LayoutInflater) -> ActivityMainBinding = ActivityMainBinding::inflate
    override val viewModel: MainViewModel by viewModels()

    private val locationListAdapter: LocationListAdapter by lazy {
        LocationListAdapter(viewModel.locationList, this::navigateToMap, this::onLongClickItem)
    }

    private val mapResultLauncher = defaultResultLauncher {
        viewModel.updateList()
    }

    private val geofenceModule by lazy {
        GeofenceModule(this)
    }

    override fun initData() {
        viewModel.setData(intent.getSerializable<LocationListWrapper>(LOCATION_LIST)?.locationList)
        binding.locationRecyclerView.adapter = locationListAdapter
        locationListAdapter.notifyItemChanged(0, viewModel.locationList.size)
    }

    override fun observeViewModel() {
        viewModel.removeGeofenceEvent.observe(this) {
            geofenceModule.removeGeofence(listOf(it.toString()), {
                showToast(getString(R.string.location_removed))
            })
        }
    }

    override fun setClickEvents() {
        binding.addButton.setOnClickListener { navigateToMap() }
    }

    private fun onLongClickItem(location: LocationEntity) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_location_text))
            .setMessage(location.name)
            .setPositiveButton(getString(R.string.just_yes)) { _, _ -> viewModel.deleteLocation(location) }
            .setNegativeButton(getString(R.string.just_No)) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun navigateToMap(location: LocationEntity? = null) {
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra(LOCATION, location)
        intent.putExtra(LOCATION_LIST, LocationListWrapper(ArrayList(viewModel.locationList)))
        mapResultLauncher.launch(intent)
        overridePendingTransition(R.anim.anim_slide_right, R.anim.anim_slide_left)
    }
}