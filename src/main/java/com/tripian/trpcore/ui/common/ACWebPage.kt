package com.tripian.trpcore.ui.common

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcWebPageBinding
import com.tripian.trpcore.util.extensions.observe


/**
 * Created by semihozkoroglu on 13.09.2020.
 */
class ACWebPage : BaseActivity<AcWebPageBinding, ACWebPageVM>() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.webview.settings.javaScriptEnabled = true
    }

    override fun getViewBinding(): AcWebPageBinding {
        return AcWebPageBinding.inflate(layoutInflater)
    }

    override fun onResume() {
        super.onResume()
        // Enter -> this class
        // Exit  -> paused class
        overridePendingTransition(R.anim.anim_slide_in_up, R.anim.anim_slide_out_up)
    }

    override fun onPause() {
        super.onPause()
        // Enter -> resumed class
        // Exit  -> this
        overridePendingTransition(R.anim.anim_slide_in_down, R.anim.anim_slide_out_down)
    }

    override fun setListeners() {
        binding.imClose.setOnClickListener { viewModel.onClickedClose() }

        binding.webview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                viewModel.onProgressChanged(progress)
            }
        }
    }

    override fun setReceivers() {
        observe(viewModel.onSetWebUrlListener) {
            binding.webview.loadUrl(it!!)
        }
    }

    override fun backPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            super.backPressed()
        }
    }
}