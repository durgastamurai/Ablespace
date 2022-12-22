package io.ablespace.androidapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.ablespace.androidapp.BuildConfig
import io.ablespace.androidapp.R
import io.ablespace.androidapp.app.Constants
import io.ablespace.androidapp.databinding.FragmentWebviewBinding
import io.ablespace.androidapp.extensions.viewBinding
import java.io.*
import java.util.*
import kotlin.math.log


class WebViewFragment : Fragment(R.layout.fragment_webview), FileHandler, IWebView {

    private val viewBinding by viewBinding(FragmentWebviewBinding::bind)
    private lateinit var chromeClient: MyChromeClient
    private var mBitmap: Bitmap? = null
    private var bitmapName: String? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressed()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        configureWebViewSettings()
        chromeClient = MyChromeClient(this)
        viewBinding.webView.webChromeClient = chromeClient
        setWebViewClient()
        val jsInterface = JSInterface(requireActivity(), this)
        with(viewBinding.webView) {
            addJavascriptInterface(jsInterface, "Android")
        }
        viewBinding.webView.loadUrl(BuildConfig.URL_BASE + Constants.ROUTE_LOGIN)
        addBackPressListener()
        setWebViewDownloadListener()
    }

    // Need to set webViewClient to avoid redirecting to browser in "Staging" build
    private fun setWebViewClient() {
        viewBinding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?
            ): Boolean {
                return false
            }
        }
    }

    override fun print() {
        printWebView(requireContext(), viewBinding.webView)
    }

    private fun setWebViewDownloadListener() {
        viewBinding.webView.setDownloadListener { url, _, contentDisposition, mimetype, _ ->
            //data:image/png;base64,
            try {
                val name = URLUtil.guessFileName(url, contentDisposition, mimetype)
                val pureBase64Encoded = url.substring(url.indexOf(",") + 1)
                val decodedBytes = Base64.decode(pureBase64Encoded, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                trySaveImage(bitmap, name)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun trySaveImage(bitmap: Bitmap, name: String) {
        val ctx = requireContext()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                saveBitmap(ctx, bitmap, name)
            } catch (e: Exception) {
                Toast.makeText(ctx, e.message, Toast.LENGTH_SHORT).show()
            }
        } else {
            if(!writePermissionGranted(ctx)){
                mBitmap = bitmap
                bitmapName = name
                requestWritePermission()
                return
            }
            try {
                saveBitmap(bitmap, name)
            } catch (e: Exception) {
                Toast.makeText(ctx, e.message, Toast.LENGTH_SHORT).show()
            }
        }
        Toast.makeText(ctx, "Image Saved", Toast.LENGTH_SHORT).show()
    }

    private fun writePermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestWritePermission() {
        // The registered ActivityResultCallback gets the result of this request.
        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                if(mBitmap != null && bitmapName != null) {
                    try {
                        saveBitmap(mBitmap!!, bitmapName!!)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    //if Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    @Throws(IOException::class)
    private fun saveBitmap(bitmap: Bitmap, name: String) {
        val imagesDir = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .toString()
        val image = File(imagesDir, "$name.png")
        val fos = FileOutputStream(image)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.close()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Throws(IOException::class)
    fun saveBitmap(context: Context, bitmap: Bitmap, displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        var uri: Uri? = null
        return runCatching {
            with(context.contentResolver) {
                insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also {
                    uri = it // Keep uri reference so it can be removed on failure
                    openOutputStream(it)?.use { stream ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                            throw IOException("Failed to save bitmap.")
                    } ?: throw IOException("Failed to open output stream.")
                } ?: throw IOException("Failed to create new MediaStore record.")
            }
        }.getOrElse {
            uri?.let { orphanUri ->
                // Don't leave an orphan entry in the MediaStore
                context.contentResolver.delete(orphanUri, null, null)
            }
            throw it
        }
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
            databaseEnabled = true
        }
    }

    private fun addBackPressListener() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, onBackPressedCallback)
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
        } else {
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
                    resultCode, intent
                )
            )
            chromeClient.uploads = null
        }
    }

    fun printWebView(context: Context, webView: WebView) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        printManager?.let {
            val jobName = "${context.getString(R.string.app_name)} Document"
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            val attr = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
            val printJob = it.print(jobName, printAdapter, attr)
//            if(printJob.isCompleted) {
//                Toast.makeText(context, "Printing completed.", LENGTH_SHORT).show()
//            }
        }
    }

    companion object {
        const val TAG = "WebViewFragment"
        const val REQUEST_SELECT_FILE = 2
        //        const val RESULT_CODE_FILECHOOSER = 2
        fun newInstance() = WebViewFragment()
    }

}