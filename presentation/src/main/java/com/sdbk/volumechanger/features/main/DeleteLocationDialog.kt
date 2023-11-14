package com.sdbk.volumechanger.features.main

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.sdbk.volumechanger.databinding.DialogDeleteLocationBinding
import com.sdbk.volumechanger.room.location.Location

class DeleteLocationDialog(
    private val location: Location,
    private val onClickYes: (Location) -> Unit
): DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DialogDeleteLocationBinding.inflate(inflater, container, false)
        binding.root.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.nameText.text = location.name

        binding.negativeButton.setOnClickListener {
            dialog?.dismiss()
        }

        binding.positiveButton.setOnClickListener {
            onClickYes(location)
            dialog?.dismiss()
        }

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), theme) {
            override fun onBackPressed() {
                if (isCancelable) dialog?.dismiss()
            }
        }
    }
}