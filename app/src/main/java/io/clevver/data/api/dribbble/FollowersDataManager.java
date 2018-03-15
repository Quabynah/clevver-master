/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.dribbble;

import android.content.Context;

import java.util.List;

import io.clevver.data.PaginatedDataManager;
import io.clevver.data.api.dribbble.model.Follow;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Loads a dribbble user's followers.
 */
public abstract class FollowersDataManager extends PaginatedDataManager<List<Follow>> {
	
	private final long playerId;
	private Call<List<Follow>> userFollowersCall;
	
	public FollowersDataManager(Context context, long playerId) {
		super(context);
		this.playerId = playerId;
	}
	
	@Override
	public void cancelLoading() {
		if (userFollowersCall != null) userFollowersCall.cancel();
	}
	
	@Override
	protected void loadData(int page) {
		userFollowersCall = getDribbbleApi()
				.getUserFollowers(playerId, page, DribbbleService.PER_PAGE_DEFAULT);
		userFollowersCall.enqueue(new Callback<List<Follow>>() {
			
			@Override
			public void onResponse(Call<List<Follow>> call, Response<List<Follow>> response) {
				if (response.isSuccessful()) {
					loadFinished();
					moreDataAvailable = response.body().size() == DribbbleService.PER_PAGE_DEFAULT;
					onDataLoaded(response.body());
					userFollowersCall = null;
				} else {
					failure();
				}
			}
			
			@Override
			public void onFailure(Call<List<Follow>> call, Throwable t) {
				failure();
			}
			
			private void failure() {
				loadFinished();
				moreDataAvailable = false;
				userFollowersCall = null;
			}
		});
	}
}
