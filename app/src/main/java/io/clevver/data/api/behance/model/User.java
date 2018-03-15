
package io.clevver.data.api.behance.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class User implements Parcelable {
	
	public long id;
	public String first_name;
	public String last_name;
	public String username;
	public String city;
	public String state;
	public String country;
	public String location;
	public String company;
	public String occupation;
	public long created_on;
	public String url;
	public Images images;
	public String displayName;
	public List<String> fields = null;
	public int hasDefaultImage;
	public String website;
	public Stats stats;
	public final static Creator<User> CREATOR = new Creator<User>() {
		
		
		@SuppressWarnings({
				"unchecked"
		})
		public User createFromParcel(Parcel in) {
			return new User(in);
		}
		
		public User[] newArray(int size) {
			return (new User[size]);
		}
		
	};
	
	protected User(Parcel in) {
		this.id = ((long) in.readValue((long.class.getClassLoader())));
		this.first_name = ((String) in.readValue((String.class.getClassLoader())));
		this.last_name = ((String) in.readValue((String.class.getClassLoader())));
		this.username = ((String) in.readValue((String.class.getClassLoader())));
		this.city = ((String) in.readValue((String.class.getClassLoader())));
		this.state = ((String) in.readValue((String.class.getClassLoader())));
		this.country = ((String) in.readValue((String.class.getClassLoader())));
		this.location = ((String) in.readValue((String.class.getClassLoader())));
		this.company = ((String) in.readValue((String.class.getClassLoader())));
		this.occupation = ((String) in.readValue((String.class.getClassLoader())));
		this.created_on = ((long) in.readValue((long.class.getClassLoader())));
		this.url = ((String) in.readValue((String.class.getClassLoader())));
		this.images = ((Images) in.readValue((Images.class.getClassLoader())));
		this.displayName = ((String) in.readValue((String.class.getClassLoader())));
		in.readList(this.fields, (String.class.getClassLoader()));
		this.hasDefaultImage = ((int) in.readValue((int.class.getClassLoader())));
		this.website = ((String) in.readValue((String.class.getClassLoader())));
		this.stats = ((Stats) in.readValue((Stats.class.getClassLoader())));
	}
	
	/**
	 * No args constructor for use in serialization
	 */
	public User() {
	}
	
	public User(long id, String first_name, String last_name, String username, String city, String
			state, String country, String location, String company, String occupation, long
			            created_on, String url, Images images, String displayName, List<String> fields,
	            int hasDefaultImage, String website, Stats stats) {
		super();
		this.id = id;
		this.first_name = first_name;
		this.last_name = last_name;
		this.username = username;
		this.city = city;
		this.state = state;
		this.country = country;
		this.location = location;
		this.company = company;
		this.occupation = occupation;
		this.created_on = created_on;
		this.url = url;
		this.images = images;
		this.displayName = displayName;
		this.fields = fields;
		this.hasDefaultImage = hasDefaultImage;
		this.website = website;
		this.stats = stats;
	}
	
	public User withId(int id) {
		this.id = id;
		return this;
	}
	
	public User withFirstName(String first_name) {
		this.first_name = first_name;
		return this;
	}
	
	public User withLastName(String last_name) {
		this.last_name = last_name;
		return this;
	}
	
	public User withUsername(String username) {
		this.username = username;
		return this;
	}
	
	public User withCity(String city) {
		this.city = city;
		return this;
	}
	
	public User withState(String state) {
		this.state = state;
		return this;
	}
	
	public User withCountry(String country) {
		this.country = country;
		return this;
	}
	
	public User withLocation(String location) {
		this.location = location;
		return this;
	}
	
	public User withCompany(String company) {
		this.company = company;
		return this;
	}
	
	public User withOccupation(String occupation) {
		this.occupation = occupation;
		return this;
	}
	
	public User withcreated_on(int created_on) {
		this.created_on = created_on;
		return this;
	}
	
	public User withUrl(String url) {
		this.url = url;
		return this;
	}
	
	public User withImages(Images images) {
		this.images = images;
		return this;
	}
	
	public User withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}
	
	public User withFields(List<String> fields) {
		this.fields = fields;
		return this;
	}
	
	public User withHasDefaultImage(int hasDefaultImage) {
		this.hasDefaultImage = hasDefaultImage;
		return this;
	}
	
	public User withWebsite(String website) {
		this.website = website;
		return this;
	}
	
	public User withStats(Stats stats) {
		this.stats = stats;
		return this;
	}
	
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeValue(id);
		dest.writeValue(first_name);
		dest.writeValue(last_name);
		dest.writeValue(username);
		dest.writeValue(city);
		dest.writeValue(state);
		dest.writeValue(country);
		dest.writeValue(location);
		dest.writeValue(company);
		dest.writeValue(occupation);
		dest.writeValue(created_on);
		dest.writeValue(url);
		dest.writeValue(images);
		dest.writeValue(displayName);
		dest.writeList(fields);
		dest.writeValue(hasDefaultImage);
		dest.writeValue(website);
		dest.writeValue(stats);
	}
	
	public int describeContents() {
		return 0;
	}
	
}
