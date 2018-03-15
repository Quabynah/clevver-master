/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.dribbble.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Models links to the various quality of images of a shot.
 */
public class Images implements Parcelable {
	
	private static final int[] NORMAL_IMAGE_SIZE = {400, 300};
	private static final int[] TWO_X_IMAGE_SIZE = {800, 600};
//	private static final int[] TWO_X_IMAGE_SIZE = {1280, 1024};
//	private static final int[] THREE_X_IMAGE_SIZE = {1200, 900};
	
	public final String hidpi;
	public final String normal;
	public final String teaser;
	
	public Images(String hidpi, String normal, String teaser) {
		this.hidpi = hidpi;
		this.normal = normal;
		this.teaser = teaser;
	}
	
	protected Images(Parcel in) {
		hidpi = in.readString();
		normal = in.readString();
		teaser = in.readString();
	}
	
	public String best() {
		return TextUtils.isEmpty(hidpi) ? normal : hidpi;
	}
	
	public int[] bestSize() {
		return TextUtils.isEmpty(hidpi) ? NORMAL_IMAGE_SIZE : TWO_X_IMAGE_SIZE;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(hidpi);
		dest.writeString(normal);
		dest.writeString(teaser);
	}
	
	@SuppressWarnings("unused")
	public static final Creator<Images> CREATOR = new Creator<Images>() {
		@Override
		public Images createFromParcel(Parcel in) {
			return new Images(in);
		}
		
		@Override
		public Images[] newArray(int size) {
			return new Images[size];
		}
	};
	
}
