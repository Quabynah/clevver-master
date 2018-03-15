/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import io.clevver.R
import io.clevver.ui.HomeActivity
import io.clevver.util.bindView

/**
 * Splash screen for application
 */
class SplashActivity : Activity() {


    private val container: ViewGroup by bindView(R.id.container)
    private val text: TextView by bindView(R.id.splash_text)
    private val loading: ProgressBar by bindView(R.id.splash_progress)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            sendCircularReveal()
        }, 2000)

    }

    /**
     * Circular reveal animation was extracted from
     * {@Link https://android.jlelse.eu/a-little-thing-that-matter-how-to-reveal-an-activity-with-circular-revelation-d94f9bfcae28}
     */
    private fun sendCircularReveal() {
        loading.visibility = View.GONE
        setupBackground()
    }

    private fun setupBackground() {
        TransitionManager.beginDelayedTransition(container)
        container.setBackgroundColor(resources.getColor(R.color.white))
        text.text = "Welcome to ${getString(R.string.app_name)}"
        text.setTextColor(resources.getColor(R.color.text_primary_dark))
        Handler().postDelayed({
            //Do circular reveal
            val options = ActivityOptions.makeSceneTransitionAnimation(this, container,
                    "transition")
            val revealX = (container.x.plus(container.width.div(2))).toInt()
            val revealY = (container.y.plus(container.height.div(2))).toInt()

            val intent = Intent(this@SplashActivity, HomeActivity::class.java)
            intent.putExtra(HomeActivity.EXTRA_CIRCULAR_REVEAL_X, revealX)
            intent.putExtra(HomeActivity.EXTRA_CIRCULAR_REVEAL_Y, revealY)
            startActivity(intent, options.toBundle())
            finish()
        }, 1000)
    }

}
