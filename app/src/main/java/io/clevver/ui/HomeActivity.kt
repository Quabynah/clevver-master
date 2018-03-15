/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.*
import android.text.Annotation
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.transition.TransitionManager
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.widget.*
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import io.clevver.R
import io.clevver.api.ClevverUtils
import io.clevver.ui.*
import io.clevver.data.DataManager
import io.clevver.data.PlaidItem
import io.clevver.data.Source
import io.clevver.data.api.dribbble.model.Shot
import io.clevver.data.prefs.BehancePrefs
import io.clevver.data.prefs.DribbblePrefs
import io.clevver.data.prefs.SourceManager
import io.clevver.ui.recyclerview.FilterTouchHelperCallback
import io.clevver.ui.recyclerview.GridItemDividerDecoration
import io.clevver.ui.recyclerview.InfiniteScrollListener
import io.clevver.ui.transitions.MorphTransform
import io.clevver.util.AnimUtils
import io.clevver.util.ViewUtils
import io.clevver.util.bindView
import io.clevver.util.drawableToBitmap
import java.security.InvalidParameterException
import java.util.*


/**
 * Home screen: Displays shots, news and stories from all sources
 */
class HomeActivity : Activity() {

    private val drawer: DrawerLayout by bindView(R.id.drawer)
    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val grid: RecyclerView by bindView(R.id.grid)
    private val fab: ImageButton by bindView(R.id.fab)
    private val filtersList: RecyclerView by bindView(R.id.filters)
    private val loading: ProgressBar by bindView(android.R.id.empty)
    private var noConnection: ImageView? = null
    private var fabPosting: ImageButton? = null
    private lateinit var layoutManager: GridLayoutManager
    private var columns: Int = 0
    internal var connected = true
    private var noFiltersEmptyText: TextView? = null
    private var monitoringConnectivity = false

    // data
    private lateinit var dataManager: DataManager
    private lateinit var adapter: FeedAdapter
    private lateinit var filtersAdapter: FilterAdapter
    private lateinit var dribbblePrefs: DribbblePrefs
    private lateinit var behancePrefs: BehancePrefs
    private lateinit var prefs: WelcomePrefs

    //others
    private var revealX: Int = 0
    private var revealY: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        doCircularReveal(savedInstanceState)

        drawer.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        setActionBar(toolbar)
        if (savedInstanceState == null) {
            animateToolbar()
        }
        setExitSharedElementCallback(FeedAdapter.createSharedElementReenterCallback(this))

        dribbblePrefs = DribbblePrefs[this]
        behancePrefs = BehancePrefs[this]
        prefs = WelcomePrefs()


        columns = resources.getInteger(R.integer.num_columns)
        filtersAdapter = FilterAdapter(this, SourceManager.getSources(this)
        ) { sharedElement, forSource ->
            val login = Intent(this@HomeActivity, DribbbleLogin::class.java)
            MorphTransform.addExtras(login,
                    ContextCompat.getColor(this@HomeActivity, R.color.background_dark),
                    sharedElement.height / 2)
            val options = ActivityOptions.makeSceneTransitionAnimation(this@HomeActivity,
                    sharedElement, getString(R.string.transition_dribbble_login))
            startActivityForResult(login,
                    getAuthSourceRequestCode(forSource), options.toBundle())
        }
        dataManager = object : DataManager(this@HomeActivity, filtersAdapter) {
            override fun onDataLoaded(data: List<PlaidItem>) {
                adapter.addAndResort(data)
                checkEmptyState()
            }
        }
        val shotPreloadSizeProvider = ViewPreloadSizeProvider<Shot>()
        adapter = FeedAdapter(this, dataManager, columns, shotPreloadSizeProvider)

        grid.adapter = adapter
        layoutManager = GridLayoutManager(this, columns)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return adapter.getItemColumnSpan(position)
            }
        }
        grid.layoutManager = layoutManager
        grid.addOnScrollListener(toolbarElevation)
        grid.addOnScrollListener(object : InfiniteScrollListener(layoutManager, dataManager) {
            override fun onLoadMore() {
                dataManager.loadAllDataSources()
            }
        })
        grid.setHasFixedSize(true)
        grid.addItemDecoration(GridItemDividerDecoration(this, R.dimen.divider_height,
                R.color.divider))
        grid.itemAnimator = HomeGridItemAnimator()

        val shotPreloader = RecyclerViewPreloader(this, adapter, shotPreloadSizeProvider, 4)
        grid.addOnScrollListener(shotPreloader)

        // drawer layout treats fitsSystemWindows specially so we have to handle insets ourselves
        drawer.setOnApplyWindowInsetsListener { _, insets ->
            // inset the toolbar down by the status bar height
            val lpToolbar = toolbar
                    .layoutParams as ViewGroup.MarginLayoutParams
            lpToolbar.topMargin += insets.systemWindowInsetTop
            lpToolbar.leftMargin += insets.systemWindowInsetLeft
            lpToolbar.rightMargin += insets.systemWindowInsetRight
            toolbar.layoutParams = lpToolbar

            // inset the grid top by statusbar+toolbar & the bottom by the navbar (don't clip)
            grid.setPadding(
                    grid.paddingLeft + insets.systemWindowInsetLeft, // landscape
                    insets.systemWindowInsetTop + ViewUtils.getActionBarSize(this@HomeActivity),
                    grid.paddingRight + insets.systemWindowInsetRight, // landscape
                    grid.paddingBottom + insets.systemWindowInsetBottom)

            // inset the fab for the navbar
            val lpFab = fab
                    .layoutParams as ViewGroup.MarginLayoutParams
            lpFab.bottomMargin += insets.systemWindowInsetBottom // portrait
            lpFab.rightMargin += insets.systemWindowInsetRight // landscape
            fab.layoutParams = lpFab

            val postingStub = findViewById<View>(R.id.stub_posting_progress)
            val lpPosting = postingStub.layoutParams as ViewGroup.MarginLayoutParams
            lpPosting.bottomMargin += insets.systemWindowInsetBottom // portrait
            lpPosting.rightMargin += insets.systemWindowInsetRight // landscape
            postingStub.layoutParams = lpPosting

            // we place a background behind the status bar to combine with it's semi-transparent
            // color to get the desired appearance.  Set it's height to the status bar height
            val statusBarBackground = findViewById<View>(R.id.status_bar_background)
            val lpStatus = statusBarBackground.layoutParams as FrameLayout.LayoutParams
            lpStatus.height = insets.systemWindowInsetTop
            statusBarBackground.layoutParams = lpStatus

            // inset the filters list for the status bar / navbar
            // need to set the padding end for landscape case
            val ltr = filtersList.layoutDirection == View.LAYOUT_DIRECTION_LTR
            filtersList.setPaddingRelative(filtersList.paddingStart,
                    filtersList.paddingTop + insets.systemWindowInsetTop,
                    filtersList.paddingEnd + if (ltr)
                        insets.systemWindowInsetRight
                    else
                        0,
                    filtersList.paddingBottom + insets.systemWindowInsetBottom)

            // clear this listener so insets aren't re-applied
            drawer.setOnApplyWindowInsetsListener(null)

            insets.consumeSystemWindowInsets()
        }
        setupTaskDescription()

        fab.setOnClickListener({ fabClick() })

        filtersList.adapter = filtersAdapter
        filtersList.itemAnimator = FilterAdapter.FilterAnimator()
        filtersAdapter.registerFilterChangedCallback(filtersChangedCallbacks)
        dataManager.loadAllDataSources()
        val callback = FilterTouchHelperCallback(filtersAdapter, this)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(filtersList)
        checkEmptyState()
    }

    /**
     * Circular reveal animation was extracted from
     * {@Link https://android.jlelse.eu/a-little-thing-that-matter-how-to-reveal-an-activity-with-circular-revelation-d94f9bfcae28}
     */
    private fun doCircularReveal(savedInstanceState: Bundle?) {
        val intent = intent
        if (savedInstanceState == null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && intent.hasExtra(EXTRA_CIRCULAR_REVEAL_X)
                && intent.hasExtra(EXTRA_CIRCULAR_REVEAL_Y)) {
            drawer.visibility = View.INVISIBLE

            revealX = intent.getIntExtra(EXTRA_CIRCULAR_REVEAL_X, 0)
            revealY = intent.getIntExtra(EXTRA_CIRCULAR_REVEAL_Y, 0)

            val treeObserver = drawer.viewTreeObserver
            if (treeObserver.isAlive) {
                treeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        revealActivity()
                        drawer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            }

        } else {
            drawer.visibility = View.VISIBLE
        }
    }

    protected fun revealActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val finalRadius = (Math.max(drawer.width, drawer.height).times(1.1)).toFloat()

            //create animator for this view
            //the start radius is 0
            val circularReveal = ViewAnimationUtils.createCircularReveal(drawer, revealX, revealY, 0f, finalRadius)
            circularReveal.duration = 600L
            circularReveal.interpolator = AccelerateInterpolator()

            //make the view visible and start animation
            drawer.visibility = View.VISIBLE
            circularReveal.start()
        } else {
            finish()
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected fun unrevealActivity() {
        val finalRadius = (Math.max(drawer.width, drawer.height).times(1.1)).toFloat()

        //create animator for this view
        //the start radius is 0
        val circularReveal = ViewAnimationUtils.createCircularReveal(drawer, revealX, revealY, finalRadius, 0f)
        circularReveal.duration = 600L
        circularReveal.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                drawer.visibility = View.INVISIBLE
                finish()
            }
        })
        circularReveal.start()

    }

    // listener for notifying adapter when data sources are deactivated
    private val filtersChangedCallbacks = object : FilterAdapter.FiltersChangedCallbacks() {
        override fun onFiltersChanged(changedFilter: Source) {
            if (!changedFilter.active) {
                adapter.removeDataSource(changedFilter.key)
            }
            checkEmptyState()
        }

        override fun onFilterRemoved(removed: Source) {
            adapter.removeDataSource(removed.key)
            checkEmptyState()
        }
    }

    private val toolbarElevation = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
            // we want the grid to scroll over the top of the toolbar but for the toolbar items
            // to be clickable when visible. To achieve this we play games with elevation. The
            // toolbar is laid out in front of the grid but when we scroll, we lower it's elevation
            // to allow the content to pass in front (and reset when scrolled to top of the grid)
            if (newState == RecyclerView.SCROLL_STATE_IDLE
                    && layoutManager.findFirstVisibleItemPosition() == 0
                    && layoutManager.findViewByPosition(0).top == grid.paddingTop
                    && toolbar.translationZ != 0f) {
                // at top, reset elevation
                toolbar.translationZ = 0f
            } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING && toolbar.translationZ != -1f) {
                // grid scrolled, lower toolbar to allow content to pass in front
                toolbar.translationZ = -1f
            }
        }
    }

    private val connectivityCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            connected = true
            if (adapter.dataItemCount != 0) return
            runOnUiThread {
                TransitionManager.beginDelayedTransition(drawer)
                noConnection!!.visibility = View.GONE
                loading.visibility = View.VISIBLE
                dataManager.loadAllDataSources()
            }
        }

        override fun onLost(network: Network) {
            connected = false
        }
    }

    override fun onResume() {
        super.onResume()
        dribbblePrefs.addLoginStatusListener(filtersAdapter)
        checkConnectivity()
    }

    override fun onPause() {
        dribbblePrefs.removeLoginStatusListener(filtersAdapter)
        if (monitoringConnectivity) {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(connectivityCallback)
            monitoringConnectivity = false
        }
        super.onPause()
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        if (data == null || resultCode != Activity.RESULT_OK
                || !data.hasExtra(DribbbleShot.RESULT_EXTRA_SHOT_ID))
            return

        // When reentering, if the shared element is no longer on screen (e.g. after an
        // orientation change) then scroll it into view.
        val sharedShotId = data.getLongExtra(DribbbleShot.RESULT_EXTRA_SHOT_ID, -1L)
        if (sharedShotId != -1L                                             // returning from a shot

                && adapter.dataItemCount > 0                           // grid populated

                && grid.findViewHolderForItemId(sharedShotId) == null) {    // view not attached
            val position = adapter.getItemPosition(sharedShotId)
            if (position == RecyclerView.NO_POSITION) return

            // delay the transition until our shared element is on-screen i.e. has been laid out
            postponeEnterTransition()
            grid.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(v: View, l: Int, t: Int, r: Int, b: Int,
                                            oL: Int, oT: Int, oR: Int, oB: Int) {
                    grid.removeOnLayoutChangeListener(this)
                    startPostponedEnterTransition()
                }
            })
            grid.scrollToPosition(position)
            toolbar.translationZ = -1f
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val dribbbleLogin = menu.findItem(R.id.menu_dribbble_login)
        dribbbleLogin?.setTitle(if (dribbblePrefs.isLoggedIn)
            R.string.dribbble_log_out
        else
            R.string.dribbble_login)
        val behanceLogin = menu.findItem(R.id.menu_behance_login)
        behanceLogin?.setTitle(if (behancePrefs.isLoggedIn)
            R.string.behance_log_out
        else
            R.string.behance_login)
        val dribbbleProfile = menu.findItem(R.id.menu_profile_dribbble)
        dribbbleProfile?.isEnabled = dribbblePrefs.isLoggedIn
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_filter -> {
                drawer.openDrawer(GravityCompat.END)
                return true
            }
            R.id.menu_search -> {
                val searchMenuView = toolbar.findViewById<View>(R.id.menu_search)
                val options = ActivityOptions.makeSceneTransitionAnimation(this, searchMenuView,
                        getString(R.string.transition_search_back)).toBundle()
                startActivityForResult(Intent(this, SearchActivity::class.java), RC_SEARCH, options)
                return true
            }
            R.id.menu_dribbble_login -> {
                if (dribbblePrefs.isLoggedIn) {
                    ClevverUtils.doLogout(this, ClevverUtils.TYPE_DRIBBBLE)
                } else {
                    startActivityForResult(Intent(this, DribbbleLogin::class.java),
                            RC_DRIBBBLE_LOGIN)
                }
                return true
            }
            R.id.menu_about -> {
                startActivity(Intent(this@HomeActivity, AboutActivity::class.java),
                        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                return true
            }
            R.id.menu_profile_dribbble -> {
                val profileIntent = Intent(this@HomeActivity, PlayerActivity::class.java)
                if (dribbblePrefs.isLoggedIn) {
                    //load user from either source
                    profileIntent.putExtra(PlayerActivity.EXTRA_PLAYER_NAME, dribbblePrefs.userName)
                    profileIntent.putExtra(PlayerActivity.EXTRA_PLAYER_ID, dribbblePrefs.userId)
                }
                startActivity(profileIntent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                return true
            }
            R.id.menu_behance_login -> {
                if (behancePrefs.isLoggedIn) {
                    ClevverUtils.doLogout(this, ClevverUtils.TYPE_BEHANCE)
                } else {
                    behancePrefs.login(this, RC_BEHANCE_AUTH)
                }
                return true
            }
            R.id.menu_github -> {
                //navigate to thr github repositories screen
                startActivity(Intent(this@HomeActivity, RepositoryActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        prefs.setUserState(System.currentTimeMillis().toString())
        if (drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END)
        } else {
            setResult(RESULT_OK)
            finishAfterTransition()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_SEARCH -> {
                // reset the search icon which we hid
                val searchMenuView = toolbar.findViewById<View>(R.id.menu_search)
                if (searchMenuView != null) {
                    searchMenuView.alpha = 1f
                }
                if (resultCode == SearchActivity.RESULT_CODE_SAVE) {
                    if (data != null) {
                        val query = data.getStringExtra(SearchActivity.EXTRA_QUERY)
                        if (TextUtils.isEmpty(query)) return
                        var dribbbleSearch: Source? = null
                        var newSource = false
                        if (data.getBooleanExtra(SearchActivity.EXTRA_SAVE_DRIBBBLE, false)) {
                            dribbbleSearch = Source.DribbbleSearchSource(query, true)
                            newSource = filtersAdapter.addFilter(dribbbleSearch)
                        }
                        if (newSource) {
                            highlightNewSources(dribbbleSearch)
                        }
                    }
                }
            }
            RC_BEHANCE_AUTH -> if (resultCode == RESULT_OK) {
                showFab()
            }
            RC_DRIBBBLE_LOGIN -> if (resultCode == Activity.RESULT_OK) {
                showFab()
            }
            RC_AUTH_DRIBBBLE_FOLLOWING -> if (resultCode == Activity.RESULT_OK) {
                filtersAdapter.enableFilterByKey(SourceManager.SOURCE_DRIBBBLE_FOLLOWING, this)
            }
            RC_AUTH_DRIBBBLE_USER_LIKES -> if (resultCode == Activity.RESULT_OK) {
                filtersAdapter.enableFilterByKey(
                        SourceManager.SOURCE_DRIBBBLE_USER_LIKES, this)
            }
            RC_AUTH_DRIBBBLE_USER_SHOTS -> if (resultCode == Activity.RESULT_OK) {
                filtersAdapter.enableFilterByKey(
                        SourceManager.SOURCE_DRIBBBLE_USER_SHOTS, this)
            }
            TOUR_CODE -> if (resultCode == RESULT_OK) {
                prefs.setUserState(System.currentTimeMillis().toString())
                val snackbar = Snackbar.make(drawer, "Thanks for taking the tour with us", Snackbar.LENGTH_INDEFINITE)
                snackbar.setAction("Dismiss", { _ ->
                    snackbar.dismiss()
                })
                snackbar.show()
            }
        }
    }

    override fun onDestroy() {
        dataManager.cancelLoading()
        filtersAdapter.unregisterFilterChangedCallback(filtersChangedCallbacks)
        super.onDestroy()
    }

    private fun fabClick() {
        //todo: add some logic here
    }

    private fun checkEmptyState() {
        if (adapter.dataItemCount == 0) {
            // if grid is empty check whether we're loading or if no filters are selected
            if (filtersAdapter.enabledSourcesCount > 0) {
                if (connected) {
                    loading.visibility = View.VISIBLE
                    setNoFiltersEmptyTextVisibility(View.GONE)
                }
            } else {
                loading.visibility = View.GONE
                setNoFiltersEmptyTextVisibility(View.VISIBLE)
            }
            toolbar.translationZ = 0f
        } else {
            loading.visibility = View.GONE
            setNoFiltersEmptyTextVisibility(View.GONE)
        }
    }

    private fun getAuthSourceRequestCode(filter: Source): Int {
        when (filter.key) {
            SourceManager.SOURCE_DRIBBBLE_FOLLOWING -> return RC_AUTH_DRIBBBLE_FOLLOWING
            SourceManager.SOURCE_DRIBBBLE_USER_LIKES -> return RC_AUTH_DRIBBBLE_USER_LIKES
            SourceManager.SOURCE_DRIBBBLE_USER_SHOTS -> return RC_AUTH_DRIBBBLE_USER_SHOTS
        }
        throw InvalidParameterException()
    }

    private fun setNoFiltersEmptyTextVisibility(visibility: Int) {
        if (visibility == View.VISIBLE) {
            if (noFiltersEmptyText == null) {
                // create the no filters empty text
                val stub = findViewById<ViewStub>(R.id.stub_no_filters)
                noFiltersEmptyText = stub.inflate() as TextView
                val emptyText = getText(R.string.no_filters_selected) as SpannedString
                val ssb = SpannableStringBuilder(emptyText)
                val annotations = emptyText.getSpans(0, emptyText.length, Annotation::class.java)
                if (annotations != null && annotations.isNotEmpty()) {
                    for (i in annotations.indices) {
                        val annotation = annotations[i]
                        if (annotation.key == "src") {
                            // image span markup
                            val name = annotation.value
                            val id = resources.getIdentifier(name, null, packageName)
                            if (id == 0) continue
                            ssb.setSpan(ImageSpan(this, id,
                                    ImageSpan.ALIGN_BASELINE),
                                    emptyText.getSpanStart(annotation),
                                    emptyText.getSpanEnd(annotation),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        } else if (annotation.key == "foregroundColor") {
                            // foreground color span markup
                            val name = annotation.value
                            val id = resources.getIdentifier(name, null, packageName)
                            if (id == 0) continue
                            ssb.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, id)),
                                    emptyText.getSpanStart(annotation),
                                    emptyText.getSpanEnd(annotation),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }
                }
                noFiltersEmptyText!!.text = ssb
                noFiltersEmptyText!!.setOnClickListener { _ -> drawer.openDrawer(GravityCompat.END) }
            }
            noFiltersEmptyText!!.visibility = visibility
        } else if (noFiltersEmptyText != null) {
            noFiltersEmptyText!!.visibility = visibility
        }

    }

    private fun setupTaskDescription() {
        val overviewIcon = drawableToBitmap(this, applicationInfo.icon)
        setTaskDescription(ActivityManager.TaskDescription(getString(R.string.app_name),
                overviewIcon,
                ContextCompat.getColor(this, R.color.primary)))
        overviewIcon!!.recycle()
    }

    private fun animateToolbar() {
        // this is gross but toolbar doesn't expose it's children to animate them :(
        val t = toolbar.getChildAt(0)
        if (t != null && t is TextView) {

            // fade in and space out the title.  Animating the letterSpacing performs horribly so
            // fake it by setting the desired letterSpacing then animating the scaleX ¯\_(ツ)_/¯
            t.alpha = 0f
            t.scaleX = 0.8f

            t.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .setStartDelay(300)
                    .setDuration(900).interpolator = AnimUtils.getFastOutSlowInInterpolator(this)
        }
    }

    private fun showFab() {
        fab.alpha = 0f
        fab.scaleX = 0f
        fab.scaleY = 0f
        fab.translationY = (fab.height / 2).toFloat()
        fab.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(300L)
                .setInterpolator(AnimUtils.getLinearOutSlowInInterpolator(this))
                .start()
    }

    override fun onStart() {
        super.onStart()
        //Delay a little before displaying the dialog
        Handler().postDelayed({
            if (prefs.isNewUser) {
                MaterialDialog.Builder(this@HomeActivity)
                        .theme(Theme.DARK)
                        .content(getString(R.string.welcome_text))
                        .typeface(Typeface.createFromAsset(assets, "fonts/nunito_semibold.ttf"),
                                Typeface.createFromAsset(assets, "fonts/nunito_semibold.ttf"))
                        .title("Hello, new Clevver!")
                        .positiveText("Take tour")
                        .negativeText("Not now")
                        .onPositive({ dialog, _ ->
                            dialog.dismiss()
                            prefs.setUserState(System.currentTimeMillis().toString())
//                            startActivityForResult(Intent(this@HomeActivity, WelcomeActivity::class.java),
//                                    TOUR_CODE)
                        })
                        .onNegative({ dialog, _ ->
                            dialog.dismiss()
                        })
                        .canceledOnTouchOutside(false)
                        .build().show()
            } else {
                return@postDelayed
            }
        }, 1000)
    }

    /**
     * Highlight the new source(s) by:
     * 1. opening the drawer
     * 2. scrolling new source(s) into view
     * 3. flashing new source(s) background
     * 4. closing the drawer (if user hasn't interacted with it)
     */
    private fun highlightNewSources(vararg sources: Source?) {
        val closeDrawerRunnable = { drawer.closeDrawer(GravityCompat.END) }
        drawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {

            // if the user interacts with the filters while it's open then don't auto-close
            private val filtersTouch = View.OnTouchListener { _, _ ->
                drawer.removeCallbacks(closeDrawerRunnable)
                false
            }

            override fun onDrawerOpened(drawerView: View) {
                // scroll to the new item(s) and highlight them
                val filterPositions = ArrayList<Int>(sources.size)
                for (source in sources) {
                    if (source != null) {
                        filterPositions.add(filtersAdapter.getFilterPosition(source))
                    }
                }
                val scrollTo = Collections.max(filterPositions)
                filtersList.smoothScrollToPosition(scrollTo)
                for (position in filterPositions) {
                    filtersAdapter.highlightFilter(position)
                }
                filtersList.setOnTouchListener(filtersTouch)
            }

            override fun onDrawerClosed(drawerView: View) {
                // reset
                filtersList.setOnTouchListener(null)
                drawer.removeDrawerListener(this)
            }

            override fun onDrawerStateChanged(newState: Int) {
                // if the user interacts with the drawer manually then don't auto-close
                if (newState == DrawerLayout.STATE_DRAGGING) {
                    drawer.removeCallbacks(closeDrawerRunnable)
                }
            }
        })
        drawer.openDrawer(GravityCompat.END)
        drawer.postDelayed(closeDrawerRunnable, 2000L)
    }

    private fun checkConnectivity() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        connected = activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
        if (!connected) {
            loading.visibility = View.GONE
            if (noConnection == null) {
                val stub = findViewById<ViewStub>(R.id.stub_no_connection)
                noConnection = stub.inflate() as ImageView
            }
            val avd: AnimatedVectorDrawable? = getDrawable(R.drawable.avd_no_connection) as AnimatedVectorDrawable
            if (noConnection != null && avd != null) {
                noConnection!!.setImageDrawable(avd)
                avd.start()
            }

            connectivityManager.registerNetworkCallback(
                    NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                    connectivityCallback)
            monitoringConnectivity = true
        }
    }

    companion object {
        //Callbacks
        private const val RC_SEARCH = 0
        private const val RC_AUTH_DRIBBBLE_FOLLOWING = 1
        private const val RC_AUTH_DRIBBBLE_USER_LIKES = 2
        private const val RC_AUTH_DRIBBBLE_USER_SHOTS = 3
        private const val RC_DRIBBBLE_LOGIN = 7
        private const val RC_BEHANCE_AUTH = 8
        private const val TOUR_CODE = 9

        //Intent data
        const val EXTRA_CIRCULAR_REVEAL_X = "EXTRA_CIRCULAR_REVEAL_X"
        const val EXTRA_CIRCULAR_REVEAL_Y = "EXTRA_CIRCULAR_REVEAL_Y"

        //Welcome user prefs
        private const val WELCOME_PREFS = "WELCOME_PREFS"
        private const val KEY_STATE = "KEY_STATE"
    }

    /**
     * Welcome user state
     */
    inner class WelcomePrefs {
        private val prefs: SharedPreferences = getSharedPreferences(WELCOME_PREFS, Context.MODE_PRIVATE)

        var isNewUser: Boolean = false
        private var state: String? = null

        init {
            //Init state
            state = prefs.getString(KEY_STATE, null)

            isNewUser = state.isNullOrEmpty()
            if (isNewUser) {
                state = prefs.getString(KEY_STATE, null)
            }
        }

        fun setUserState(newState: String) {
            isNewUser = true
            state = newState
            prefs.edit().putString(KEY_STATE, state).apply()
        }

        fun clearState() {
            isNewUser = false
            state = null
            prefs.edit().putString(KEY_STATE, state).apply()
        }
    }
}
