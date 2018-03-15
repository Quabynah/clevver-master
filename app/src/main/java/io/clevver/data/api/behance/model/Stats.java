
package io.clevver.data.api.behance.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Stats implements Parcelable {
	
	public long followers;
	public long following;
	public long appreciations;
	public long views;
	public long comments;
	public static final Creator<Stats> CREATOR = new Creator<Stats>() {
		
		
		@SuppressWarnings("unchecked")
		public Stats createFromParcel(Parcel in) {
			return new Stats(in);
		}
		
		public Stats[] newArray(int size) {
			return (new Stats[size]);
		}
		
	};
	
	protected Stats(Parcel in) {
		this.followers = ((long) in.readValue((long.class.getClassLoader())));
		this.following = ((long) in.readValue((long.class.getClassLoader())));
		this.appreciations = ((long) in.readValue((int.class.getClassLoader())));
		this.views = ((long) in.readValue((long.class.getClassLoader())));
		this.comments = ((long) in.readValue((long.class.getClassLoader())));
	}
	
	/**
	 * No args constructor for use in serialization
	 */
	public Stats() {
	}
	
	/**
	 * @param following
	 * @param followers
	 * @param views
	 * @param appreciations
	 * @param comments
	 */
	public Stats(long followers, long following, long appreciations, long views, long comments) {
		this.followers = followers;
		this.following = following;
		this.appreciations = appreciations;
		this.views = views;
		this.comments = comments;
	}
	
	public Stats withFollowers(int followers) {
		this.followers = followers;
		return this;
	}
	
	public Stats withFollowing(int following) {
		this.following = following;
		return this;
	}
	
	public Stats withAppreciations(int appreciations) {
		this.appreciations = appreciations;
		return this;
	}
	
	public Stats withViews(int views) {
		this.views = views;
		return this;
	}
	
	public Stats withComments(int comments) {
		this.comments = comments;
		return this;
	}
	
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeValue(followers);
		dest.writeValue(following);
		dest.writeValue(appreciations);
		dest.writeValue(views);
		dest.writeValue(comments);
	}
	
	public int describeContents() {
		return 0;
	}
	
}
