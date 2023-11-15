package com.sdbk.volumechanger.features.splash.dialog

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.sdbk.volumechanger.R
import com.sdbk.volumechanger.databinding.DialogRequestPermissionBinding
import com.sdbk.volumechanger.features.agreements.PrivacyPolicyActivity
import kotlin.system.exitProcess

class RequestPermissionDialog: DialogFragment() {
    private lateinit var binding: DialogRequestPermissionBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogRequestPermissionBinding.inflate(inflater, container, false)
        binding.root.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isCancelable = false

        setClickEvents()
        setPolicyTextClickEvent()

        return binding.root
    }

    private fun setClickEvents() {
        binding.noButton.setOnClickListener {
            exitProcess(0)
        }

        binding.yesButton.setOnClickListener {
            setFragmentResult(getString(R.string.click_ok), bundleOf())
            dismiss()
        }
    }

    private fun setPolicyTextClickEvent() {
        val privacyPolicySpanString = resources.getString(R.string.privacy_policy)
        val policyStringSpan = SpannableString(binding.agreementsText.text).apply {
            val start = this.indexOf(privacyPolicySpanString)
            val end = start + privacyPolicySpanString.length
            setSpan(onClickPrivacyPolicyClickListener, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        binding.agreementsText.run {
            linksClickable = true
            movementMethod = LinkMovementMethod.getInstance()
            text = policyStringSpan
        }
    }

    private val onClickPrivacyPolicyClickListener = object : ClickableSpan() {
        override fun onClick(view: View) {
            goToPrivacyPolicy()
        }
        override fun updateDrawState(ds: TextPaint) {}
    }

    private fun goToPrivacyPolicy() {
        val intent = Intent(requireActivity(), PrivacyPolicyActivity::class.java)
        startActivity(intent)
    }
}