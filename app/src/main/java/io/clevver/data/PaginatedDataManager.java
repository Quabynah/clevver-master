/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data;

import android.content.Context;

/**
 * Load a paginated data source. Instantiating classes are responsible for providing implementations
 * of {@link #loadData(int)} to actually load the data, and {@link #onDataLoaded} to do something
 * with it.
 */
public abstract class PaginatedDataManager<T> extends BaseDataManager<T> {
	
	// state
	private int page = 0;
	protected boolean moreDataAvailable = true;
	
	public PaginatedDataManager(Context context) {
		super(context);
	}
	
	public void loadData() {
		if (!moreDataAvailable) return;
		page++;
		loadStarted();
		loadData(page);
	}
	
	/**
	 * Extending classes must provide this method to actually load data. They must call
	 * {@link #loadFinished()} when finished.
	 */
	protected abstract void loadData(int page);
	
}
