package io.ablespace.androidapp.ui

import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import io.ablespace.androidapp.app.Constants
import io.ablespace.androidapp.R
import io.ablespace.androidapp.databinding.FragmentWebviewBinding
import io.ablespace.androidapp.extensions.viewBinding

class WebViewFragment: Fragment(R.layout.fragment_webview) {

    private val viewBinding by viewBinding(FragmentWebviewBinding::bind)

    private val onBackPressedCallback = object: OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressed()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewBinding.webView.settings.apply {
            javaScriptEnabled = true
            // loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            domStorageEnabled = true
            userAgentString += " mobileapp"
        }

        //viewBinding.webView.webViewClient
        //viewBinding.webView.webChromeClient
        viewBinding.webView.loadUrl(Constants.URL_HOME)

        activity
            ?.onBackPressedDispatcher
            ?.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    override fun onResume() {
        super.onResume()
        enableBackPressListener()
    }

    override fun onPause() {
        super.onPause()
        disableBackPressListener()
    }

    fun enableBackPressListener() {
        onBackPressedCallback.isEnabled = true
    }

    fun disableBackPressListener() {
        onBackPressedCallback.isEnabled = false
    }

    fun onBackPressed() {
        if (viewBinding.webView.canGoBack()) {
            viewBinding.webView.goBack()
        }
        else {
            disableBackPressListener()
            activity?.onBackPressed()
        }
    }

    companion object {
        const val TAG = "WebViewFragment"

        fun newInstance() = WebViewFragment()
    }

}