/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.github.model;


import android.os.Parcel;
import android.os.Parcelable;

/**
 * Data model for github repositories
 */
public class GithubRepo implements Parcelable {
	private String name;
	
	public GithubRepo() {
	}
	
	public GithubRepo(String name) {
		this.name = name;
	}
	
	protected GithubRepo(Parcel in) {
		name = in.readString();
	}
	
	public static final Creator<GithubRepo> CREATOR = new Creator<GithubRepo>() {
		@Override
		public GithubRepo createFromParcel(Parcel in) {
			return new GithubRepo(in);
		}
		
		@Override
		public GithubRepo[] newArray(int size) {
			return new GithubRepo[size];
		}
	};
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		
		dest.writeString(name);
	}
}
