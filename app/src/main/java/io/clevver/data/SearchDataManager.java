/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import io.clevver.data.BaseDataManager;
import io.clevver.data.api.dribbble.DribbbleSearchService;
import io.clevver.data.api.dribbble.model.Shot;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Responsible for loading search results from dribbble and designer news. Instantiating classes are
 * responsible for providing the {code onDataLoaded} method to do something with the data.
 */
public abstract class SearchDataManager extends BaseDataManager<List<? extends PlaidItem>> {

    // state
    private String query = "";
    private int page = 1;
    private List<Call> inflight;

    public SearchDataManager(Context context) {
        super(context);
        inflight = new ArrayList<>(0);
    }

    public void searchFor(String query) {
        if (this.query.equals(query)) {
            page++;
        } else {
            clear();
            this.query = query;
        }
        searchDribbble(query, page);
    }

    public void loadMore() {
        searchFor(query);
    }

    public void clear() {
        cancelLoading();
        query = "";
        page = 1;
        resetLoadingCount();
    }

    @Override
    public void cancelLoading() {
        if (inflight.size() > 0) {
            for (Call call : inflight) {
                call.cancel();
            }
            inflight.clear();
        }
    }

    public String getQuery() {
        return query;
    }

    private void searchDribbble(final String query, final int resultsPage) {
        loadStarted();
        final Call<List<Shot>> dribbbleSearchCall = getDribbbleSearchApi().search(
                query, resultsPage, DribbbleSearchService.PER_PAGE_DEFAULT,
                DribbbleSearchService.SORT_POPULAR);
        dribbbleSearchCall.enqueue(new Callback<List<Shot>>() {
            @Override
            public void onResponse(Call<List<Shot>> call, Response<List<Shot>> response) {
                if (response.isSuccessful()) {
                    loadFinished();
                    final List<Shot> shots = response.body();
                    if (shots != null) {
                        Companion.setPage(shots, resultsPage);
                        Companion.setDataSource(shots,
                                Source.DribbbleSearchSource.DRIBBBLE_QUERY_PREFIX + query);
                        onDataLoaded(shots);
                    }
                    inflight.remove(dribbbleSearchCall);
                } else {
                    failure(dribbbleSearchCall);
                }
            }

            @Override
            public void onFailure(Call<List<Shot>> call, Throwable t) {
                failure(dribbbleSearchCall);
            }
        });
        inflight.add(dribbbleSearchCall);
    }

    private void failure(Call call) {
        loadFinished();
        inflight.remove(call);
    }

}
