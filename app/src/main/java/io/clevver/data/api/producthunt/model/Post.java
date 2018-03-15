/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.producthunt.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import io.clevver.data.PlaidItem;
import timber.log.Timber;

/**
 * Models a post on Product Hunt.
 */
public class Post extends PlaidItem implements Parcelable {
	
	public final String name;
	public final String tagline;
	public final String discussion_url;
	public final String redirect_url;
	//public final Date created_at;
	public final int comments_count;
	public final int votes_count;
	public User user;
	public final List<User> makers;
	public final CurrentUser current_user;
	public final boolean maker_inside;
	//todo: improve this
//	public final Map<String, String> screenshot_url;
	
	public Post(long id,
	            String name,
	            String tagline,
	            String discussion_url,
	            String redirect_url,
	            //Date created_at,
	            int comments_count,
	            int votes_count,
	            User user,
	            List<User> makers,
	            CurrentUser current_user,
	            boolean maker_inside/*,
	            Map<String, String> screenshot_url*/) {
		super(id, name, discussion_url);
		this.name = name;
		//this.title = name;
		this.tagline = tagline;
		this.discussion_url = discussion_url;
		this.redirect_url = redirect_url;
		//this.created_at = created_at;
		this.comments_count = comments_count;
		this.votes_count = votes_count;
		this.user = user;
		this.makers = makers;
		this.current_user = current_user;
		this.maker_inside = maker_inside;
		//todo: improve this
//		this.screenshot_url = screenshot_url;
	}
	
	protected Post(Parcel in) {
		super(in.readLong(), in.readString(), in.readString());
		name = in.readString();
		tagline = in.readString();
		discussion_url = in.readString();
		redirect_url = in.readString();
//        long tmpCreated_at = in.readLong();
		//created_at = tmpCreated_at != -1 ? new Date(tmpCreated_at) : null;
		comments_count = in.readInt();
		votes_count = in.readInt();
		try {
			user = (User) in.readValue(User.class.getClassLoader());
		} catch (RuntimeException e) {
			Timber.e(e);
		}
		if (in.readByte() == 0x01) {
			makers = new ArrayList<>(0);
			in.readList(makers, User.class.getClassLoader());
		} else {
			makers = null;
		}
		current_user = (CurrentUser) in.readValue(CurrentUser.class.getClassLoader());
		maker_inside = in.readByte() != 0x00;
		//todo: improve this
//		screenshot_url = ParcelUtils.readStringMap(in);
	}
	
	/*public String getScreenshotUrl(int width) {
		String url = null;
		for (String widthStr : screenshot_url.keySet()) {
			url = screenshot_url.get(widthStr);
			try {
				int screenshotWidth = Integer.parseInt(widthStr.substring(0, widthStr.length() -
						2));
				if (screenshotWidth > width) {
					break;
				}
			} catch (NumberFormatException nfe) {
			}
		}
		
		return url;
	}*/
	
	@Override
	public String toString() {
		return "Post{" +
				"name='" + name + '\'' +
				", tagline='" + tagline + '\'' +
				", discussion_url='" + discussion_url + '\'' +
				", redirect_url='" + redirect_url + '\'' +
				", comments_count=" + comments_count +
				", votes_count=" + votes_count +
				", user=" + user +
				", makers=" + makers +
				", current_user=" + current_user +
				", maker_inside=" + maker_inside +
				'}';
	}
	
	/* Parcelable stuff */
	
	@SuppressWarnings("unused")
	public static final Creator<Post> CREATOR = new Creator<Post>() {
		@Override
		public Post createFromParcel(Parcel in) {
			return new Post(in);
		}
		
		@Override
		public Post[] newArray(int size) {
			return new Post[size];
		}
	};
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeString(title);
		dest.writeString(url);
		dest.writeString(name);
		dest.writeString(tagline);
		dest.writeString(discussion_url);
		dest.writeString(redirect_url);
		//dest.writeLong(created_at != null ? created_at.getTime() : -1L);
		dest.writeInt(comments_count);
		dest.writeInt(votes_count);
		try {
			dest.writeValue(user);
		} catch (RuntimeException e) {
			Timber.e(e);
		}
		if (makers == null) {
			dest.writeByte((byte) (0x00));
		} else {
			dest.writeByte((byte) (0x01));
			dest.writeList(makers);
		}
		dest.writeValue(current_user);
		dest.writeByte((byte) (maker_inside ? 0x01 : 0x00));
		//todo: improve this
//		ParcelUtils.writeStringMap(screenshot_url, dest);
	}
	
	public static class CurrentUser implements Parcelable {
		
		public final boolean voted_for_post;
		public final boolean commented_on_post;
		
		public CurrentUser(boolean voted_for_post,
		                   boolean commented_on_post) {
			this.voted_for_post = voted_for_post;
			this.commented_on_post = commented_on_post;
		}
		
		protected CurrentUser(Parcel in) {
			voted_for_post = in.readByte() != 0x00;
			commented_on_post = in.readByte() != 0x00;
		}
		
		public static final Creator<CurrentUser> CREATOR = new Creator<CurrentUser>() {
			@Override
			public CurrentUser createFromParcel(Parcel in) {
				return new CurrentUser(in);
			}
			
			@Override
			public CurrentUser[] newArray(int size) {
				return new CurrentUser[size];
			}
		};
		
		@Override
		public int describeContents() {
			return 0;
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeByte((byte) (voted_for_post ? 0x01 : 0x00));
			dest.writeByte((byte) (commented_on_post ? 0x01 : 0x00));
		}
	}
}
