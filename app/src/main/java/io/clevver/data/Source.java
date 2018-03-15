/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data;

import android.support.annotation.DrawableRes;

import java.util.Comparator;

import io.clevver.R;

/**
 * Representation of a data source
 */
public class Source {
	
	public final String key;
	public final int sortOrder;
	public final String name;
	@DrawableRes
	public final
	int iconRes;
	public boolean active;
	
	public Source(String key,
	              int sortOrder,
	              String name,
	              @DrawableRes int iconResId,
	              boolean active) {
		this.key = key;
		this.sortOrder = sortOrder;
		this.name = name;
		this.iconRes = iconResId;
		this.active = active;
	}
	
	public boolean isSwipeDismissable() {
		return false;
	}
	
	public static class DribbbleSource extends Source {
		
		public DribbbleSource(String key,
		                      int sortOrder,
		                      String name,
		                      boolean active) {
			super(key, sortOrder, name, R.drawable.ic_dribbble, active);
		}
	}
	
	public static class DribbbleSearchSource extends DribbbleSource {
		
		public static final String DRIBBBLE_QUERY_PREFIX = "DRIBBBLE_QUERY_";
		private static final int SEARCH_SORT_ORDER = 400;
		
		public final String query;
		
		public DribbbleSearchSource(String query,
		                            boolean active) {
			super(DRIBBBLE_QUERY_PREFIX + query, SEARCH_SORT_ORDER, "“" + query + "”", active);
			this.query = query;
		}
		
		@Override
		public boolean isSwipeDismissable() {
			return true;
		}
	}
	
	public static class SourceComparator implements Comparator<Source> {
		
		@Override
		public int compare(Source lhs, Source rhs) {
			return lhs.sortOrder - rhs.sortOrder;
		}
	}
}


