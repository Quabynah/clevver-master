/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui

import android.app.Activity
import android.app.SearchManager
import android.app.SharedElementCallback
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.graphics.Typeface
import android.os.Bundle
import android.support.annotation.TransitionRes
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.StyleSpan
import android.transition.Transition
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import io.clevver.R
import io.clevver.data.PlaidItem
import io.clevver.data.SearchDataManager
import io.clevver.data.api.dribbble.model.Shot
import io.clevver.ui.recyclerview.InfiniteScrollListener
import io.clevver.ui.recyclerview.SlideInItemAnimator
import io.clevver.ui.transitions.CircularReveal
import io.clevver.util.ImeUtils
import io.clevver.util.ShortcutHelper
import io.clevver.util.TransitionUtils
import io.clevver.util.bindView

/**
 * Search dribbble shot or designer news by topic, name,author etc
 */
class SearchActivity : Activity() {

    private val searchBack: ImageButton by bindView(R.id.searchback)
    private val searchBackContainer: ViewGroup by bindView(R.id.searchback_container)
    private val searchView: SearchView by bindView(R.id.search_view)
    private val searchBackground: View by bindView(R.id.search_background)
    private val progress: ProgressBar by bindView(android.R.id.empty)
    private val results: RecyclerView by bindView(R.id.search_results)
    private val container: ViewGroup by bindView(R.id.container)
    private val searchToolbar: ViewGroup by bindView(R.id.search_toolbar)
    private val resultsContainer: ViewGroup by bindView(R.id.results_container)
    private val fab: ImageButton by bindView(R.id.fab)
    private val confirmSaveContainer: ViewGroup by bindView(R.id.confirm_save_container)
    private val saveConfirm: Button by bindView(R.id.save_confirmed)
    private val saveDribbble: CheckedTextView by bindView(R.id.save_dribbble)
    private val scrim: View by bindView(R.id.scrim)
    private val resultsScrim: View by bindView(R.id.results_scrim)


    private var columns: Int = 0
    private var appBarElevation: Float = 0.toFloat()
    private lateinit var dataManager: SearchDataManager
    private lateinit var adapter: FeedAdapter
    private var noResults: TextView? = null
    private val transitions = SparseArray<Transition>()
    private var focusQuery = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setupSearchView()

        appBarElevation = resources.getDimension(R.dimen.z_app_bar)
        columns = resources.getInteger(R.integer.num_columns)

        dataManager = object : SearchDataManager(this@SearchActivity) {
            override fun onDataLoaded(data: List<PlaidItem>?) {
                if (data != null && data.isNotEmpty()) {
                    if (results.visibility != View.VISIBLE) {
                        TransitionManager.beginDelayedTransition(container,
                                getTransition(R.transition.search_show_results))
                        progress.visibility = View.GONE
                        results.visibility = View.VISIBLE
                        fab.visibility = View.VISIBLE
                    }
                    adapter.addAndResort(data)
                } else {
                    TransitionManager.beginDelayedTransition(
                            container, getTransition(R.transition.auto))
                    progress.visibility = View.GONE
                    setNoResultsVisibility(View.VISIBLE)
                }
            }
        }
        val shotPreloadSizeProvider = ViewPreloadSizeProvider<Shot>()
        adapter = FeedAdapter(this, dataManager, columns, shotPreloadSizeProvider)
        setExitSharedElementCallback(FeedAdapter.createSharedElementReenterCallback(this))
        results.adapter = adapter
        results.itemAnimator = SlideInItemAnimator()
        val layoutManager = GridLayoutManager(this, columns)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return adapter.getItemColumnSpan(position)
            }
        }
        results.layoutManager = layoutManager
        results.addOnScrollListener(object : InfiniteScrollListener(layoutManager, dataManager) {
            override fun onLoadMore() {
                dataManager.loadMore()
            }
        })
        results.setHasFixedSize(true)
        val shotPreloader = RecyclerViewPreloader(this, adapter, shotPreloadSizeProvider, 4)
        results.addOnScrollListener(shotPreloader)

        setupTransitions()
        onNewIntent(intent)
        ShortcutHelper.reportSearchUsed(this)

        scrim.setOnClickListener({ dismiss() })
        searchBack.setOnClickListener({ dismiss() })
        fab.setOnClickListener({ save() })
        saveConfirm.setOnClickListener({ doSave() })
        resultsScrim.setOnClickListener({ hideSaveConfirmation() })
        saveDribbble.setOnClickListener({ toggleSaveCheck(saveDribbble) })
    }

    override fun onNewIntent(intent: Intent) {
        if (intent.hasExtra(SearchManager.QUERY)) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            if (!TextUtils.isEmpty(query)) {
                searchView.setQuery(query, false)
                searchFor(query)
            }
        }
    }

    override fun onBackPressed() {
        if (confirmSaveContainer.visibility == View.VISIBLE) {
            hideSaveConfirmation()
        } else {
            dismiss()
        }
    }

    override fun onPause() {
        // needed to suppress the default window animation when closing the activity
        overridePendingTransition(0, 0)
        super.onPause()
    }

    override fun onDestroy() {
        dataManager.cancelLoading()
        super.onDestroy()
    }

    override fun onEnterAnimationComplete() {
        if (focusQuery) {
            // focus the search view once the enter transition finishes
            searchView.requestFocus()
            ImeUtils.showIme(searchView)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            FeedAdapter.REQUEST_CODE_VIEW_SHOT ->
                // by default we focus the search filed when entering this screen. Don't do that
                // when returning from viewing a search result.
                focusQuery = false
        }
    }

    private fun dismiss() {
        // clear the background else the touch ripple moves with the translation which looks bad
        searchBack.background = null
        finishAfterTransition()
    }

    private fun save() {
        // show the save confirmation bubble
        TransitionManager.beginDelayedTransition(
                resultsContainer, getTransition(R.transition.search_show_confirm))
        fab.visibility = View.INVISIBLE
        confirmSaveContainer.visibility = View.VISIBLE
        resultsScrim.visibility = View.VISIBLE
    }

    private fun doSave() {
        val saveData = Intent()
        saveData.putExtra(EXTRA_QUERY, dataManager.query)
        saveData.putExtra(EXTRA_SAVE_DRIBBBLE, saveDribbble.isChecked)
        setResult(RESULT_CODE_SAVE, saveData)
        dismiss()
    }

    private fun hideSaveConfirmation() {
        if (confirmSaveContainer.visibility == View.VISIBLE) {
            TransitionManager.beginDelayedTransition(
                    resultsContainer, getTransition(R.transition.search_hide_confirm))
            confirmSaveContainer.visibility = View.GONE
            resultsScrim.visibility = View.GONE
            fab.visibility = results.visibility
        }
    }

    private fun toggleSaveCheck(ctv: CheckedTextView) {
        ctv.toggle()
    }

    internal fun clearResults() {
        TransitionManager.beginDelayedTransition(container, getTransition(R.transition.auto))
        adapter.clear()
        dataManager.clear()
        results.visibility = View.GONE
        progress.visibility = View.GONE
        fab.visibility = View.GONE
        confirmSaveContainer.visibility = View.GONE
        resultsScrim.visibility = View.GONE
        setNoResultsVisibility(View.GONE)
    }

    internal fun setNoResultsVisibility(visibility: Int) {
        if (visibility == View.VISIBLE) {
            if (noResults == null) {
                noResults = (findViewById<View>(R.id.stub_no_search_results) as ViewStub).inflate() as TextView
                noResults!!.setOnClickListener { v ->
                    searchView.setQuery("", false)
                    searchView.requestFocus()
                    ImeUtils.showIme(searchView)
                }
            }
            val message = String.format(
                    getString(R.string.no_search_results), searchView.query.toString())
            val ssb = SpannableStringBuilder(message)
            ssb.setSpan(StyleSpan(Typeface.ITALIC),
                    message.indexOf('“') + 1,
                    message.length - 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            noResults!!.text = ssb
        }
        if (noResults != null) {
            noResults!!.visibility = visibility
        }
    }

    internal fun searchFor(query: String) {
        clearResults()
        progress.visibility = View.VISIBLE
        ImeUtils.hideIme(searchView)
        searchView.clearFocus()
        dataManager.searchFor(query)
    }

    internal fun getTransition(@TransitionRes transitionId: Int): Transition {
        var transition: Transition? = transitions.get(transitionId)
        if (transition == null) {
            transition = TransitionInflater.from(this).inflateTransition(transitionId)
            transitions.put(transitionId, transition)
        }
        return transition!!
    }

    private fun setupSearchView() {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        // hint, inputType & ime options seem to be ignored from XML! Set in code
        searchView.queryHint = getString(R.string.search_hint)
        searchView.inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
        searchView.imeOptions = searchView.imeOptions or EditorInfo.IME_ACTION_SEARCH or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchFor(query)
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                if (TextUtils.isEmpty(query)) {
                    clearResults()
                }
                return true
            }
        })
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus && confirmSaveContainer.visibility == View.VISIBLE) {
                hideSaveConfirmation()
            }
        }
    }

    private fun setupTransitions() {
        // grab the position that the search icon transitions in *from*
        // & use it to configure the return transition
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onSharedElementStart(
                    sharedElementNames: List<String>,
                    sharedElements: List<View>?,
                    sharedElementSnapshots: List<View>) {
                if (sharedElements != null && !sharedElements.isEmpty()) {
                    val searchIcon = sharedElements[0]
                    if (searchIcon.id != R.id.searchback) return
                    val centerX = (searchIcon.left + searchIcon.right) / 2
                    val hideResults = TransitionUtils.findTransition(
                            window.returnTransition as TransitionSet,
                            CircularReveal::class.java, R.id.results_container) as CircularReveal?
                    hideResults?.setCenter(Point(centerX, 0))
                }
            }
        })
    }

    companion object {

        const val EXTRA_QUERY = "EXTRA_QUERY"
        const val EXTRA_SAVE_DRIBBBLE = "EXTRA_SAVE_DRIBBBLE"
        const val RESULT_CODE_SAVE = 7
    }

}
