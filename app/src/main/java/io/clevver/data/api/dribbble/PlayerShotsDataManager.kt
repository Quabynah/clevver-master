/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.api.dribbble

import android.content.Context
import io.clevver.data.BaseDataManager
import io.clevver.data.PaginatedDataManager
import io.clevver.data.api.dribbble.model.Shot
import io.clevver.data.api.dribbble.model.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Responsible for loading a dribbble player's shots. Instantiating classes are
 * responsible for providing the `onDataLoaded` method to do something with the data.
 */
abstract class PlayerShotsDataManager(context: Context, player: User) : PaginatedDataManager<List<Shot>>(context) {

    private val userId: Long = player.id
    private val isTeam: Boolean = player.type == "Team"
    private var loadShotsCall: Call<List<Shot>>? = null

    override fun loadData(page: Int) {
        if (isTeam) {
            loadTeamShots(page)
        } else {
            loadUserShots(page)
        }
    }

    override fun cancelLoading() {
        if (loadShotsCall != null) loadShotsCall!!.cancel()
    }

    private fun loadUserShots(page: Int) {
        loadShotsCall = dribbbleApi
                .getUsersShots(userId, page, DribbbleService.PER_PAGE_DEFAULT)
        loadShotsCall!!.enqueue(object : Callback<List<Shot>> {
            override fun onResponse(call: Call<List<Shot>>, response: Response<List<Shot>>) {
                if (response.isSuccessful) {
                    val shots = response.body()
                    BaseDataManager.setPage(shots!!, page)
                    BaseDataManager.setDataSource(shots, SOURCE_PLAYER_SHOTS)
                    onDataLoaded(shots)
                    loadFinished()
                    moreDataAvailable = shots.size == DribbbleService.PER_PAGE_DEFAULT
                    loadShotsCall = null
                } else {
                    failure()
                }
            }

            override fun onFailure(call: Call<List<Shot>>, t: Throwable) {
                failure()
            }
        })
    }

    private fun loadTeamShots(page: Int) {
        loadShotsCall = dribbbleApi
                .getTeamShots(userId, page, DribbbleService.PER_PAGE_DEFAULT)
        loadShotsCall!!.enqueue(object : Callback<List<Shot>> {
            override fun onResponse(call: Call<List<Shot>>, response: Response<List<Shot>>) {
                if (response.isSuccessful) {
                    val shots = response.body()
                    BaseDataManager.setPage(shots!!, page)
                    BaseDataManager.setDataSource(shots, SOURCE_TEAM_SHOTS)
                    onDataLoaded(shots)
                    loadFinished()
                    moreDataAvailable = shots.size == DribbbleService.PER_PAGE_DEFAULT
                    loadShotsCall = null
                } else {
                    failure()
                }
            }

            override fun onFailure(call: Call<List<Shot>>, t: Throwable) {
                failure()
            }
        })
    }

    private fun failure() {
        loadFinished()
        loadShotsCall = null
        moreDataAvailable = false
    }

    companion object {

        const val SOURCE_PLAYER_SHOTS = "SOURCE_PLAYER_SHOTS"
        const val SOURCE_TEAM_SHOTS = "SOURCE_TEAM_SHOTS"
    }

}
