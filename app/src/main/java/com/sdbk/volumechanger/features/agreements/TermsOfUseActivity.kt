package com.sdbk.volumechanger.features.agreements

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sdbk.volumechanger.databinding.ActivityTermsOfUseBinding

class TermsOfUseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTermsOfUseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsOfUseBinding.inflate(layoutInflater)

        setContentView(binding.root)
    }
}