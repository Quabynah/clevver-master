
package io.clevver.data.api.behance.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Images implements Parcelable {
	
	public String _50;
	public String _100;
	public String _115;
	public String _138;
	public String _230;
	public String _276;
	public static final Creator<Images> CREATOR = new Creator<Images>() {
		
		
		@SuppressWarnings("unchecked")
		public Images createFromParcel(Parcel in) {
			return new Images(in);
		}
		
		public Images[] newArray(int size) {
			return (new Images[size]);
		}
		
	};
	
	protected Images(Parcel in) {
		this._50 = ((String) in.readValue((String.class.getClassLoader())));
		this._100 = ((String) in.readValue((String.class.getClassLoader())));
		this._115 = ((String) in.readValue((String.class.getClassLoader())));
		this._138 = ((String) in.readValue((String.class.getClassLoader())));
		this._230 = ((String) in.readValue((String.class.getClassLoader())));
		this._276 = ((String) in.readValue((String.class.getClassLoader())));
	}
	
	/**
	 * No args constructor for use in serialization
	 */
	public Images() {
	}
	
	public Images(String _50, String _100, String _115, String _138, String _230, String _276) {
		this._50 = _50;
		this._100 = _100;
		this._115 = _115;
		this._138 = _138;
		this._230 = _230;
		this._276 = _276;
	}
	
	public Images with50(String _50) {
		this._50 = _50;
		return this;
	}
	
	public Images with100(String _100) {
		this._100 = _100;
		return this;
	}
	
	public Images with115(String _115) {
		this._115 = _115;
		return this;
	}
	
	public Images with138(String _138) {
		this._138 = _138;
		return this;
	}
	
	public Images with230(String _230) {
		this._230 = _230;
		return this;
	}
	
	public Images with276(String _276) {
		this._276 = _276;
		return this;
	}
	
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeValue(_50);
		dest.writeValue(_100);
		dest.writeValue(_115);
		dest.writeValue(_138);
		dest.writeValue(_230);
		dest.writeValue(_276);
	}
	
	public int describeContents() {
		return 0;
	}
	
}
