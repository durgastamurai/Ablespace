package io.ablespace.androidapp.ui

import android.app.Activity
import android.util.Log
import android.webkit.JavascriptInterface

interface IWebView {
    fun print()
}

class JSInterface(private val activity: Activity, private val iWebView: IWebView?) {

    // print as pdf or use Printer
    @JavascriptInterface
    fun print() {
        activity.runOnUiThread {
            iWebView?.print()
        }
    }

//    @JavascriptInterface
//    fun print(htmlString: String?) {
////        Log.d("durga", "print: $htmlString")
//        activity.runOnUiThread {
//            if(htmlString.isNullOrEmpty()) {
//                printNative(webView)
//            } else {
//                val wv = WebView(activity)
//                wv.loadData(htmlString, "text/html; charset=utf-8", "UTF-8")
//                printNative(wv)
//            }
//        }
//    }

}