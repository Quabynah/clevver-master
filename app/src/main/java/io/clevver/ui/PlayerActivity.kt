/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.*
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.util.ViewPreloadSizeProvider
import io.clevver.ui.FeedAdapter
import io.clevver.R
import io.clevver.data.api.dribbble.PlayerShotsDataManager
import io.clevver.data.api.dribbble.model.Shot
import io.clevver.data.api.dribbble.model.User
import io.clevver.data.prefs.DribbblePrefs
import io.clevver.ui.recyclerview.InfiniteScrollListener
import io.clevver.ui.recyclerview.SlideInItemAnimator
import io.clevver.ui.transitions.MorphTransform
import io.clevver.ui.widget.ElasticDragDismissFrameLayout
import io.clevver.ui.widget.ElasticDragDismissFrameLayout.SystemChromeFader
import io.clevver.util.DribbbleUtils
import io.clevver.util.ViewUtils
import io.clevver.util.bindView
import io.clevver.util.glide.GlideApp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat

/**
 * A screen displaying a player's details and their shots.
 */
class PlayerActivity : Activity() {

    private var player: User? = null
    private var dataManager: PlayerShotsDataManager? = null
    private var adapter: FeedAdapter? = null
    private var layoutManager: GridLayoutManager? = null
    internal var following: Boolean? = null
    private var chromeFader: ElasticDragDismissFrameLayout.SystemChromeFader? = null
    private var followerCount: Int = 0

    private val draggableFrame: ElasticDragDismissFrameLayout by bindView(R.id.draggable_frame)
    private val container: ViewGroup by bindView(R.id.container)
    private val avatar: ImageView by bindView(R.id.avatar)
    private val playerName: TextView by bindView(R.id.player_name)
    private val follow: Button by bindView(R.id.follow)
    private val bio: TextView by bindView(R.id.player_bio)
    private val shotCount: TextView by bindView(R.id.shot_count)
    private val followersCount: TextView by bindView(R.id.followers_count)
    private val likesCount: TextView by bindView(R.id.likes_count)
    private val loading: ProgressBar by bindView(R.id.loading)
    private val shots: RecyclerView by bindView(R.id.player_shots)
    private var columns: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dribbble_player)
        chromeFader = SystemChromeFader(this)

        columns = resources.getInteger(R.integer.num_columns)

        val intent = intent
        if (intent.hasExtra(EXTRA_PLAYER)) {
            player = intent.getParcelableExtra(EXTRA_PLAYER)
            if (player != null) {
                bindPlayer()
            } else {
                Snackbar.make(container, "No Player found", Snackbar.LENGTH_LONG).show()
            }
        } else if (intent.hasExtra(EXTRA_PLAYER_NAME)) {
            val name = intent.getStringExtra(EXTRA_PLAYER_NAME)
            if (!name.isNullOrEmpty()) {
                playerName.text = name
                if (intent.hasExtra(EXTRA_PLAYER_ID)) {
                    val userId = intent.getLongExtra(EXTRA_PLAYER_ID, 0L)
                    loadPlayer(userId)
                } else if (intent.hasExtra(EXTRA_PLAYER_USERNAME)) {
                    val username = intent.getStringExtra(EXTRA_PLAYER_USERNAME)
                    loadPlayer(username)
                }
            } else {
                Snackbar.make(container, "No Player name found", Snackbar.LENGTH_LONG).show()
            }
        } else if (intent.data != null) {
            // todo support url intents
        }

        // setup immersive mode i.e. draw behind the system chrome & adjust insets
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            draggableFrame.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        } else {
            draggableFrame.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
        draggableFrame.setOnApplyWindowInsetsListener { _, insets ->
            val lpFrame = draggableFrame.layoutParams as MarginLayoutParams
            lpFrame.leftMargin += insets.systemWindowInsetLeft    // landscape
            lpFrame.rightMargin += insets.systemWindowInsetRight  // landscape
            (avatar.layoutParams as MarginLayoutParams).topMargin += insets.systemWindowInsetTop
            ViewUtils.setPaddingTop(container, insets.systemWindowInsetTop)
            ViewUtils.setPaddingBottom(shots, insets.systemWindowInsetBottom)
            // clear this listener so insets aren't re-applied
            draggableFrame.setOnApplyWindowInsetsListener(null)
            insets
        }
        setExitSharedElementCallback(FeedAdapter.createSharedElementReenterCallback(this))

        shotCount.setOnClickListener({ playerActionClick(shotCount) })
        likesCount.setOnClickListener({ playerActionClick(likesCount) })
        followersCount.setOnClickListener({ playerActionClick(followersCount) })
    }

    override fun onResume() {
        super.onResume()
        draggableFrame.addListener(chromeFader)
    }

    override fun onPause() {
        draggableFrame.removeListener(chromeFader)
        super.onPause()
    }

    override fun onDestroy() {
        dataManager?.cancelLoading()
        super.onDestroy()
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        if (data == null || resultCode != Activity.RESULT_OK
                || !data.hasExtra(DribbbleShot.RESULT_EXTRA_SHOT_ID))
            return

        // When reentering, if the shared element is no longer on screen (e.g. after an
        // orientation change) then scroll it into view.
        val sharedShotId = data.getLongExtra(DribbbleShot.RESULT_EXTRA_SHOT_ID, -1L)
        if (sharedShotId != -1L                                             // returning from a shot

                && adapter?.dataItemCount!! > 0                           // grid populated

                && shots.findViewHolderForItemId(sharedShotId) == null) {   // view not attached
            val position = adapter?.getItemPosition(sharedShotId)
            if (position == RecyclerView.NO_POSITION) return

            // delay the transition until our shared element is on-screen i.e. has been laid out
            postponeEnterTransition()
            shots.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(v: View, l: Int, t: Int, r: Int, b: Int,
                                            oL: Int, oT: Int, oR: Int, oB: Int) {
                    shots.removeOnLayoutChangeListener(this)
                    startPostponedEnterTransition()
                }
            })
            if (position != null) {
                shots.scrollToPosition(position)
            }
        }
    }

    internal fun bindPlayer() {
        if (player == null) return

        val res = resources
        val nf = NumberFormat.getInstance()

        GlideApp.with(this)
                .load(player?.highQualityAvatarUrl)
                .placeholder(R.drawable.avatar_placeholder)
                .circleCrop()
                .transition(withCrossFade())
                .into(avatar)
        playerName.text = player?.name?.toLowerCase()
        if (!player?.bio.isNullOrEmpty()) {
            DribbbleUtils.parseAndSetText(bio, player?.bio)
        } else {
            bio.visibility = View.GONE
        }

        shotCount.text = res.getQuantityString(R.plurals.shots, player!!.shots_count,
                nf.format(player!!.shots_count.toLong()))
        if (player?.shots_count == 0) {
            shotCount.setCompoundDrawablesRelativeWithIntrinsicBounds(null, getDrawable(R.drawable.avd_no_shots), null, null)
        }
        setFollowerCount(player?.followers_count)
        likesCount.text = res.getQuantityString(R.plurals.likes, player!!.likes_count,
                nf.format(player!!.likes_count.toLong()))

        // load the users shots
        if (player != null && player?.id != null && player?.id!! > -1L) {
            dataManager = object : PlayerShotsDataManager(this@PlayerActivity, player!!) {
                override fun onDataLoaded(data: List<Shot>?) {
                    if (data != null && data.isNotEmpty()) {
                        if (adapter?.dataItemCount!! == 0) {
                            loading.visibility = View.GONE
                            ViewUtils.setPaddingTop(shots, likesCount.bottom)
                        }
                        adapter?.addAndResort(data)
                    }
                }
            }
        }
        val shotPreloadSizeProvider = ViewPreloadSizeProvider<Shot>()
        adapter = FeedAdapter(this, dataManager, columns,
                shotPreloadSizeProvider)
        shots.adapter = adapter
        shots.itemAnimator = SlideInItemAnimator()
        shots.visibility = View.VISIBLE
        layoutManager = GridLayoutManager(this, columns)
        layoutManager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return adapter?.getItemColumnSpan(position)!!
            }
        }
        shots.layoutManager = layoutManager
        shots.addOnScrollListener(object : InfiniteScrollListener(layoutManager!!, dataManager!!) {
            override fun onLoadMore() {
                dataManager?.loadData()
            }
        })
        shots.setHasFixedSize(true)
        val shotPreloader = RecyclerViewPreloader(this, adapter!!, shotPreloadSizeProvider, 4)
        shots.addOnScrollListener(shotPreloader)

        // forward on any clicks above the first item in the grid (i.e. in the paddingTop)
        // to 'pass through' to the view behind
        shots.setOnTouchListener { _, event ->
            val firstVisible = layoutManager!!.findFirstVisibleItemPosition()
            if (firstVisible > 0) return@setOnTouchListener false

            // if no data loaded then pass through
            if (adapter!!.dataItemCount == 0) {
                return@setOnTouchListener false
            } else {
                container.dispatchTouchEvent(event)
            }

            val vh = shots.findViewHolderForAdapterPosition(0) ?: return@setOnTouchListener false
            val firstTop = vh!!.itemView.top
            if (event.y < firstTop) {
                return@setOnTouchListener container!!.dispatchTouchEvent(event)
            }
            false
        }

        // check if following
        if (dataManager?.dribbblePrefs!!.isLoggedIn) {
            if (player!!.id == dataManager!!.dribbblePrefs.userId) {
                TransitionManager.beginDelayedTransition(container)
                follow.visibility = View.GONE
                ViewUtils.setPaddingTop(shots, container.height - follow.height
                        - (follow.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin)
            } else {
                val followingCall = dataManager?.dribbbleApi?.following(player!!.id)
                followingCall?.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        following = response.isSuccessful
                        if (following != null && following!!) return
                        TransitionManager.beginDelayedTransition(container)
                        follow.setText(R.string.following)
                        follow.isActivated = true
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {}
                })
            }
        }

        follow.setOnClickListener({ if (player != null) follow() })

        if (player?.shots_count != null && player?.shots_count!! > 0) {
            dataManager?.loadData() // kick off initial load
        } else {
            loading.visibility = View.GONE
        }
    }

    private fun follow() {
        if (DribbblePrefs[this].isLoggedIn) {
            if (following != null && following!!) {
                val unfollowCall = dataManager?.dribbbleApi?.unfollow(player!!.id)
                unfollowCall?.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {}

                    override fun onFailure(call: Call<Void>, t: Throwable) {}
                })
                following = false
                TransitionManager.beginDelayedTransition(container)
                follow.setText(R.string.follow)
                follow.isActivated = false
                setFollowerCount(followerCount - 1)
            } else {
                val followCall = dataManager?.dribbbleApi?.follow(player!!.id)
                followCall?.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {}

                    override fun onFailure(call: Call<Void>, t: Throwable) {}
                })
                following = true
                TransitionManager.beginDelayedTransition(container)
                follow.setText(R.string.following)
                follow.isActivated = true
                setFollowerCount(followerCount + 1)
            }
        } else {
            val login = Intent(this, DribbbleLogin::class.java)
            MorphTransform.addExtras(login,
                    ContextCompat.getColor(this, R.color.dribbble),
                    resources.getDimensionPixelSize(R.dimen.dialog_corners))
            val options = ActivityOptions.makeSceneTransitionAnimation(this, follow, getString(R.string.transition_dribbble_login))
            startActivity(login, options.toBundle())
        }
    }

    private fun playerActionClick(view: TextView) {
        (view.compoundDrawables[1] as AnimatedVectorDrawable).start()
        if (player != null) {
            when (view.id) {
                R.id.followers_count -> PlayerSheet.start(this@PlayerActivity, player!!)
            }
        } else {
            Toast.makeText(applicationContext, "User not loaded", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPlayer(userId: Long) {
        val userCall = DribbblePrefs[this].getApi().getUser(userId)
        userCall.enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                player = response.body()
                bindPlayer()
            }

            override fun onFailure(call: Call<User>, t: Throwable) {}
        })
    }

    private fun loadPlayer(username: String) {
        val userCall = DribbblePrefs[this].getApi().getUser(username)
        userCall.enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                player = response.body()
                bindPlayer()
            }

            override fun onFailure(call: Call<User>, t: Throwable) {}
        })
    }

    private fun setFollowerCount(count: Int?) {
        if (count != null) {
            followerCount = count
            followersCount.text = resources.getQuantityString(R.plurals.follower_count,
                    followerCount, NumberFormat.getInstance().format(followerCount.toLong()))
            if (followerCount == 0) {
                followersCount.background = null
            }
        }
    }

    companion object {
        const val EXTRA_PLAYER = "EXTRA_PLAYER"
        const val EXTRA_PLAYER_NAME = "EXTRA_PLAYER_NAME"
        const val EXTRA_PLAYER_ID = "EXTRA_PLAYER_ID"
        const val EXTRA_PLAYER_USERNAME = "EXTRA_PLAYER_USERNAME"
    }

}
