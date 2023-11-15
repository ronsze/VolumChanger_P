package com.sdbk.volumechanger.features.agreements

import com.sdbk.volumechanger.base.BaseViewModel
import java.util.Locale

class PrivacyPolicyViewModel: BaseViewModel() {
    fun getUrl(locale: Locale) =
        when (locale) {
            Locale.ENGLISH -> "https://sites.google.com/view/volumechanger-privacy-en/%ED%99%88"
            else -> "https://sites.google.com/view/volumechager-privacy/%ED%99%88"
        }
}