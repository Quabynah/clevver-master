package io.clevver.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import io.clevver.R

@SuppressLint("GoogleAppIndexingApiWarning")
/**
 * Unsplash image details activity
 */
class UnsplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unsplash)
    }
}
