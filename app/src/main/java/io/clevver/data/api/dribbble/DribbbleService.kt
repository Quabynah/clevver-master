/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.dribbble

import android.support.annotation.StringDef
import io.clevver.data.api.dribbble.model.*
import retrofit2.Call
import retrofit2.http.*

/**
 * Dribbble API - http://developer.dribbble.com/v1/
 */
interface DribbbleService {

    @GET("v1/user")
    fun authenticatedUser(): Call<User>

    /* Shots */

    @GET("v1/shots")
    fun getPopular(@Query("page") page: Int?,
                   @Query("per_page") pageSize: Int?): Call<List<Shot>>

    @GET("v1/shots?sort=recent")
    fun getRecent(@Query("page") page: Int?,
                  @Query("per_page") pageSize: Int?): Call<List<Shot>>

    @GET("v1/shots?list=debuts")
    fun getDebuts(@Query("page") page: Int?,
                  @Query("per_page") pageSize: Int?): Call<List<Shot>>

    @GET("v1/shots?list=animated")
    fun getAnimated(@Query("page") page: Int?,
                    @Query("per_page") pageSize: Int?): Call<List<Shot>>

    @GET("v1/shots")
    fun getShots(@Query("list") @ShotType shotType: String,
                 @Query("timeframe") @ShotTimeframe timeframe: String,
                 @Query("sort") @ShotSort shotSort: String): Call<List<Shot>>

    @GET("v1/shots/{id}")
    fun getShot(@Path("id") shotId: Long): Call<Shot>

    @GET("v1/user/following/shots")
    fun getFollowing(@Query("page") page: Int?,
                     @Query("per_page") pageSize: Int?): Call<List<Shot>>

    /* List the authenticated user’s shot likes */
    @GET("v1/user/likes")
    fun getUserLikes(@Query("page") page: Int?,
                     @Query("per_page") pageSize: Int?): Call<List<Like>>

    /* List the authenticated user’s shots */
    @GET("v1/user/shots")
    fun getUserShots(@Query("page") page: Int?,
                     @Query("per_page") pageSize: Int?): Call<List<Shot>>

    /* Shot likes */

    @GET("v1/shots/{id}/likes")
    fun getShotLikes(@Path("id") shotId: Long,
                     @Query("page") page: Int?,
                     @Query("per_page") pageSize: Int?): Call<List<Like>>

    @GET("v1/shots/{id}/like")
    fun liked(@Path("id") shotId: Long): Call<Like>

    @POST("v1/shots/{id}/like")
    fun like(@Path("id") shotId: Long): Call<Like>

    @DELETE("v1/shots/{id}/like")
    fun unlike(@Path("id") shotId: Long): Call<Void>


    /* Comments */

    @GET("v1/shots/{id}/comments")
    fun getComments(@Path("id") shotId: Long,
                    @Query("page") page: Int?,
                    @Query("per_page") pageSize: Int?): Call<List<Comment>>

    @GET("v1/shots/{shot}/comments/{id}/likes")
    fun getCommentLikes(@Path("shot") shotId: Long,
                        @Path("id") commentId: Long): Call<List<Like>>

    @POST("v1/shots/{shot}/comments")
    fun postComment(@Path("shot") shotId: Long,
                    @Query("body") body: String): Call<Comment>


    @DELETE("v1/shots/{shot}/comments/{id}")
    fun deleteComment(@Path("shot") shotId: Long,
                      @Path("id") commentId: Long): Call<Void>

    @GET("v1/shots/{shot}/comments/{id}/like")
    fun likedComment(@Path("shot") shotId: Long,
                     @Path("id") commentId: Long): Call<Like>

    @POST("v1/shots/{shot}/comments/{id}/like")
    fun likeComment(@Path("shot") shotId: Long,
                    @Path("id") commentId: Long): Call<Like>

    @DELETE("v1/shots/{shot}/comments/{id}/like")
    fun unlikeComment(@Path("shot") shotId: Long,
                      @Path("id") commentId: Long): Call<Void>


    /* Users */

    @GET("v1/users/{user}")
    fun getUser(@Path("user") userId: Long): Call<User>

    @GET("v1/users/{user}")
    fun getUser(@Path("user") username: String): Call<User>

    @GET("v1/users/{user}")
    fun getUser(@Path("user") token: AccessToken): Call<User>

    @GET("v1/users/{user}/shots")
    fun getUsersShots(@Path("user") userId: Long,
                      @Query("page") page: Int?,
                      @Query("per_page") pageSize: Int?): Call<List<Shot>>

    @GET("v1/users/{user}/shots")
    fun getUsersShots(@Path("user") username: String,
                      @Query("page") page: Int?,
                      @Query("per_page") pageSize: Int?): Call<List<Shot>>

    @GET("v1/user/following/{user}")
    fun following(@Path("user") userId: Long): Call<Void>

    @GET("v1/user/following/{user}")
    fun following(@Path("user") username: String): Call<Void>

    @PUT("v1/users/{user}/follow")
    fun follow(@Path("user") userId: Long): Call<Void>

    @PUT("v1/users/{user}/follow")
    fun follow(@Path("user") username: String): Call<Void>

    @DELETE("v1/users/{user}/follow")
    fun unfollow(@Path("user") userId: Long): Call<Void>

    @DELETE("v1/users/{user}/follow")
    fun unfollow(@Path("user") username: String): Call<Void>

    @GET("v1/users/{user}/followers")
    fun getUserFollowers(@Path("user") userId: Long,
                         @Query("page") page: Int?,
                         @Query("per_page") pageSize: Int?): Call<List<Follow>>


    /* Teams */

    @GET("v1/teams/{team}/shots")
    fun getTeamShots(@Path("team") teamId: Long,
                     @Query("page") page: Int?,
                     @Query("per_page") pageSize: Int?): Call<List<Shot>>

    @GET("v1/teams/{team}/shots")
    fun getTeamShots(@Path("team") teamName: String,
                     @Query("page") page: Int?,
                     @Query("per_page") pageSize: Int?): Call<List<Shot>>

    @GET("v1/teams/{team}/members")
    fun getTeamMembers(@Path("team") teamId: Long,
                       @Query("page") page: Int?,
                       @Query("per_page") pageSize: Int?): Call<List<User>>

    @GET("v1/teams/{team}/members")
    fun getTeamMembers(@Path("team") teamName: String,
                       @Query("page") page: Int?,
                       @Query("per_page") pageSize: Int?): Call<List<User>>

    // Shot type
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(SHOT_TYPE_ANIMATED, SHOT_TYPE_ATTACHMENTS, SHOT_TYPE_DEBUTS, SHOT_TYPE_PLAYOFFS, SHOT_TYPE_REBOUNDS, SHOT_TYPE_TEAMS)
    annotation class ShotType

    // Shot timeframe
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(SHOT_TIMEFRAME_WEEK, SHOT_TIMEFRAME_MONTH, SHOT_TIMEFRAME_YEAR, SHOT_TIMEFRAME_EVER)
    annotation class ShotTimeframe

    // Short sort order
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(SHOT_SORT_COMMENTS, SHOT_SORT_RECENT, SHOT_SORT_VIEWS)
    annotation class ShotSort

    companion object {

        const val DATE_FORMAT = "yyyy/MM/dd HH:mm:ss Z"
        const val PER_PAGE_MAX = 100
        const val PER_PAGE_DEFAULT = 30


        /* Magic Constants */

        const val SHOT_TYPE_ANIMATED = "animated"
        const val SHOT_TYPE_ATTACHMENTS = "attachments"
        const val SHOT_TYPE_DEBUTS = "debuts"
        const val SHOT_TYPE_PLAYOFFS = "playoffs"
        const val SHOT_TYPE_REBOUNDS = "rebounds"
        const val SHOT_TYPE_TEAMS = "teams"
        const val SHOT_TIMEFRAME_WEEK = "week"
        const val SHOT_TIMEFRAME_MONTH = "month"
        const val SHOT_TIMEFRAME_YEAR = "year"
        const val SHOT_TIMEFRAME_EVER = "ever"
        const val SHOT_SORT_COMMENTS = "comments"
        const val SHOT_SORT_RECENT = "recent"
        const val SHOT_SORT_VIEWS = "views"
    }

}
