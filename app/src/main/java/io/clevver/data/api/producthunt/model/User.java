/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.producthunt.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Models a user on Product Hunt.
 */
public class User implements Parcelable {
	
	public static final Creator<User> CREATOR = new Creator<User>() {
		@Override
		public User createFromParcel(Parcel in) {
			return new User(in);
		}
		
		@Override
		public User[] newArray(int size) {
			return new User[size];
		}
	};
	public final long id;
	public final String name;
	public final String headline;
	//public final Date created_at;
	public final String username;
	public final String website_url;
	public final String profile_url;
	//todo: improve this
//    public final Map<String, String> image_url;
	
	public User(long id,
	            String name,
	            String headline,
	            //Date created_at,
	            String username,
	            String website_url,
	            String profile_url/*,
	            Map<String, String> image_url*/) {
		this.id = id;
		this.name = name;
		this.headline = headline;
		//this.created_at = created_at;
		this.username = username;
		this.website_url = website_url;
		this.profile_url = profile_url;
		//todo: improve this
//		this.image_url = image_url;
	}
	
	protected User(Parcel in) {
		id = in.readLong();
		name = in.readString();
		headline = in.readString();
//        long tmpCreated_at = in.readLong();
		//created_at = tmpCreated_at != -1 ? new Date(tmpCreated_at) : null;
		username = in.readString();
		website_url = in.readString();
		profile_url = in.readString();
		//todo: improve this
//        image_url = ParcelUtils.readStringMap(in);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeString(name);
		dest.writeString(headline);
		//dest.writeLong(created_at != null ? created_at.getTime() : -1L);
		dest.writeString(username);
		dest.writeString(website_url);
		dest.writeString(profile_url);
		//todo: improve this
//        ParcelUtils.writeStringMap(image_url, dest);
	}
}