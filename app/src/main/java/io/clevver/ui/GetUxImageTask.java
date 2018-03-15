/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import io.clevver.R;
import io.clevver.api.ClevverUtils;
import timber.log.Timber;

/**
 * Download image from url
 */
public class GetUxImageTask {
	
	private final Context context;
	private String downloadUrl = "", downloadFileName = "";
	private final ViewGroup container;
	private final MaterialDialog loading;
	
	public GetUxImageTask(Context context, String downloadUrl, ViewGroup container) {
		this.context = context;
		this.downloadUrl = downloadUrl;
		this.container = container;
		
		//Set file name
		this.downloadFileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
		Timber.d(downloadFileName);
		
		//Progress dialog
		loading = new MaterialDialog.Builder(context)
				.theme(Theme.DARK)
				.content(R.string.fetching_image)
				.progress(true, 0)
				.cancelable(false)
				.canceledOnTouchOutside(true)
				.build();
		
		//Execute download task
		new GettingUxImageTask().execute();
	}
	
	private boolean isSDCardPresent() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}
	
	@SuppressLint("StaticFieldLeak")
	private class GettingUxImageTask extends AsyncTask<Void, Void, File> {
		File outputFile;
		File downloadedFile;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			loading.show();
		}
		
		@Override
		protected File doInBackground(Void... voids) {
			try {
				return Glide.with(context)
						.load(downloadUrl)
						.downloadOnly(800, 600)
						.get();
			} catch (InterruptedException | ExecutionException e) {
				Timber.e(e);
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(File result) {
			try {
				if (result == null) {
					loading.dismiss();
					showMessage("Unable to download image");
					return;
				} else {
					showMessage("Image downloaded successfully. Saving image...");
					//Check if SD card exists
					if (isSDCardPresent()) {
						//Set output file's directory
						outputFile = new File(Environment.getExternalStorageDirectory()
								+ "/" + ClevverUtils.CLEVVER_DOWNLOAD_DIR);
					} else {
						loading.dismiss();
						showMessage("No storage device found");
						return;
					}
					
					//If File is not present create directory
					if (!outputFile.exists()) {
						if (outputFile.mkdir()) {
							Timber.d("Directory created");
						} else {
							loading.dismiss();
							showMessage("Directory could not be created");
							return;
						}
					}
					
					//Create output file in main file
					downloadedFile = new File(outputFile, downloadFileName);
					
					//If new file is not present create directory
					if (!downloadedFile.exists()) {
						if (downloadedFile.mkdir()) {
							Timber.d("File created");
						} else {
							loading.dismiss();
							showMessage("File could not be created");
							return;
						}
					}
					
					FileOutputStream fos = new FileOutputStream(downloadedFile);
					fos.flush();
					fos.close();
				}
			} catch (RuntimeException | IOException e) {
				Timber.e(e);
				showMessage("Unable to download image");
				loading.dismiss();
			} finally {
				//Prompt user to view downloaded file
				openFolder();
			}
			super.onPostExecute(result);
		}
	}
	
	private void showMessage(String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}
	
	private void openFolder() {
		Snackbar snackbar = Snackbar.make(container, "Download was successful. Open file?", Snackbar.LENGTH_LONG);
		snackbar.setAction("Open", new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isSDCardPresent()) {
					//Get downloaded file
					File recentFile = new File(Environment.getExternalStorageDirectory() + "/"
							+ ClevverUtils.CLEVVER_DOWNLOAD_DIR);
					
					//Get file state
					if (recentFile.exists()) {
						snackbar.dismiss();
						//navigate user to download folder
						Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
						Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath() + "/" +
								ClevverUtils.CLEVVER_DOWNLOAD_DIR);
						intent.setDataAndType(uri, "file/*");
						context.startActivity(Intent.createChooser(intent, "Open download folder"));
					} else {
						showMessage("Current file does not exists in this directory");
						snackbar.dismiss();
					}
					
				} else {
					snackbar.dismiss();
					showMessage("Cannot access storage now");
				}
			}
		}).show();
	}
}
