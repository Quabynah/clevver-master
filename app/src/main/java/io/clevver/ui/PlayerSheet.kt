/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.IntDef
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.text.format.DateUtils
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import io.clevver.R
import io.clevver.data.DataLoadingSubject
import io.clevver.data.PaginatedDataManager
import io.clevver.data.api.dribbble.FollowersDataManager
import io.clevver.data.api.dribbble.ShotLikesDataManager
import io.clevver.data.api.dribbble.model.*
import io.clevver.ui.recyclerview.InfiniteScrollListener
import io.clevver.ui.recyclerview.SlideInItemAnimator
import io.clevver.ui.widget.BottomSheet
import io.clevver.util.AnimUtils.getLinearOutSlowInInterpolator
import io.clevver.util.DribbbleUtils
import io.clevver.util.bindView
import io.clevver.util.glide.GlideApp
import java.text.NumberFormat
import java.util.*

/**
 * Shows likes or followers of a particular dribbble player
 */
class PlayerSheet : Activity() {

    private val bottomSheet: BottomSheet by bindView(R.id.bottom_sheet)
    private val content: ViewGroup by bindView(R.id.bottom_sheet_content)
    private val titleBar: ViewGroup by bindView(R.id.title_bar)
    private val close: ImageView by bindView(R.id.close)
    private val title: TextView by bindView(R.id.title)
    private val playerList: RecyclerView by bindView(R.id.player_list)
    private var largeAvatarSize: Int = 0
    private var shot: Shot? = null
    private var player: User? = null
    private var dataManager: PaginatedDataManager<*>? = null
    private var layoutManager: LinearLayoutManager? = null
    private var dismissState = DISMISS_DOWN

    private val titleElevation = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            val raiseTitleBar = dy > 0 || playerList.computeVerticalScrollOffset() != 0
            titleBar.isActivated = raiseTitleBar // animated via a StateListAnimator
        }
    }

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(MODE_FOLLOWERS, MODE_SHOT_LIKES)
    internal annotation class PlayerSheetMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.player_sheet)

        largeAvatarSize = resources.getDimensionPixelSize(R.dimen.large_avatar_size)

        val intent = intent
        @PlayerSheetMode val mode = intent.getIntExtra(EXTRA_MODE, -1)
        when (mode) {
            MODE_SHOT_LIKES -> {
                shot = intent.getParcelableExtra(EXTRA_SHOT)
                title.text = resources.getQuantityString(
                        R.plurals.fans,
                        shot!!.likes_count.toInt(),
                        NumberFormat.getInstance().format(shot!!.likes_count))
                val adapter = PlayerAdapter<Like>(this)
                dataManager = object : ShotLikesDataManager(this@PlayerSheet, shot!!.id) {
                    override fun onDataLoaded(likes: List<Like>) {
                        adapter.addItems(likes)
                    }
                }
                layoutManager = LinearLayoutManager(this)
                playerList.layoutManager = layoutManager
                playerList.itemAnimator = SlideInItemAnimator()
                dataManager!!.registerCallback(adapter)
                playerList.adapter = adapter
                playerList.addOnScrollListener(object : InfiniteScrollListener(layoutManager!!, dataManager!!) {
                    override fun onLoadMore() {
                        dataManager!!.loadData()
                    }
                })
                playerList.addOnScrollListener(titleElevation)
                dataManager!!.loadData() // kick off initial load
            }
            MODE_FOLLOWERS -> {
                player = intent.getParcelableExtra(EXTRA_USER)
                title.text = resources.getQuantityString(
                        R.plurals.follower_count,
                        player!!.followers_count,
                        NumberFormat.getInstance().format(player!!.followers_count.toLong()))
                val adapter = PlayerAdapter<Follow>(this)
                dataManager = object : FollowersDataManager(this@PlayerSheet, player!!.id) {
                    override fun onDataLoaded(followers: List<Follow>) {
                        adapter.addItems(followers)
                    }
                }
                layoutManager = LinearLayoutManager(this)
                playerList.layoutManager = layoutManager
                playerList.itemAnimator = SlideInItemAnimator()
                dataManager!!.registerCallback(adapter)
                playerList.adapter = adapter
                playerList.addOnScrollListener(object : InfiniteScrollListener(layoutManager!!, dataManager!!) {
                    override fun onLoadMore() {
                        dataManager!!.loadData()
                    }
                })
                playerList.addOnScrollListener(titleElevation)
                dataManager!!.loadData() // kick off initial load
            }
            else -> throw IllegalArgumentException("Unknown launch mode.")
        }

        bottomSheet.registerCallback(object : BottomSheet.Callbacks() {
            override fun onSheetDismissed() {
                finishAfterTransition()
            }

            override fun onSheetPositionChanged(sheetTop: Int, interacted: Boolean) {
                if (interacted && close.visibility != View.VISIBLE) {
                    close.visibility = View.VISIBLE
                    close.alpha = 0f
                    close.animate()
                            .alpha(1f)
                            .setDuration(400L)
                            .setInterpolator(getLinearOutSlowInInterpolator(this@PlayerSheet))
                            .start()
                }
                if (sheetTop == 0) {
                    showClose()
                } else {
                    showDown()
                }
            }
        })

        close.setOnClickListener({ dismiss(close) })
        bottomSheet.setOnClickListener({ dismiss(bottomSheet) })

    }

    override fun onDestroy() {
        dataManager!!.cancelLoading()
        super.onDestroy()
    }

    private fun showClose() {
        if (dismissState == DISMISS_CLOSE) return
        dismissState = DISMISS_CLOSE
        close.setImageState(intArrayOf(android.R.attr.state_expanded), true)
    }

    private fun showDown() {
        if (dismissState == DISMISS_DOWN) return
        dismissState = DISMISS_DOWN
        close.setImageState(intArrayOf(-android.R.attr.state_expanded), true)
    }

    fun dismiss(view: View) {
        if (view.visibility != View.VISIBLE) return
        bottomSheet.dismiss()
    }

    private inner class PlayerAdapter<T : PlayerListable> internal constructor(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DataLoadingSubject.DataLoadingCallbacks {
        private var loading = true
        private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
        internal var items: MutableList<T> = ArrayList(0)

        internal val dataItemCount: Int
            get() = items.size

        private val loadingMoreItemPosition: Int
            get() = if (loading) itemCount - 1 else RecyclerView.NO_POSITION

        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            when (viewType) {
                TYPE_PLAYER -> return createPlayerViewHolder(parent)
                else // TYPE_LOADING
                -> return LoadingViewHolder(
                        layoutInflater.inflate(R.layout.list_loading, parent, false))
            }
        }

        private fun createPlayerViewHolder(parent: ViewGroup): PlayerViewHolder {
            val holder = PlayerViewHolder(
                    layoutInflater.inflate(R.layout.player_item, parent, false))
            holder.itemView.setOnClickListener { _ ->
                val user = items[holder.adapterPosition].player
                val player = Intent(this@PlayerSheet, PlayerActivity::class.java)
                player.putExtra(PlayerActivity.EXTRA_PLAYER, user)
                val options = ActivityOptions.makeSceneTransitionAnimation(this@PlayerSheet,
                        Pair.create(holder.playerAvatar as View?,
                                getString(R.string.transition_player_avatar)),
                        Pair.create(holder.itemView,
                                getString(R.string.transition_player_background)))
                startActivity(player, options.toBundle())
            }
            return holder
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (position == loadingMoreItemPosition) return
            bindPlayer(holder as PlayerViewHolder, items[position])
        }

        private fun bindPlayer(holder: PlayerViewHolder, player: T) {
            GlideApp.with(holder.itemView.context)
                    .load(player.player.highQualityAvatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.avatar_placeholder)
                    .override(largeAvatarSize, largeAvatarSize)
                    .transition(withCrossFade())
                    .into(holder.playerAvatar!!)
            holder.playerName!!.text = player.player.name.toLowerCase()
            if (!TextUtils.isEmpty(player.player.bio)) {
                DribbbleUtils.parseAndSetText(holder.playerBio, player.player.bio)
            } else if (!TextUtils.isEmpty(player.player.location)) {
                holder.playerBio!!.text = player.player.location
            }
            holder.timeAgo!!.text = DateUtils.getRelativeTimeSpanString(player.dateCreated.time,
                    System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)
                    .toString().toLowerCase()
        }

        override fun getItemViewType(position: Int): Int {
            return if (position < dataItemCount && dataItemCount > 0) {
                TYPE_PLAYER
            } else TYPE_LOADING
        }

        override fun getItemId(position: Int): Long {
            return if (getItemViewType(position) == TYPE_LOADING) {
                -1L
            } else items[position].id
        }

        override fun getItemCount(): Int {
            return dataItemCount + if (loading) 1 else 0
        }

        override fun dataStartedLoading() {
            if (loading) return
            loading = true
            notifyItemInserted(loadingMoreItemPosition)
        }

        override fun dataFinishedLoading() {
            if (!loading) return
            val loadingPos = loadingMoreItemPosition
            loading = false
            notifyItemRemoved(loadingPos)
        }

        internal fun addItems(newItems: List<T>) {
            val insertRangeStart = dataItemCount
            items.addAll(newItems)
            notifyItemRangeInserted(insertRangeStart, newItems.size)
        }

    }

    internal class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var playerAvatar: ImageView? = null
        var playerName: TextView? = null
        var playerBio: TextView? = null
        var timeAgo: TextView? = null

        init {
            playerAvatar = itemView.findViewById(R.id.player_avatar)
            playerName = itemView.findViewById(R.id.player_name)
            playerBio = itemView.findViewById(R.id.player_bio)
            timeAgo = itemView.findViewById(R.id.time_ago)
        }
    }

    internal class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var progress: ProgressBar = itemView as ProgressBar

    }

    companion object {

        private const val TYPE_PLAYER = 7
        private const val TYPE_LOADING = -1
        private const val MODE_SHOT_LIKES = 1
        private const val MODE_FOLLOWERS = 2
        private const val DISMISS_DOWN = 0
        private const val DISMISS_CLOSE = 1
        private const val EXTRA_MODE = "EXTRA_MODE"
        private const val EXTRA_SHOT = "EXTRA_SHOT"
        private const val EXTRA_USER = "EXTRA_USER"

        fun start(launching: Activity, shot: Shot) {
            val starter = Intent(launching, PlayerSheet::class.java)
            starter.putExtra(EXTRA_MODE, MODE_SHOT_LIKES)
            starter.putExtra(EXTRA_SHOT, shot)
            launching.startActivity(starter,
                    ActivityOptions.makeSceneTransitionAnimation(launching).toBundle())
        }

        fun start(launching: Activity, player: User) {
            val starter = Intent(launching, PlayerSheet::class.java)
            starter.putExtra(EXTRA_MODE, MODE_FOLLOWERS)
            starter.putExtra(EXTRA_USER, player)
            launching.startActivity(starter,
                    ActivityOptions.makeSceneTransitionAnimation(launching).toBundle())
        }
    }
}
