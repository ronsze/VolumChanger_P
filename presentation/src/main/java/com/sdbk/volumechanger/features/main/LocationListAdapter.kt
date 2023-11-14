package com.sdbk.volumechanger.features.main

import android.content.Context
import android.location.Geocoder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.sdbk.volumechanger.databinding.ItemLocationListBinding
import com.sdbk.volumechanger.room.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

class LocationListAdapter(
    private val locationList: ArrayList<Location>,
    private val onClickItem: (Location) -> Unit,
    private val onLongClickItem: (Location) -> Unit
): RecyclerView.Adapter<LocationListViewHolder>() {
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationListViewHolder {
        val binding = ItemLocationListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return LocationListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationListViewHolder, position: Int) {
        val item = locationList[position]
        setAddressFromLatLng(item.latLng, holder.binding.location)

        holder.binding.let {
            it.name.text = item.name

            it.rootLayout.setOnClickListener {
                onClickItem(item)
            }

            it.rootLayout.setOnLongClickListener {
                onLongClickItem(item)
                false
            }
        }
    }

    override fun getItemCount(): Int = locationList.count()

    fun updateList() {
        notifyDataSetChanged()
    }

    private fun setAddressFromLatLng(latLngString: String, locationTextView: TextView) {
        CoroutineScope(Dispatchers.Main).launch {
            val latLng = getLatLngFromString(latLngString)

            var address = withContext(Dispatchers.Default) {
                val geocoder = Geocoder(context, Locale.getDefault())

                val addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                addressList?.first()?.getAddressLine(0)
            } ?: ""
            locationTextView.text = address
        }
    }

    private fun getLatLngFromString(str: String): LatLng {
        val locationString = str.split(",")
        val lat = locationString[0].toDouble()
        val lng = locationString[1].toDouble()
        return LatLng(lat, lng)
    }
}