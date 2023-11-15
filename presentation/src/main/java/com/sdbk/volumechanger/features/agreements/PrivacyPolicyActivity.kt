package com.sdbk.volumechanger.features.agreements

import android.view.LayoutInflater
import androidx.activity.viewModels
import com.sdbk.volumechanger.base.BaseActivity
import com.sdbk.volumechanger.databinding.ActivityPrivacyPolicyBinding
import java.util.Locale

class PrivacyPolicyActivity : BaseActivity<ActivityPrivacyPolicyBinding, PrivacyPolicyViewModel>() {
    override val bindingInflater: (LayoutInflater) -> ActivityPrivacyPolicyBinding = ActivityPrivacyPolicyBinding::inflate
    override val viewModel: PrivacyPolicyViewModel by viewModels()

    override fun initData() {
        binding.webView.run {
            loadUrl(viewModel.getUrl(Locale.getDefault()))
            settings.textZoom = 50
        }
    }

    override fun observeViewModel() {}
}