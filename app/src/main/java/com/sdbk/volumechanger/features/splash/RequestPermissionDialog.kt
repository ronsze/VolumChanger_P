package com.sdbk.volumechanger.features.splash

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.sdbk.volumechanger.databinding.DialogRequestPermissionBinding

class RequestPermissionDialog(
    private val onClickNo: () -> Unit,
    private val onClickYes: () -> Unit
): DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DialogRequestPermissionBinding.inflate(inflater, container, false)
        binding.root.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.noButton.setOnClickListener {
            onClickNo()
        }

        binding.yesButton.setOnClickListener {
            onClickYes()
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