/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.dribbble;

import android.content.Context;

import java.util.List;

import io.clevver.data.PaginatedDataManager;
import io.clevver.data.api.dribbble.model.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Loads the members of a given dribbble team.
 */
public abstract class TeamMembersDataManager extends PaginatedDataManager<List<User>> {
	
	private final String teamName;
	private Call<List<User>> teamMembersCall;
	
	public TeamMembersDataManager(Context context, String teamName) {
		super(context);
		this.teamName = teamName;
	}
	
	@Override
	public void cancelLoading() {
		if (teamMembersCall != null) teamMembersCall.cancel();
	}
	
	@Override
	protected void loadData(int page) {
		teamMembersCall = getDribbbleApi()
				.getTeamMembers(teamName, page, DribbbleService.PER_PAGE_DEFAULT);
		teamMembersCall.enqueue(new Callback<List<User>>() {
			
			@Override
			public void onResponse(Call<List<User>> call, Response<List<User>> response) {
				if (response.isSuccessful()) {
					loadFinished();
					final List<User> teamMembers = response.body();
					moreDataAvailable = teamMembers.size() == DribbbleService.PER_PAGE_DEFAULT;
					onDataLoaded(teamMembers);
					teamMembersCall = null;
				} else {
					failure();
				}
			}
			
			@Override
			public void onFailure(Call<List<User>> call, Throwable t) {
				failure();
			}
			
			private void failure() {
				loadFinished();
				moreDataAvailable = false;
				teamMembersCall = null;
			}
		});
	}
}
