package com.sdbk.volumechanger.features.agreements

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sdbk.volumechanger.databinding.ActivityPrivacyPolicyBinding
import java.util.Locale

class PrivacyPolicyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val url = when (Locale.getDefault()) {
            Locale.ENGLISH -> "https://sites.google.com/view/volumechanger-privacy-en/%ED%99%88"
            else -> "https://sites.google.com/view/volumechager-privacy/%ED%99%88"
        }
        binding.webView.run {
            loadUrl(url)
            settings.textZoom = 50
        }
    }
}