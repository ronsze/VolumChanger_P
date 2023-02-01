    package com.sdbk.volumechanger.features.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.sdbk.volumechanger.R
import com.sdbk.volumechanger.base.BaseActivity
import com.sdbk.volumechanger.databinding.ActivityMainBinding
import com.sdbk.volumechanger.features.map.MapActivity
import com.sdbk.volumechanger.room.location.Location
import com.sdbk.volumechanger.room.location.LocationDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

    @AndroidEntryPoint
class MainActivity: BaseActivity() {
    companion object {
        const val LOCATION_LIST = "location_list"
    }

    private lateinit var binding: ActivityMainBinding
    @Inject lateinit var locationDao: LocationDao

    private lateinit var locationListAdapter: LocationListAdapter
    private lateinit var deleteLocationDialog: DeleteLocationDialog

    private val mapResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                updateList()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
        setClickEvent()
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
}