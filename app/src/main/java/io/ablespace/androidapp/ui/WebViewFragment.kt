package io.ablespace.androidapp.ui

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import io.ablespace.androidapp.R
import io.ablespace.androidapp.app.Constants
import io.ablespace.androidapp.databinding.FragmentWebviewBinding
import io.ablespace.androidapp.extensions.viewBinding


class WebViewFragment: Fragment(R.layout.fragment_webview), FileHandler {

    private val viewBinding by viewBinding(FragmentWebviewBinding::bind)
    private lateinit var chromeClient: MyChromeClient

    private val onBackPressedCallback = object: OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressed()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        configureWebViewSettings()
        //viewBinding.webView.webViewClient
        chromeClient = MyChromeClient(this)
        viewBinding.webView.webChromeClient = chromeClient
        viewBinding.webView.loadUrl(Constants.URL_HOME)
        addBackPressListener()
    }

    override fun onShowFileChooser(intent: Intent?) {
        //  js interface?
        //  ask permissions till 10?
        if (intent != null) {
            startActivityForResult(intent, REQUEST_SELECT_FILE)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebViewSettings() {
        viewBinding.webView.settings.apply {
            javaScriptEnabled = true
//            loadWithOverviewMode = true
//            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
//            cacheMode = WebSettings.LOAD_NO_CACHE
            domStorageEnabled = true
            userAgentString += " mobileapp"
            allowFileAccess = true
//            allowContentAccess = true
        }
    }
    
    private fun addBackPressListener() {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == REQUEST_SELECT_FILE) {
            if (null == chromeClient.uploads || intent == null || resultCode != RESULT_OK) {
                return
            }
            chromeClient.uploads?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(
                    resultCode,
                    intent
                )
            )
            chromeClient.uploads = null
        }
    }

    companion object {
        const val TAG = "WebViewFragment"
        const val REQUEST_SELECT_FILE = 2
//        const val RESULT_CODE_FILECHOOSER = 2
        fun newInstance() = WebViewFragment()
    }

}