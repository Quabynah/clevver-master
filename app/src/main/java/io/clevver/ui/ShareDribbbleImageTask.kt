/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui

import android.app.Activity
import android.os.AsyncTask
import android.support.v4.app.ShareCompat
import android.support.v4.content.FileProvider
import com.bumptech.glide.Glide
import io.clevver.BuildConfig
import io.clevver.R
import io.clevver.data.api.dribbble.model.Shot
import timber.log.Timber
import java.io.File

/**
 * An AsyncTask which retrieves a File from the Glide cache then shares it.
 */
internal class ShareDribbbleImageTask(private val activity: Activity, private val shot: Shot) :
        AsyncTask<Void, Void, File>() {

    private val shareText: String
        get() = "“" + shot.title + "” by " + shot.user.name + "\n" + shot.url + "\n" +
                "Image generated from ${activity.getString(R.string.app_name)} \nAn application created by Dennis Bilson"

    override fun doInBackground(vararg params: Void): File? {
        val url = shot.images.best()
        return try {
            Glide.with(activity)
                    .load(url)
                    .downloadOnly(shot.width.toInt(), shot.height.toInt())
                    .get()
        } catch (ex: Exception) {
            Timber.w(ex, "Sharing $url failed")
            null
        }

    }

    override fun onPostExecute(result: File?) {
        if (result == null) {
            return
        }
        // glide cache uses an unfriendly & extension-less name,
        // massage it based on the original
        var fileName = shot.images.best()
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1)
        val renamed = File(result.parent, fileName)
        result.renameTo(renamed)
        val uri = FileProvider.getUriForFile(activity, BuildConfig.FILES_AUTHORITY, renamed)
        ShareCompat.IntentBuilder.from(activity)
                .setText(shareText)
                .setType(getImageMimeType(fileName))
                .setSubject(shot.title)
                .setStream(uri)
                .startChooser()
    }

    private fun getImageMimeType(fileName: String): String {
        if (fileName.endsWith(".png")) {
            return "image/png"
        } else if (fileName.endsWith(".gif")) {
            return "image/gif"
        }
        return "image/jpeg"
    }
}
