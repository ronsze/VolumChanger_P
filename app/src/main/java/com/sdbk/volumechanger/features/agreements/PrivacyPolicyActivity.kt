package com.sdbk.volumechanger.features.agreements

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sdbk.volumechanger.databinding.ActivityPrivacyPolicyBinding

class PrivacyPolicyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)

        setContentView(binding.root)
    }
}