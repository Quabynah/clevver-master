/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.assist.AssistContent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toolbar
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import io.clevver.ui.GetUxImageTask
import io.clevver.R
import io.clevver.data.api.dribbble.model.Shot
import io.clevver.ui.ShareDribbbleImageTask
import io.clevver.ui.widget.PhotoView
import io.clevver.util.bindView
import io.clevver.util.customtabs.CustomTabActivityHelper
import io.clevver.util.glide.GlideApp

/**
 * Image details from a dribbble shot or firebase news item
 */
class ImageDetailsActivity : Activity() {

    private val container: ViewGroup by bindView(R.id.container)
    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val imageView: PhotoView by bindView(R.id.imageView)

    private var shot: Shot? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_details)
        setActionBar(toolbar)

        toolbar.setNavigationOnClickListener({
            onBackPressed()
        })

        //Get intent from parent activity
        val intent = intent
        if (intent.hasExtra(EXTRA_IMAGE_URL)) {
            //Get intent data
            shot = intent.getParcelableExtra(EXTRA_IMAGE_URL)
            loadImageFromURL()
        }
    }

    private fun loadImageFromURL() {
        if (shot != null) {
            //Toolbar title
            toolbar.title = shot?.title

            //Load image
            val imageSize = shot!!.images!!.bestSize()
            GlideApp.with(this)
                    .load(shot!!.images.best())
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .priority(Priority.IMMEDIATE)
                    .override(imageSize[0], imageSize[1])
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView)

            imageView.setOnClickListener({ openLink(shot!!.url) })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.image, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_share -> {
                if (shot != null) {
                    ShareDribbbleImageTask(this, shot!!).execute()
                }
                true
            }
            R.id.menu_download -> {
                if (shot == null) {
                    Toast.makeText(applicationContext, "Oops! Shot is null", Toast.LENGTH_SHORT).show()
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this@ImageDetailsActivity,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_GRANTED) {
                            getImage()
                        } else {
                            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                    STORAGE_PERMISSION)
                        }

                    }

                }
                true
            }
            R.id.menu_web_view -> {
                if (shot == null) {
                    Toast.makeText(applicationContext, "Oops! Shot is null", Toast.LENGTH_SHORT).show()
                } else {
                    openLink(shot!!.url)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getImage() {
        GetUxImageTask(this, shot!!.url, container)
    }


    @TargetApi(Build.VERSION_CODES.M)
    override fun onProvideAssistContent(outContent: AssistContent) {
        outContent.webUri = Uri.parse(shot!!.url)
    }

    internal fun openLink(url: String) {
        CustomTabActivityHelper.openCustomTab(
                this@ImageDetailsActivity,
                CustomTabsIntent.Builder()
                        .setToolbarColor(ContextCompat.getColor(this@ImageDetailsActivity, R.color.dribbble))
                        .addDefaultShareMenuItem()
                        .build(),
                Uri.parse(url))
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        if (grantResults != null && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getImage()
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(applicationContext, "You need permission for storage", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL"
        private const val STORAGE_PERMISSION = 2323
    }
}
