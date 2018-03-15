/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.producthunt;

import java.util.List;

import io.clevver.data.api.EnvelopePayload;
import io.clevver.data.api.producthunt.model.Post;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Models the Product Hunt API. See https://api.producthunt.com/v1/docs
 */
public interface ProductHuntService {
	
	@EnvelopePayload("posts")
	@GET("v1/posts")
	Call<List<Post>> getPosts(@Query("days_ago") Integer page);
}
