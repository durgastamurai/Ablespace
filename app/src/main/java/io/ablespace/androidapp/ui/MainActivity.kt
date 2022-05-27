package io.ablespace.androidapp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.ablespace.androidapp.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        //Change from Splash theme to Main theme
        setTheme(R.style.Theme_Ablespace)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //load main fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container_view, WebViewFragment(), WebViewFragment.TAG)
                .commit()
        }
    }

}