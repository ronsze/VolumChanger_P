package com.sdbk.volumechanger.features.map.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import com.sdbk.domain.location.LocationEntity
import com.sdbk.volumechanger.R
import com.sdbk.volumechanger.databinding.DialogAddLocationBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AddLocationDialog(
    private val coordinate: LatLng,
    private val onClickAdd: (LocationEntity) -> Unit
): DialogFragment() {
    companion object {
        const val VOLUME_MUTE = -1
        const val VOLUME_VIBRATION = 0
        const val VOLUME_MAX = 100
    }

    private lateinit var binding: DialogAddLocationBinding
    private lateinit var rangeSpinnerAdapter: ArrayAdapter<CharSequence>
    private var bellVolume: Int = 0
    private var mediaVolume: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAddLocationBinding.inflate(inflater, container, false)
        binding.root.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        initView()
        setOnClickEvent()

        return binding.root
    }

    private fun initView() {
        setAddressFromLatLng()
        rangeSpinnerAdapter = ArrayAdapter.createFromResource(
            requireActivity(),
            R.array.range_array,
            android.R.layout.simple_spinner_item
        )
        rangeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        binding.rangeSpinner.adapter = rangeSpinnerAdapter

        binding.bellVolumeSeekbar.setOnSeekBarChangeListener(bellSeekbarChangeListener)
        binding.mediaVolumeSeekbar.setOnSeekBarChangeListener(mediaSeekbarChangeListener)
    }

    private fun setOnClickEvent() {
        binding.bellMuteButton.setOnClickListener {
            bellVolume = -1
            binding.bellVolumeSeekbar.progress = 0
            changeBellVolumeButtonDrawable()
        }

        binding.bellVibrationButton.setOnClickListener {
            bellVolume = VOLUME_VIBRATION
            binding.bellVolumeSeekbar.progress = bellVolume
            changeBellVolumeButtonDrawable()
        }

        binding.bellMaxButton.setOnClickListener {
            bellVolume = VOLUME_MAX
            binding.bellVolumeSeekbar.progress = bellVolume
            changeBellVolumeButtonDrawable()
        }

        binding.mediaMuteButton.setOnClickListener {
            mediaVolume = -1
            binding.mediaVolumeSeekbar.progress = 0
            changeMediaVolumeButtonDrawable()
        }

        binding.mediaMaxButton.setOnClickListener {
            mediaVolume = VOLUME_MAX
            binding.mediaVolumeSeekbar.progress = mediaVolume
            changeMediaVolumeButtonDrawable()
        }


        binding.addButton.setOnClickListener {
            val range = rangeSpinnerAdapter.getItem(binding.rangeSpinner.selectedItemPosition)?.run {
                substring(0, lastIndex).toInt()
            } ?: 100
            val location = LocationEntity(
                latitude = coordinate.latitude,
                longitude = coordinate.longitude,
                name = binding.nameEdittext.text.toString().ifEmpty { "None" },
                range = range,
                bellVolume = bellVolume,
                mediaVolume = mediaVolume
            )
            onClickAdd(location)
            dialog?.dismiss()
        }
    }

    private fun changeBellVolumeButtonDrawable() {
        when (bellVolume) {
            VOLUME_MUTE -> {
                binding.bellMuteButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_gray)
                binding.bellVibrationButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_white)
                binding.bellMaxButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_white)
            }
            VOLUME_VIBRATION -> {
                binding.bellMuteButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_white)
                binding.bellVibrationButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_gray)
                binding.bellMaxButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_white)
            }
            VOLUME_MAX -> {
                binding.bellMuteButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_white)
                binding.bellVibrationButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_white)
                binding.bellMaxButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_gray)
            }
            else -> {
                binding.bellMuteButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_white)
                binding.bellVibrationButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_white)
                binding.bellMaxButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_white)
            }
        }
    }

    private fun changeMediaVolumeButtonDrawable() {
        when (mediaVolume) {
            VOLUME_MUTE -> {
                binding.mediaMuteButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_gray)
                binding.mediaMaxButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_white)
            }
            VOLUME_MAX -> {
                binding.mediaMuteButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_white)
                binding.mediaMaxButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_gray)
            }
            else -> {
                binding.mediaMuteButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_white)
                binding.mediaMaxButton.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_button_white)
            }
        }
    }

    private val bellSeekbarChangeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekbar: SeekBar, p1: Int, p2: Boolean) {}
        override fun onStartTrackingTouch(seekbar: SeekBar) {}
        override fun onStopTrackingTouch(seekbar: SeekBar) {
            bellVolume = seekbar.progress
            changeBellVolumeButtonDrawable()
        }
    }

    private val mediaSeekbarChangeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekbar: SeekBar, p1: Int, p2: Boolean) {}
        override fun onStartTrackingTouch(seekbar: SeekBar) {}
        override fun onStopTrackingTouch(seekbar: SeekBar) {
            mediaVolume = seekbar.progress
            changeMediaVolumeButtonDrawable()
        }
    }

    private fun setAddressFromLatLng() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val geocoder = Geocoder(requireActivity(), Locale.getDefault())

            geocoder.getFromLocation(coordinate.latitude, coordinate.longitude, 1, GeocodeListener {
                val address = it.first().getAddressLine(0)
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.locationTextview.text = address
                }
            })
        } else {
            requireActivity().lifecycleScope.launch(Dispatchers.Main) {
                val address: CharSequence = withContext(Dispatchers.Default) {
                    val geocoder = Geocoder(requireActivity(), Locale.getDefault())

                    val addressList = geocoder.getFromLocation(coordinate.latitude, coordinate.longitude, 1)
                    addressList?.run {
                        if (isNotEmpty()) first().getAddressLine(0)
                        else ""
                    } ?: ""
                }
                binding.locationTextview.text = address
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), theme) {
            override fun onBackPressed() {
                if (isCancelable) dialog?.dismiss()
            }
        }
    }
}