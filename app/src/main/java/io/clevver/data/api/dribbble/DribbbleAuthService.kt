/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.dribbble

import io.clevver.data.api.dribbble.model.AccessToken
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Dribbble Auth API (a different endpoint)
 */
interface DribbbleAuthService {

    @POST("/oauth/token")
    fun getAccessToken(@Query("client_id") client_id: String,
                       @Query("client_secret") client_secret: String,
                       @Query("code") code: String): Call<AccessToken>

}
