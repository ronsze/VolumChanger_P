package com.sdbk.volumechanger.base

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.sdbk.volumechanger.R
import java.util.Locale

abstract class BaseActivity<B: ViewBinding, V: BaseViewModel> : AppCompatActivity() {
    private lateinit var _binding: B
    val binding get() = _binding

    abstract val bindingInflater: (LayoutInflater) -> B
    abstract val viewModel: V

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = bindingInflater(layoutInflater)
        setContentView(binding.root)

        setLanguage()
        initData()
        observeViewModel()
        setClickEvents()
        setFragmentResultListener()
    }

    abstract fun initData()
    abstract fun observeViewModel()

    private fun setLanguage() {
        val locale = Locale.getDefault()

        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    protected fun startActivitySlide(intent: Intent) {
        startActivity(intent)
        overridePendingTransition(R.anim.anim_slide_right, R.anim.anim_slide_left)
    }

    protected fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    protected fun defaultResultLauncher(action: (Intent?) -> Unit) = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            action(it.data)
        }
    }

    protected open fun setClickEvents() {}
    protected open fun setFragmentResultListener() {}
}