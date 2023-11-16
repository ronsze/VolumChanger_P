package com.sdbk.volumechanger.features.main.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sdbk.domain.location.LocationEntity
import com.sdbk.volumechanger.databinding.ItemLocationListBinding
import com.sdbk.volumechanger.util.coordinateToAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationListAdapter(
    private val locationList: List<LocationEntity>,
    private val onClickItem: (LocationEntity) -> Unit,
    private val onLongClickItem: (LocationEntity) -> Unit
): RecyclerView.Adapter<LocationListAdapter.LocationListViewHolder>() {
    inner class LocationListViewHolder(val binding: ItemLocationListBinding): RecyclerView.ViewHolder(binding.root)
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationListViewHolder {
        val binding = ItemLocationListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return LocationListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationListViewHolder, position: Int) {
        val item = locationList[position]

        holder.binding.run {
            name.text = item.name

            parentLayout.setOnClickListener { onClickItem(item) }
            parentLayout.setOnLongClickListener {
                onLongClickItem(item)
                false
            }

            CoroutineScope(Dispatchers.Main).launch {
                location.text = coordinateToAddress(context, item.latitude, item.longitude)
            }
        }
    }

    override fun getItemCount(): Int = locationList.count()
}