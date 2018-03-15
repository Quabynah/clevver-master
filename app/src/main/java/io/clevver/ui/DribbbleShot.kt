/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.graphics.Palette
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.text.format.DateUtils
import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionManager
import android.util.Pair
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.clevver.R
import io.clevver.data.api.dribbble.DribbbleService
import io.clevver.data.api.dribbble.model.Comment
import io.clevver.data.api.dribbble.model.Like
import io.clevver.data.api.dribbble.model.Shot
import io.clevver.data.prefs.DribbblePrefs
import io.clevver.ui.recyclerview.Divided
import io.clevver.ui.recyclerview.InsetDividerDecoration
import io.clevver.ui.recyclerview.SlideInItemAnimator
import io.clevver.ui.transitions.FabTransform
import io.clevver.ui.widget.*
import io.clevver.util.bindView
import io.clevver.util.*
import io.clevver.util.AnimUtils.getFastOutSlowInInterpolator
import io.clevver.util.glide.GlideApp
import io.clevver.util.glide.getBitmap
import okhttp3.HttpUrl
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.*

@SuppressLint("GoogleAppIndexingApiWarning")
/**
 * Dribbble Shot screen
 */
class DribbbleShot : Activity() {

    private val draggableFrame: ElasticDragDismissFrameLayout by bindView(R.id.draggable_frame)
    private val back: ImageButton by bindView(R.id.back)
    private val imageView: ParallaxScrimageView by bindView(R.id.shot)
    private val commentsList: RecyclerView by bindView(R.id.dribbble_comments)
    private val fab: FABToggle by bindView(R.id.fab_heart)
    internal var shotDescription: View? = null
    internal var shotSpacer: View? = null
    private var likeCount: Button? = null
    private var viewCount: Button? = null
    private var share: Button? = null
    private var playerAvatar: ImageView? = null
    private var enterComment: EditText? = null
    private var postComment: ImageButton? = null
    private var title: View? = null
    private var description: View? = null
    private var playerName: TextView? = null
    private var shotTimeAgo: TextView? = null
    private var commentFooter: View? = null
    private var userAvatar: ImageView? = null
    private var chromeFader: ElasticDragDismissFrameLayout.SystemChromeFader? = null

    internal var shot: Shot? = null
    internal var fabOffset: Int = 0
    private lateinit var dribbblePrefs: DribbblePrefs
    internal var performingLike: Boolean = false
    internal var allowComment: Boolean = false
    private lateinit var adapter: CommentsAdapter
    private lateinit var commentAnimator: CommentAnimator
    internal var largeAvatarSize: Int = 0
    private var cardElevation: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dribbble_shot)
        dribbblePrefs = DribbblePrefs.get(this)

        cardElevation = resources.getDimensionPixelSize(R.dimen.z_card)

        shotDescription = layoutInflater.inflate(R.layout.dribbble_shot_description,
                commentsList, false)
        shotSpacer = shotDescription?.findViewById(R.id.shot_spacer)
        title = shotDescription?.findViewById(R.id.shot_title)
        description = shotDescription?.findViewById(R.id.shot_description)
        likeCount = shotDescription?.findViewById(R.id.shot_like_count)
        viewCount = shotDescription?.findViewById(R.id.shot_view_count)
        share = shotDescription?.findViewById(R.id.shot_share_action)
        playerName = shotDescription?.findViewById(R.id.player_name)
        playerAvatar = shotDescription?.findViewById(R.id.player_avatar)
        shotTimeAgo = shotDescription?.findViewById(R.id.shot_time_ago)

        setupCommenting()
        commentsList.addOnScrollListener(scrollListener)
        commentsList.onFlingListener = flingListener
        back.setOnClickListener { _ -> setResultAndFinish() }
        fab.setOnClickListener(fabClick)
        chromeFader = object : ElasticDragDismissFrameLayout.SystemChromeFader(this) {
            override fun onDragDismissed() {
                setResultAndFinish()
            }
        }

        postComment?.setOnClickListener({ postComment() })

        val intent = intent
        if (intent.hasExtra(EXTRA_SHOT)) {
            shot = intent.getParcelableExtra(EXTRA_SHOT)
            bindShot(true)
        } else if (intent.data != null) {
            val url = HttpUrl.parse(intent.dataString!!)
            if (url!!.pathSize() == 2 && url.pathSegments()[0] == "shots") {
                try {
                    val shotPath = url.pathSegments()[1]
                    val id = java.lang.Long.parseLong(shotPath.substring(0, shotPath.indexOf("-")))

                    val shotCall = dribbblePrefs.getApi().getShot(id)
                    shotCall.enqueue(object : Callback<Shot> {
                        override fun onResponse(call: Call<Shot>, response: Response<Shot>) {
                            shot = response.body()
                            bindShot(false)
                        }

                        override fun onFailure(call: Call<Shot>, t: Throwable) {
                            reportUrlError()
                        }
                    })
                } catch (ex: NumberFormatException) {
                    reportUrlError()
                } catch (ex: StringIndexOutOfBoundsException) {
                    reportUrlError()
                }

            } else {
                reportUrlError()
            }
        }
    }

    /**
     * We run a transition to expand/collapse comments. Scrolling the RecyclerView while this is
     * running causes issues, so we consume touch events while the transition runs.
     */
    internal var touchEater: View.OnTouchListener = View.OnTouchListener { _, _ -> true }

    private val shotLoadListener = object : RequestListener<Drawable> {
        override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>,
                                     dataSource: DataSource, isFirstResource: Boolean): Boolean {
            val bitmap = resource.getBitmap() ?: return false
            val twentyFourDip = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    24f, this@DribbbleShot.resources.displayMetrics).toInt()
            Palette.from(bitmap)
                    .maximumColorCount(3)
                    .clearFilters() /* by default palette ignore certain hues
                        (e.g. pure black/white) but we don't want this. */
                    .setRegion(0, 0, bitmap.width - 1, twentyFourDip) /* - 1 to work around
                        https://code.google.com/p/android/issues/detail?id=191013 */
                    .generate { palette ->
                        val isDark: Boolean
                        @ColorUtils.Lightness val lightness = ColorUtils.isDark(palette)
                        isDark = if (lightness == ColorUtils.LIGHTNESS_UNKNOWN) {
                            ColorUtils.isDark(bitmap, bitmap.width / 2, 0)
                        } else {
                            lightness == ColorUtils.IS_DARK
                        }

                        if (!isDark) { // make back icon dark on light images
                            back.setColorFilter(ContextCompat.getColor(
                                    this@DribbbleShot, R.color.dark_icon))
                        }

                        // color the status bar. Set a complementary dark color on L,
                        // light or dark color on M (with matching status bar icons)
                        var statusBarColor = window.statusBarColor
                        val topColor = ColorUtils.getMostPopulousSwatch(palette)
                        if (topColor != null && (isDark || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
                            statusBarColor = ColorUtils.scrimify(topColor.rgb,
                                    isDark, SCRIM_ADJUSTMENT)
                            // set a light status bar on M+
                            if (!isDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                ViewUtils.setLightStatusBar(imageView)
                            }
                        }

                        if (statusBarColor != window.statusBarColor) {
                            imageView.setScrimColor(statusBarColor)
                            val statusBarColorAnim = ValueAnimator.ofArgb(
                                    window.statusBarColor, statusBarColor)
                            statusBarColorAnim.addUpdateListener { animation ->
                                window.statusBarColor = animation.animatedValue as Int
                            }
                            statusBarColorAnim.duration = 1000L
                            statusBarColorAnim.interpolator = getFastOutSlowInInterpolator(this@DribbbleShot)
                            statusBarColorAnim.start()
                        }
                    }

            Palette.from(bitmap)
                    .clearFilters()
                    .generate { palette ->
                        // color the ripple on the image spacer (default is grey)
                        shotSpacer?.background = ViewUtils.createRipple(palette, 0.25f, 0.5f,
                                ContextCompat.getColor(this@DribbbleShot, R.color.mid_grey),
                                true)
                        // slightly more opaque ripple on the pinned image to compensate
                        // for the scrim
                        imageView.foreground = ViewUtils.createRipple(palette, 0.3f, 0.6f,
                                ContextCompat.getColor(this@DribbbleShot, R.color.mid_grey),
                                true)
                    }

            imageView.background = null
            return false
        }

        override fun onLoadFailed(e: GlideException?, model: Any,
                                  target: Target<Drawable>, isFirstResource: Boolean): Boolean {
            return false
        }
    }

    private val enterCommentFocus = View.OnFocusChangeListener { _, hasFocus ->
        // kick off an anim (via animated state list) on the post button. see
        // @drawable/ic_add_comment
        postComment?.isActivated = hasFocus

        // prevent content hovering over image when not pinned.
        if (hasFocus) {
            imageView.bringToFront()
            imageView.offset = -imageView.height
            imageView.isImmediatePin = true
        }
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            val scrollY = shotDescription!!.top
            imageView.offset = scrollY
            fab.setOffset(fabOffset + scrollY)
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
            // as we animate the main image's elevation change when it 'pins' at it's min height
            // a fling can cause the title to go over the image before the animation has a chance to
            // run. In this case we short circuit the animation and just jump to state.
            imageView.isImmediatePin = newState == RecyclerView.SCROLL_STATE_SETTLING
        }
    }

    private val flingListener = object : RecyclerView.OnFlingListener() {
        override fun onFling(velocityX: Int, velocityY: Int): Boolean {
            imageView.isImmediatePin = true
            return false
        }
    }

    private val fabClick = View.OnClickListener {
        if (dribbblePrefs.isLoggedIn) {
            fab.toggle()
            doLike()
        } else {
            val login = Intent(this@DribbbleShot, DribbbleLogin::class.java)
            FabTransform.addExtras(login, ContextCompat.getColor(this@DribbbleShot, R
                    .color.dribbble), R.drawable.ic_heart_empty_56dp)
            val options = ActivityOptions.makeSceneTransitionAnimation(this@DribbbleShot, fab, getString(R.string.transition_dribbble_login))
            startActivityForResult(login, RC_LOGIN_LIKE, options.toBundle())
        }
    }

    private val shotClick = View.OnClickListener { _ ->
        val intent = Intent(this@DribbbleShot, ImageDetailsActivity::class.java)
        intent.putExtra(ImageDetailsActivity.EXTRA_IMAGE_URL, shot)
        val options = ActivityOptions.makeSceneTransitionAnimation(this@DribbbleShot,
                android.util.Pair.create(shotSpacer, getString(R.string.transition_shot)))
        startActivity(intent, options.toBundle())
    }

    override fun onResume() {
        super.onResume()
        if (!performingLike) {
            checkLiked()
        }
        draggableFrame.addListener(chromeFader)
    }

    override fun onPause() {
        draggableFrame.removeListener(chromeFader)
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_LOGIN_LIKE -> if (resultCode == Activity.RESULT_OK) {
                fab.isChecked = true
                doLike()
                setupCommenting()
            }
            RC_LOGIN_COMMENT -> if (resultCode == Activity.RESULT_OK) {
                setupCommenting()
            }
        }
    }

    override fun onBackPressed() {
        setResultAndFinish()
    }

    override fun onNavigateUp(): Boolean {
        setResultAndFinish()
        return true
    }

    private fun postComment() {
        if (dribbblePrefs.isLoggedIn) {
            if (TextUtils.isEmpty(enterComment!!.text)) return
            enterComment!!.isEnabled = false
            val postCommentCall = dribbblePrefs.getApi().postComment(
                    shot!!.id, enterComment!!.text.toString().trim { it <= ' ' })
            postCommentCall.enqueue(object : Callback<Comment> {
                override fun onResponse(call: Call<Comment>, response: Response<Comment>) {
                    loadComments()
                    enterComment!!.text.clear()
                    enterComment!!.isEnabled = true
                }

                override fun onFailure(call: Call<Comment>, t: Throwable) {
                    enterComment!!.isEnabled = true
                }
            })
        } else {
            val login = Intent(this@DribbbleShot, DribbbleLogin::class.java)
            FabTransform.addExtras(login, ContextCompat.getColor(
                    this@DribbbleShot, R.color.background_light), R.drawable.ic_comment_add)
            val options = ActivityOptions.makeSceneTransitionAnimation(
                    this@DribbbleShot, postComment, getString(R.string.transition_dribbble_login))
            startActivityForResult(login, RC_LOGIN_COMMENT, options.toBundle())
        }
    }

    internal fun bindShot(postponeEnterTransition: Boolean) {
        val res = resources

        // load the main image
        val imageSize = shot?.images!!.bestSize()
        GlideApp.with(this)
                .load(shot!!.images.best())
                .listener(shotLoadListener)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .priority(Priority.IMMEDIATE)
                .override(imageSize[0], imageSize[1])
                .transition(withCrossFade())
                .into(imageView)
        imageView.setOnClickListener(shotClick)
        shotSpacer?.setOnClickListener(shotClick)

        if (postponeEnterTransition) postponeEnterTransition()
        imageView.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        imageView.viewTreeObserver.removeOnPreDrawListener(this)
                        calculateFabPosition()
                        if (postponeEnterTransition) startPostponedEnterTransition()
                        return true
                    }
                })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (title as FabOverlapTextView).setText(shot!!.title)
        } else {
            (title as TextView).text = shot!!.title
        }
        if (!TextUtils.isEmpty(shot!!.description)) {
            val descText = shot!!.getParsedDescription(
                    ContextCompat.getColorStateList(this, R.color.dribbble_links),
                    ContextCompat.getColor(this, R.color.dribbble_link_highlight))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                (description as FabOverlapTextView).setText(descText)
            } else {
                HtmlUtils.setTextWithNiceLinks(description as TextView?, descText)
            }
        } else {
            description!!.visibility = View.GONE
        }
        val nf = NumberFormat.getInstance()
        likeCount?.text = res.getQuantityString(R.plurals.likes,
                shot!!.likes_count.toInt(),
                nf.format(shot!!.likes_count))
        likeCount?.setOnClickListener { v ->
            (likeCount!!.compoundDrawables[1] as AnimatedVectorDrawable).start()
            if (shot!!.likes_count > 0) {
                PlayerSheet.start(this@DribbbleShot, shot!!)
            }
        }
        if (shot!!.likes_count == 0L) {
            likeCount?.background = null // clear touch ripple if doesn't do anything
        }
        viewCount?.text = res.getQuantityString(R.plurals.views,
                shot!!.views_count.toInt(),
                nf.format(shot!!.views_count))
        viewCount?.setOnClickListener { _ ->
            (viewCount!!.compoundDrawables[1] as
                    AnimatedVectorDrawable).start()
        }
        share?.setOnClickListener { _ ->
            (share!!.compoundDrawables[1] as AnimatedVectorDrawable).start()
            ShareDribbbleImageTask(this@DribbbleShot, shot!!).execute()
        }
        if (shot!!.user != null) {
            playerName!!.text = shot!!.user.name.toLowerCase()
            if (playerAvatar != null) {
                GlideApp.with(this)
                        .load(shot!!.user.highQualityAvatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.avatar_placeholder)
                        .override(largeAvatarSize, largeAvatarSize)
                        .transition(withCrossFade())
                        .into(playerAvatar!!)
            }
            val playerClick: View.OnClickListener = View.OnClickListener {
                val player = Intent(this@DribbbleShot, PlayerActivity::class.java)
                /*if (shot!!.user.shots_count > 0) {
                    // legit user object
                    player.putExtra(PlayerActivity.EXTRA_PLAYER, shot!!.user)
                } else {
                    // search doesn't fully populate the user object,
                    // in this case send the ID not the full user
                    player.putExtra(PlayerActivity.EXTRA_PLAYER_NAME, shot!!.user.username)
                    player.putExtra(PlayerActivity.EXTRA_PLAYER_ID, shot!!.user.id)
                }*/
                // legit user object
                player.putExtra(PlayerActivity.EXTRA_PLAYER, shot!!.user)
                val options = ActivityOptions.makeSceneTransitionAnimation(this@DribbbleShot,
                        playerAvatar, getString(R.string.transition_player_avatar))
                startActivity(player, options.toBundle())
            }
            playerAvatar?.setOnClickListener(playerClick)
            playerName?.setOnClickListener(playerClick)
            if (shot!!.created_at != null) {
                shotTimeAgo?.text = DateUtils.getRelativeTimeSpanString(shot!!.created_at.time,
                        System.currentTimeMillis(),
                        DateUtils.SECOND_IN_MILLIS).toString().toLowerCase()
            }
        } else {
            playerName?.visibility = View.GONE
            playerAvatar?.visibility = View.GONE
            shotTimeAgo?.visibility = View.GONE
        }

        commentAnimator = CommentAnimator()
        commentsList.itemAnimator = commentAnimator
        adapter = CommentsAdapter(shotDescription!!, commentFooter, shot!!.comments_count,
                resources.getInteger(R.integer.comment_expand_collapse_duration).toLong())
        commentsList.adapter = adapter
        commentsList.addItemDecoration(InsetDividerDecoration(
                res.getDimensionPixelSize(R.dimen.divider_height),
                res.getDimensionPixelSize(R.dimen.keyline_1),
                ContextCompat.getColor(this, R.color.divider)))
        if (shot!!.comments_count != 0L) {
            loadComments()
        }
        checkLiked()
    }

    internal fun reportUrlError() {
        Snackbar.make(draggableFrame, R.string.bad_dribbble_shot_url, Snackbar.LENGTH_SHORT).show()
        draggableFrame.postDelayed({ this.finishAfterTransition() }, 3000L)
    }

    internal fun loadComments() {
        val commentsCall = dribbblePrefs.getApi().getComments(shot!!.id, 0, DribbbleService.PER_PAGE_MAX)
        commentsCall.enqueue(object : Callback<List<Comment>> {
            override fun onResponse(call: Call<List<Comment>>, response: Response<List<Comment>>) {
                val comments = response.body()
                if (comments != null && !comments.isEmpty()) {
                    adapter.addComments(comments)
                }
            }

            override fun onFailure(call: Call<List<Comment>>, t: Throwable) {}
        })
    }

    internal fun setResultAndFinish() {
        val resultData = Intent()
        resultData.putExtra(RESULT_EXTRA_SHOT_ID, shot!!.id)
        setResult(Activity.RESULT_OK, resultData)
        finishAfterTransition()
    }

    internal fun calculateFabPosition() {
        // calculate 'natural' position i.e. with full height image. Store it for use when scrolling
        fabOffset = imageView.height + title!!.height - fab.height / 2
        fab.setOffset(fabOffset)

        // calculate min position i.e. pinned to the collapsed image when scrolled
        fab.setMinOffset(imageView.minimumHeight - fab.height / 2)
    }

    internal fun doLike() {
        performingLike = true
        if (fab.isChecked) {
            val likeCall = dribbblePrefs.getApi().like(shot!!.id)
            likeCall.enqueue(object : Callback<Like> {
                override fun onResponse(call: Call<Like>, response: Response<Like>) {
                    performingLike = false
                }

                override fun onFailure(call: Call<Like>, t: Throwable) {
                    performingLike = false
                }
            })
        } else {
            val unlikeCall = dribbblePrefs.getApi().unlike(shot!!.id)
            unlikeCall.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    performingLike = false
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    performingLike = false
                }
            })
        }
    }

    internal fun isOP(playerId: Long): Boolean {
        return shot!!.user != null && shot!!.user.id == playerId
    }

    private fun checkLiked() {
        if (shot != null && dribbblePrefs.isLoggedIn) {
            val likedCall = dribbblePrefs.getApi().liked(shot!!.id)
            likedCall.enqueue(object : Callback<Like> {
                override fun onResponse(call: Call<Like>, response: Response<Like>) {
                    // note that like.user will be null here
                    fab.isChecked = response.body() != null
                }

                override fun onFailure(call: Call<Like>, t: Throwable) {
                    // 404 is expected if shot is not liked
                    fab.isChecked = false
                    fab.jumpDrawablesToCurrentState()
                }
            })
        }
    }

    private fun setupCommenting() {
        allowComment = !dribbblePrefs.isLoggedIn || dribbblePrefs.isLoggedIn && dribbblePrefs.userCanPost()
        if (allowComment && commentFooter == null) {
            commentFooter = layoutInflater.inflate(R.layout.dribbble_enter_comment,
                    commentsList, false)
            userAvatar = commentFooter!!.findViewById<View>(R.id.avatar) as ForegroundImageView
            enterComment = commentFooter!!.findViewById(R.id.comment)
            postComment = commentFooter!!.findViewById(R.id.post_comment)
            enterComment!!.onFocusChangeListener = enterCommentFocus
        } else if (!allowComment && commentFooter != null) {
            adapter.removeCommentingFooter()
            commentFooter = null
            Toast.makeText(applicationContext,
                    R.string.prospects_cant_post, Toast.LENGTH_SHORT).show()
        }

        if (allowComment
                && dribbblePrefs.isLoggedIn
                && !TextUtils.isEmpty(dribbblePrefs.userAvatar)) {
            GlideApp.with(this)
                    .load(dribbblePrefs.userAvatar)
                    .circleCrop()
                    .placeholder(R.drawable.ic_player)
                    .transition(withCrossFade())
                    .into(userAvatar!!)
        }
    }

    internal inner class CommentsAdapter(
            private val description: View,
            private var footer: View?,
            commentCount: Long,
            expandDuration: Long) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val comments = ArrayList<Comment>(0)
        private val expandCollapse: Transition

        private var loading: Boolean = false
        private var noComments: Boolean = false
        var expandedCommentPosition = RecyclerView.NO_POSITION

        init {
            noComments = commentCount == 0L
            loading = !noComments
            expandCollapse = AutoTransition()
            expandCollapse.duration = expandDuration
            expandCollapse.interpolator = getFastOutSlowInInterpolator(this@DribbbleShot)
            expandCollapse.addListener(object : TransitionUtils.TransitionListenerAdapter() {
                override fun onTransitionStart(transition: Transition) {
                    commentsList.setOnTouchListener(touchEater)
                }

                override fun onTransitionEnd(transition: Transition) {
                    commentAnimator.setAnimateMoves(true)
                    commentsList.setOnTouchListener(null)
                }
            })
        }

        override fun getItemViewType(position: Int): Int {
            if (position == 0) return R.layout.dribbble_shot_description
            if (position == 1) {
                if (loading) return R.layout.loading
                if (noComments) return R.layout.dribbble_no_comments
            }
            if (footer != null) {
                val footerPos = if (loading || noComments) 2 else comments.size + 1
                if (position == footerPos) return R.layout.dribbble_enter_comment
            }
            return R.layout.dribbble_comment
        }

        override fun getItemCount(): Int {
            var count = 1 // description
            if (!comments.isEmpty()) {
                count += comments.size
            } else {
                count++ // either loading or no comments
            }
            if (footer != null) count++
            return count
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            when (viewType) {
                R.layout.dribbble_shot_description -> return SimpleViewHolder(description)
                R.layout.dribbble_comment -> return createCommentHolder(parent, viewType)
                R.layout.loading, R.layout.dribbble_no_comments -> return SimpleViewHolder(
                        layoutInflater.inflate(viewType, parent, false))
                R.layout.dribbble_enter_comment -> return SimpleViewHolder(footer!!)
            }
            throw IllegalArgumentException()
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                R.layout.dribbble_comment -> bindComment(holder as CommentViewHolder, getComment(position))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, partialChangePayloads: MutableList<Any>) {
            if (holder is CommentViewHolder) {
                bindPartialCommentChange(
                        holder, position, partialChangePayloads)
            } else {
                onBindViewHolder(holder, position)
            }
        }

        fun getComment(adapterPosition: Int): Comment {
            return comments[adapterPosition - 1] // description
        }

        fun addComments(newComments: List<Comment>?) {
            hideLoadingIndicator()
            noComments = false
            comments.addAll(newComments!!)
            notifyItemRangeInserted(1, newComments.size)
        }

        fun removeCommentingFooter() {
            if (footer == null) return
            val footerPos = itemCount - 1
            footer = null
            notifyItemRemoved(footerPos)
        }

        private fun createCommentHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
            val holder = CommentViewHolder(
                    layoutInflater.inflate(viewType, parent, false))

            holder.itemView.setOnClickListener { _ ->
                val position = holder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener

                val comment = getComment(position)
                TransitionManager.beginDelayedTransition(commentsList, expandCollapse)
                commentAnimator.setAnimateMoves(false)

                // collapse any currently expanded items
                if (RecyclerView.NO_POSITION != expandedCommentPosition) {
                    notifyItemChanged(expandedCommentPosition, COLLAPSE)
                }

                // expand this item (if it wasn't already)
                if (expandedCommentPosition != position) {
                    expandedCommentPosition = position
                    notifyItemChanged(position, EXPAND)
                    if (comment.liked == null) {
                        val liked = dribbblePrefs.getApi().likedComment(shot!!.id, comment.id)
                        liked.enqueue(object : Callback<Like> {
                            override fun onResponse(call: Call<Like>, response: Response<Like>) {
                                comment.liked = response.isSuccessful
                                holder.likeHeart.isChecked = comment.liked
                                holder.likeHeart.jumpDrawablesToCurrentState()
                            }

                            override fun onFailure(call: Call<Like>, t: Throwable) {}
                        })
                    }
                    if (enterComment != null && enterComment!!.hasFocus()) {
                        enterComment!!.clearFocus()
                        ImeUtils.hideIme(enterComment!!)
                    }
                    holder.itemView.requestFocus()
                } else {
                    expandedCommentPosition = RecyclerView.NO_POSITION
                }
            }

            holder.avatar.setOnClickListener { _ ->
                val position = holder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener

                val comment = getComment(position)
                val player = Intent(this@DribbbleShot, PlayerActivity::class.java)
                player.putExtra(PlayerActivity.EXTRA_PLAYER, comment.user)
                val options = ActivityOptions.makeSceneTransitionAnimation(this@DribbbleShot,
                        Pair.create(holder.itemView,
                                getString(R.string.transition_player_background)),
                        Pair.create<View, String>(holder.avatar,
                                getString(R.string.transition_player_avatar)))
                startActivity(player, options.toBundle())
            }

            holder.reply.setOnClickListener { _ ->
                val position = holder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener

                val comment = getComment(position)
                enterComment?.setText("@" + comment.user.username + " ")
                enterComment?.setSelection(enterComment!!.text.length)

                // collapse the comment and scroll the reply box (in the footer) into view
                expandedCommentPosition = RecyclerView.NO_POSITION
                notifyItemChanged(position, REPLY)
                holder.reply.jumpDrawablesToCurrentState()
                enterComment?.requestFocus()
                commentsList.smoothScrollToPosition(itemCount - 1)
            }

            holder.likeHeart.setOnClickListener { _ ->
                if (dribbblePrefs.isLoggedIn) {
                    val position = holder.adapterPosition
                    if (position == RecyclerView.NO_POSITION) return@setOnClickListener

                    val comment = getComment(position)
                    if (comment.liked == null || !comment.liked) {
                        comment.liked = true
                        comment.likes_count++
                        holder.likesCount.text = comment.likes_count.toString()
                        notifyItemChanged(position, COMMENT_LIKE)
                        val likeCommentCall = dribbblePrefs.getApi().likeComment(shot!!.id, comment.id)
                        likeCommentCall.enqueue(object : Callback<Like> {
                            override fun onResponse(call: Call<Like>, response: Response<Like>) {}

                            override fun onFailure(call: Call<Like>, t: Throwable) {}
                        })
                    } else {
                        comment.liked = false
                        comment.likes_count--
                        holder.likesCount.text = comment.likes_count.toString()
                        notifyItemChanged(position, COMMENT_LIKE)
                        val unlikeCommentCall = dribbblePrefs.getApi().unlikeComment(shot!!.id, comment.id)
                        unlikeCommentCall.enqueue(object : Callback<Void> {
                            override fun onResponse(call: Call<Void>, response: Response<Void>) {}

                            override fun onFailure(call: Call<Void>, t: Throwable) {}
                        })
                    }
                } else {
                    holder.likeHeart.isChecked = false
                    startActivityForResult(Intent(this@DribbbleShot,
                            DribbbleLogin::class.java), RC_LOGIN_LIKE)
                }
            }

            holder.likesCount.setOnClickListener { _ ->
                val position = holder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener

                val comment = getComment(position)
                val commentLikesCall = dribbblePrefs.getApi().getCommentLikes(shot!!.id, comment.id)
                commentLikesCall.enqueue(object : Callback<List<Like>> {
                    override fun onResponse(call: Call<List<Like>>,
                                            response: Response<List<Like>>) {
                        // TODO something better than this.
                        val sb = StringBuilder("Liked by:\n\n")
                        for (like in response.body()!!) {
                            if (like.user != null) {
                                sb.append("@")
                                sb.append(like.user.username)
                                sb.append("\n")
                            }
                        }
                        Toast.makeText(applicationContext, sb.toString(), Toast
                                .LENGTH_SHORT).show()
                    }

                    override fun onFailure(call: Call<List<Like>>, t: Throwable) {}
                })
            }

            return holder
        }

        private fun bindComment(holder: CommentViewHolder, comment: Comment) {
            val position = holder.adapterPosition
            val isExpanded = position == expandedCommentPosition
            GlideApp.with(this@DribbbleShot)
                    .load(comment.user.highQualityAvatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.avatar_placeholder)
                    .override(largeAvatarSize, largeAvatarSize)
                    .transition(withCrossFade())
                    .into(holder.avatar)
            holder.author.text = comment.user.name.toLowerCase()
            holder.author.isOriginalPoster = isOP(comment.user.id)
            holder.timeAgo.text = if (comment.created_at == null)
                ""
            else
                DateUtils.getRelativeTimeSpanString(comment.created_at.time,
                        System.currentTimeMillis(),
                        DateUtils.SECOND_IN_MILLIS)
                        .toString().toLowerCase()
            HtmlUtils.setTextWithNiceLinks(holder.commentBody,
                    comment.getParsedBody(holder.commentBody))
            holder.likeHeart.isChecked = comment.liked != null && comment.liked
            holder.likeHeart.isEnabled = comment.user.id != dribbblePrefs.userId
            holder.likesCount.text = comment.likes_count.toString()
            setExpanded(holder, isExpanded)
        }

        private fun setExpanded(holder: CommentViewHolder, isExpanded: Boolean) {
            holder.itemView.isActivated = isExpanded
            holder.reply.visibility = if (isExpanded && allowComment) View.VISIBLE else View.GONE
            holder.likeHeart.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.likesCount.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }

        private fun bindPartialCommentChange(
                holder: CommentViewHolder, position: Int, partialChangePayloads: List<Any>?) {
            // for certain changes we don't need to rebind data, just update some view state
            if (partialChangePayloads!!.contains(EXPAND) || partialChangePayloads.contains(COLLAPSE) || partialChangePayloads.contains(REPLY)) {
                setExpanded(holder, position == expandedCommentPosition)
            } else if (partialChangePayloads.contains(COMMENT_LIKE)) {
                return  // nothing to do
            } else {
                onBindViewHolder(holder, position)
            }
        }

        private fun hideLoadingIndicator() {
            if (!loading) return
            loading = false
            notifyItemRemoved(1)
        }

    }

    internal class SimpleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    internal class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), Divided {

        var avatar: ImageView = itemView.findViewById(R.id.player_avatar)
        var author: AuthorTextView = itemView.findViewById(R.id.comment_author)
        var timeAgo: TextView = itemView.findViewById(R.id.comment_time_ago)
        var commentBody: TextView = itemView.findViewById(R.id.comment_text)
        var reply: ImageButton = itemView.findViewById(R.id.comment_reply)
        var likeHeart: CheckableImageButton = itemView.findViewById(R.id.comment_like)
        var likesCount: TextView = itemView.findViewById(R.id.comment_likes_count)
    }

    /**
     * A [RecyclerView.ItemAnimator] which allows disabling move animations. RecyclerView
     * does not like animating item height changes. [android.transition.ChangeBounds] allows
     * this but in order to simultaneously collapse one item and expand another, we need to run the
     * Transition on the entire RecyclerView. As such it attempts to move views around. This
     * custom item animator allows us to stop RecyclerView from trying to handle this for us while
     * the transition is running.
     */
    internal class CommentAnimator : SlideInItemAnimator() {

        private var animateMoves: Boolean = false

        fun setAnimateMoves(animateMoves: Boolean) {
            this.animateMoves = animateMoves
        }

        override fun animateMove(
                holder: RecyclerView.ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
            if (!animateMoves) {
                dispatchMoveFinished(holder)
                return false
            }
            return super.animateMove(holder, fromX, fromY, toX, toY)
        }
    }

    companion object {

        const val EXTRA_SHOT = "EXTRA_SHOT"
        const val RESULT_EXTRA_SHOT_ID = "RESULT_EXTRA_SHOT_ID"

        private const val EXPAND = 0x1
        private const val COLLAPSE = 0x2
        private const val COMMENT_LIKE = 0x3
        private const val REPLY = 0x4

        private const val RC_LOGIN_LIKE = 0
        private const val RC_LOGIN_COMMENT = 1
        private const val SCRIM_ADJUSTMENT = 0.075f
    }

}
