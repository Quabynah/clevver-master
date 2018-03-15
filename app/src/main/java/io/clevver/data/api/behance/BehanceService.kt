/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.behance

import io.clevver.data.api.behance.model.User
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Behance API - http://www.behance.dev.com/v2/
 */
interface BehanceService {

    @GET("v2/users")
    fun authenticatedUser(): Call<User>

    @GET(" /v2/users/{user}")
    fun getUser(@Path("user") user: User): Call<List<User>>


}