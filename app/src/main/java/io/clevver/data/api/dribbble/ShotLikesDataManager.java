/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.dribbble;

import android.content.Context;


import java.util.List;

import io.clevver.data.PaginatedDataManager;
import io.clevver.data.api.dribbble.model.Like;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Loads the dribbble players who like a given shot.
 */
public abstract class ShotLikesDataManager extends PaginatedDataManager<List<Like>> {
	
	private final long shotId;
	private Call<List<Like>> shotLikesCall;
	
	public ShotLikesDataManager(Context context, long shotId) {
		super(context);
		this.shotId = shotId;
	}
	
	@Override
	public void cancelLoading() {
		if (shotLikesCall != null) shotLikesCall.cancel();
	}
	
	@Override
	protected void loadData(int page) {
		shotLikesCall = getDribbbleApi()
				.getShotLikes(shotId, page, DribbbleService.PER_PAGE_DEFAULT);
		shotLikesCall.enqueue(new Callback<List<Like>>() {
			
			@Override
			public void onResponse(Call<List<Like>> call, Response<List<Like>> response) {
				if (response.isSuccessful()) {
					loadFinished();
					final List<Like> likes = response.body();
					moreDataAvailable = likes.size() == DribbbleService.PER_PAGE_DEFAULT;
					onDataLoaded(likes);
					shotLikesCall = null;
				} else {
					failure();
				}
			}
			
			@Override
			public void onFailure(Call<List<Like>> call, Throwable t) {
				failure();
			}
			
			private void failure() {
				loadFinished();
				moreDataAvailable = false;
				shotLikesCall = null;
			}
		});
	}
}
