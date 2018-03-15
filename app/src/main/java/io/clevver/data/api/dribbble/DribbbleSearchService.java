/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.dribbble;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import io.clevver.data.api.dribbble.model.Shot;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Fake-API for searching dribbble
 */
public interface DribbbleSearchService {
	
	String SORT_POPULAR = "";
	String SORT_RECENT = "latest";
	int PER_PAGE_DEFAULT = 12;
	
	@GET("search")
	Call<List<Shot>> search(@Query("q") String query,
	                        @Query("page") Integer page,
	                        @Query("per_page") Integer pageSize,
	                        @Query("s") @SortOrder String sort);
	
	
	/**
	 * magic constants
	 **/
	
	@Retention(RetentionPolicy.SOURCE)
	@StringDef({
			SORT_POPULAR,
			SORT_RECENT
	})
	@interface SortOrder {
	}
	
}
