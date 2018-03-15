/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.github

import io.clevver.data.api.github.model.GithubRepo
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Github service
 */
interface GithubService {
    //Returns a list of repo
    @GET("/users/{user}/repos")
    fun getRepos(@Path("user") user: String): Call<List<GithubRepo>>

}