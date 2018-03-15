/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data

import android.app.Activity
import io.clevver.data.api.dribbble.DribbbleSearchService
import io.clevver.data.api.dribbble.DribbbleService
import io.clevver.data.api.dribbble.model.Like
import io.clevver.data.api.dribbble.model.Shot
import io.clevver.data.api.producthunt.model.Post
import io.clevver.data.prefs.SourceManager
import io.clevver.ui.FilterAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

/**
 * Responsible for loading data from the various sources. Instantiating classes are responsible for
 * providing the [onDataLoaded] method to do something with the data.
 */
abstract class DataManager(context: Activity,
                           private val filterAdapter: FilterAdapter) : BaseDataManager<List<PlaidItem>>(context) {
    private var pageIndexes: MutableMap<String, Int> = HashMap(0)
    private val inflight: MutableMap<String, Any> = HashMap(0)

    private val filterListener = object : FilterAdapter.FiltersChangedCallbacks() {
        override fun onFiltersChanged(changedFilter: Source) {
            if (changedFilter.active) {
                loadSource(changedFilter)
            } else { // filter deactivated
                val key = changedFilter.key
                if (inflight.containsKey(key)) {
                    val call = inflight[key] as Call<*>
                    call.cancel()
                    inflight.remove(key)
                }
                // clear the page index for the source
                pageIndexes[key] = 0
            }
        }
    }

    init {
        filterAdapter.registerFilterChangedCallback(filterListener)
        setupPageIndexes()
    }

    fun loadAllDataSources() {
        for (filter in filterAdapter.filters) {
            loadSource(filter)
        }
    }

    override fun cancelLoading() {
        if (!inflight.isEmpty()) {
            for (o in inflight.values) {
                (o as? Call<*>)?.cancel()
            }
            inflight.clear()
        }
    }

    private fun loadSource(source: Source) {
        if (source.active) {
            loadStarted()
            val page = getNextPageIndex(source.key)
            when (source.key) {
                SourceManager.SOURCE_DRIBBBLE_POPULAR -> loadDribbblePopular(page)
                SourceManager.SOURCE_DRIBBBLE_FOLLOWING -> loadDribbbleFollowing(page)
                SourceManager.SOURCE_DRIBBBLE_USER_LIKES -> loadDribbbleUserLikes(page)
                SourceManager.SOURCE_DRIBBBLE_USER_SHOTS -> loadDribbbleUserShots(page)
                SourceManager.SOURCE_DRIBBBLE_RECENT -> loadDribbbleRecent(page)
                SourceManager.SOURCE_DRIBBBLE_DEBUTS -> loadDribbbleDebuts(page)
                SourceManager.SOURCE_DRIBBBLE_ANIMATED -> loadDribbbleAnimated(page)
                SourceManager.SOURCE_PRODUCT_HUNT -> loadProductHunt(page)
                SourceManager.SOURCE_BEHANCE_PROJECTS -> loadBehanceProjects(page)
                else -> if (source is Source.DribbbleSearchSource) {
                    loadDribbbleSearch(source, page)
                }
            }
        }
    }

    private fun setupPageIndexes() {
        val dateSources = filterAdapter.filters
        pageIndexes = HashMap(dateSources.size)
        for (source in dateSources) {
            pageIndexes[source.key] = 0
        }
    }

    private fun getNextPageIndex(dataSource: String): Int {
        var nextPage = 1 // default to one – i.e. for newly added sources
        if (pageIndexes.containsKey(dataSource)) {
            nextPage = pageIndexes[dataSource]!!.plus(1)
        }
        pageIndexes[dataSource] = nextPage
        return nextPage
    }

    private fun sourceIsEnabled(key: String): Boolean {
        return pageIndexes[key] != 0
    }

    private fun sourceLoaded(data: List<PlaidItem>?, page: Int, key: String) {
        loadFinished()
        if (data != null && !data.isEmpty() && sourceIsEnabled(key)) {
            setPage(data, page)
            setDataSource(data, key)
            onDataLoaded(data)
        }
        inflight.remove(key)
    }

    private fun loadFailed(key: String) {
        loadFinished()
        inflight.remove(key)
    }

    private fun loadDribbblePopular(page: Int) {
        val popularCall = dribbbleApi
                .getPopular(page, DribbbleService.PER_PAGE_DEFAULT)
        popularCall.enqueue(object : Callback<List<Shot>> {
            override fun onResponse(call: Call<List<Shot>>, response: Response<List<Shot>>) {
                if (response.isSuccessful) {
                    sourceLoaded(response.body(), page, SourceManager.SOURCE_DRIBBBLE_POPULAR)
                } else {
                    loadFailed(SourceManager.SOURCE_DRIBBBLE_POPULAR)
                }
            }

            override fun onFailure(call: Call<List<Shot>>, t: Throwable) {
                loadFailed(SourceManager.SOURCE_DRIBBBLE_POPULAR)
            }
        })
        inflight[SourceManager.SOURCE_DRIBBBLE_POPULAR] = popularCall
    }

    private fun loadDribbbleDebuts(page: Int) {
        val debutsCall = dribbbleApi
                .getDebuts(page, DribbbleService.PER_PAGE_DEFAULT)
        debutsCall.enqueue(object : Callback<List<Shot>> {
            override fun onResponse(call: Call<List<Shot>>, response: Response<List<Shot>>) {
                if (response.isSuccessful) {
                    sourceLoaded(response.body(), page, SourceManager.SOURCE_DRIBBBLE_DEBUTS)
                } else {
                    loadFailed(SourceManager.SOURCE_DRIBBBLE_DEBUTS)
                }
            }

            override fun onFailure(call: Call<List<Shot>>, t: Throwable) {
                loadFailed(SourceManager.SOURCE_DRIBBBLE_DEBUTS)
            }
        })
        inflight[SourceManager.SOURCE_DRIBBBLE_DEBUTS] = debutsCall
    }

    private fun loadDribbbleAnimated(page: Int) {
        val animatedCall = dribbbleApi
                .getAnimated(page, DribbbleService.PER_PAGE_DEFAULT)
        animatedCall.enqueue(object : Callback<List<Shot>> {
            override fun onResponse(call: Call<List<Shot>>, response: Response<List<Shot>>) {
                if (response.isSuccessful) {
                    sourceLoaded(response.body(), page, SourceManager.SOURCE_DRIBBBLE_ANIMATED)
                } else {
                    loadFailed(SourceManager.SOURCE_DRIBBBLE_ANIMATED)
                }
            }

            override fun onFailure(call: Call<List<Shot>>, t: Throwable) {
                loadFailed(SourceManager.SOURCE_DRIBBBLE_ANIMATED)
            }
        })
        inflight[SourceManager.SOURCE_DRIBBBLE_ANIMATED] = animatedCall
    }

    private fun loadDribbbleRecent(page: Int) {
        val recentCall = dribbbleApi
                .getRecent(page, DribbbleService.PER_PAGE_DEFAULT)
        recentCall.enqueue(object : Callback<List<Shot>> {
            override fun onResponse(call: Call<List<Shot>>, response: Response<List<Shot>>) {
                if (response.isSuccessful) {
                    sourceLoaded(response.body(), page, SourceManager.SOURCE_DRIBBBLE_RECENT)
                } else {
                    loadFailed(SourceManager.SOURCE_DRIBBBLE_RECENT)
                }
            }

            override fun onFailure(call: Call<List<Shot>>, t: Throwable) {
                loadFailed(SourceManager.SOURCE_DRIBBBLE_RECENT)
            }
        })
        inflight[SourceManager.SOURCE_DRIBBBLE_RECENT] = recentCall
    }

    private fun loadDribbbleFollowing(page: Int) {
        val followingCall = dribbbleApi
                .getFollowing(page, DribbbleService.PER_PAGE_DEFAULT)
        followingCall.enqueue(object : Callback<List<Shot>> {
            override fun onResponse(call: Call<List<Shot>>, response: Response<List<Shot>>) {
                if (response.isSuccessful) {
                    sourceLoaded(response.body(), page, SourceManager.SOURCE_DRIBBBLE_FOLLOWING)
                } else {
                    loadFailed(SourceManager.SOURCE_DRIBBBLE_FOLLOWING)
                }
            }

            override fun onFailure(call: Call<List<Shot>>, t: Throwable) {
                loadFailed(SourceManager.SOURCE_DRIBBBLE_FOLLOWING)
            }
        })
        inflight[SourceManager.SOURCE_DRIBBBLE_FOLLOWING] = followingCall
    }

    private fun loadDribbbleUserLikes(page: Int) {
        if (dribbblePrefs.isLoggedIn) {
            val userLikesCall = dribbbleApi
                    .getUserLikes(page, DribbbleService.PER_PAGE_DEFAULT)
            userLikesCall.enqueue(object : Callback<List<Like>> {
                override fun onResponse(call: Call<List<Like>>, response: Response<List<Like>>) {
                    if (response.isSuccessful) {
                        // API returns Likes but we just want the Shots
                        val likes = response.body()
                        var likedShots: MutableList<Shot>? = null
                        if (likes != null && !likes.isEmpty()) {
                            likedShots = ArrayList(likes.size)
                            for (like in likes) {
                                if (like.shot != null) likedShots.add(like.shot)
                            }
                        }
                        sourceLoaded(likedShots, page, SourceManager.SOURCE_DRIBBBLE_USER_LIKES)
                    } else {
                        loadFailed(SourceManager.SOURCE_DRIBBBLE_USER_LIKES)
                    }
                }

                override fun onFailure(call: Call<List<Like>>, t: Throwable) {
                    loadFailed(SourceManager.SOURCE_DRIBBBLE_USER_LIKES)
                }
            })
            inflight[SourceManager.SOURCE_DRIBBBLE_USER_LIKES] = userLikesCall
        } else {
            loadFinished()
        }
    }

    private fun loadDribbbleUserShots(page: Int) {
        if (dribbblePrefs.isLoggedIn) {
            val userShotsCall = dribbbleApi
                    .getUserShots(page, DribbbleService.PER_PAGE_DEFAULT)
            userShotsCall.enqueue(object : Callback<List<Shot>> {
                override fun onResponse(call: Call<List<Shot>>, response: Response<List<Shot>>) {
                    if (response.isSuccessful) {
                        loadFinished()
                        val shots = response.body()
                        if (shots != null && !shots.isEmpty()) {
                            // this api call doesn't populate the shot user field but we need it
                            val user = dribbblePrefs.user
                            for (shot in shots) {
                                shot.user = user
                            }
                        }
                        sourceLoaded(shots, page, SourceManager.SOURCE_DRIBBBLE_USER_SHOTS)
                    } else {
                        loadFailed(SourceManager.SOURCE_DRIBBBLE_USER_SHOTS)
                    }
                }

                override fun onFailure(call: Call<List<Shot>>, t: Throwable) {
                    loadFailed(SourceManager.SOURCE_DRIBBBLE_USER_SHOTS)
                }
            })
            inflight[SourceManager.SOURCE_DRIBBBLE_USER_SHOTS] = userShotsCall
        } else {
            loadFinished()
        }
    }


    private fun loadDribbbleSearch(source: Source.DribbbleSearchSource, page: Int) {
        val searchCall = getDribbbleSearchApi()!!.search(source.query, page,
                DribbbleSearchService.PER_PAGE_DEFAULT, DribbbleSearchService.SORT_RECENT)
        searchCall.enqueue(object : Callback<List<Shot>> {
            override fun onResponse(call: Call<List<Shot>>, response: Response<List<Shot>>) {
                if (response.isSuccessful) {
                    sourceLoaded(response.body(), page, source.key)
                } else {
                    loadFailed(source.key)
                }
            }

            override fun onFailure(call: Call<List<Shot>>, t: Throwable) {
                loadFailed(source.key)
            }
        })
        inflight[source.key] = searchCall
    }

    private fun loadProductHunt(page: Int) {
        // this API's paging is 0 based but this class (& sorting) is 1 based so adjust locally
        val postsCall = getProductHuntApi()!!.getPosts(page - 1)
        postsCall.enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (response.isSuccessful) {
                    sourceLoaded(response.body(), page, SourceManager.SOURCE_PRODUCT_HUNT)
                } else {
                    loadFailed(SourceManager.SOURCE_PRODUCT_HUNT)
                }
            }

            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                loadFailed(SourceManager.SOURCE_PRODUCT_HUNT)
            }
        })
        inflight[SourceManager.SOURCE_PRODUCT_HUNT] = postsCall
    }

    private fun loadBehanceProjects(page: Int) {
        //todo: load behance projects here
    }

}
