/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.support.design.widget.Snackbar
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import io.clevver.R
import timber.log.Timber
import java.io.*
import java.net.URL

internal class DownloadImageTask(@SuppressLint("StaticFieldLeak") private val context: Context,
                                 @SuppressLint("StaticFieldLeak") private val view: View,
                                 private val imageName: String?) :
        AsyncTask<String, Void, Bitmap>() {
    private var loading: MaterialDialog = MaterialDialog.Builder(context)
            .theme(Theme.DARK)
            .progress(true, 0)
            .cancelable(false)
            .canceledOnTouchOutside(false)
            .content(context.getString(R.string.fetching_image))
            .build()

    override fun onPreExecute() {
        super.onPreExecute()
        loading.show()
    }

    override fun doInBackground(vararg params: String?): Bitmap? {
        var bitmap: Bitmap? = null
        val imageUrl = params[0]
        try {
            val inputStream = URL(imageUrl).openStream()
            bitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Timber.e(e)
        }
        return bitmap
    }

    override fun onPostExecute(result: Bitmap?) {
        loading.dismiss()
        saveImage(result)
    }

    private fun saveImage(bitmap: Bitmap?) {
        if (bitmap == null) {
            Timber.d("Cannot save image")
            return
        } else {
            var fos: FileOutputStream? = null
            try {
                fos = context.openFileOutput(imageName, Context.MODE_PRIVATE)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)

            } catch (e: FileNotFoundException) {
                Timber.e(e)
            } catch (e: IOException) {
                Timber.e(e)
            } finally {
                fos?.close()
                val file = File(context.filesDir, imageName)
                var inputStream: FileInputStream? = null
                try {
                    if (file.exists()) {
                        inputStream = FileInputStream(file)
                        val filePath: String = inputStream.toString()
                        Snackbar.make(view, "Download completed successfully", Snackbar.LENGTH_LONG)
                                .setAction("Open", {
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.data = Uri.parse(filePath)
                                    context.startActivity(Intent.createChooser(intent, "Open with"))
                                }).setActionTextColor(context.resources.getColor(android.R.color.holo_green_light))
                                .show()
                    }
                } catch (e: IOException) {
                    Timber.e(e)
                } finally {
                    inputStream?.close()
                }

            }

        }
    }

}