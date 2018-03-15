/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui

import `in`.uncod.android.bypass.Bypass
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.TargetApi
import android.app.Activity
import android.app.assist.AssistContent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toolbar
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import io.clevver.R
import io.clevver.data.api.producthunt.model.Post
import io.clevver.ui.transitions.GravityArcMotion
import io.clevver.ui.widget.ElasticDragDismissFrameLayout
import io.clevver.util.AnimUtils
import io.clevver.util.HtmlUtils
import io.clevver.util.ViewUtils
import io.clevver.util.bindView
import io.clevver.util.customtabs.CustomTabActivityHelper
import io.clevver.util.glide.GlideApp
import io.clevver.util.glide.ImageSpanTarget

/**
 * Product Hunt details screen
 */
class ProductHunt : Activity() {

    private val container: ViewGroup by bindView(R.id.container)
    private val fab: ImageButton by bindView(R.id.fab)
    private val fabExpand: View by bindView(R.id.fab_expand)
    private val draggableFrame: ElasticDragDismissFrameLayout by bindView(R.id.post_container)
    private lateinit var chromeFader: ElasticDragDismissFrameLayout.SystemChromeFader
    private val background: View by bindView(R.id.background)
    private val toolbar: Toolbar by bindView(R.id.story_toolbar)
    private var fabExpandDuration: Int = 0
    private var threadWidth: Int = 0
    private var threadGap: Int = 0

    private var story: Post? = null
    private var markdown: Bypass? = null
    private lateinit var customTab: CustomTabActivityHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_hunt)
        setActionBar(toolbar)

        toolbar.setNavigationOnClickListener(backClick)

        fabExpandDuration = resources.getInteger(R.integer.fab_expand_duration)
        threadWidth = resources.getDimensionPixelSize(R.dimen.comment_thread_width)
        threadGap = resources.getDimensionPixelSize(R.dimen.comment_thread_gap)

        chromeFader = ElasticDragDismissFrameLayout.SystemChromeFader(this)
        markdown = Bypass(this, Bypass.Options()
                .setBlockQuoteLineColor(
                        ContextCompat.getColor(this, R.color.designer_news_quote_line))
                .setBlockQuoteLineWidth(2) // dps
                .setBlockQuoteLineIndent(8) // dps
                .setPreImageLinebreakHeight(4) //dps
                .setBlockQuoteIndentSize(TypedValue.COMPLEX_UNIT_DIP, 2f)
                .setBlockQuoteTextColor(ContextCompat.getColor(this, R.color.designer_news_quote)))

        customTab = CustomTabActivityHelper()
        customTab.setConnectionCallback(customTabConnect)

        val intent = intent
        if (intent.hasExtra(EXTRA_PRODUCT_HUNT)) {
            story = intent.getParcelableExtra(EXTRA_PRODUCT_HUNT)
            if (story != null) {
                bindProductHunt()
            }
        }

    }

    private val customTabConnect = object : CustomTabActivityHelper.ConnectionCallback {

        override fun onCustomTabsConnected() {
            if (story != null && !story!!.discussion_url.isNullOrEmpty()) {
                customTab.mayLaunchUrl(Uri.parse(story!!.discussion_url), null, null)
            }
        }

        override fun onCustomTabsDisconnected() {}
    }

    private val backClick: View.OnClickListener = View.OnClickListener { finishAfterTransition() }

    override fun onPause() {
        draggableFrame.removeListener(chromeFader)
        super.onPause()
    }

    override fun onStop() {
        customTab.unbindCustomTabsService(this)
        super.onStop()
    }

    override fun onDestroy() {
        customTab.setConnectionCallback(null)
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        customTab.bindCustomTabsService(this)
    }

    override fun onResume() {
        super.onResume()
        // clean up after any fab expansion
        fab.alpha = 1f
        fabExpand.visibility = View.INVISIBLE
        draggableFrame.addListener(chromeFader)
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onProvideAssistContent(outContent: AssistContent) {
        outContent.webUri = Uri.parse(story!!.url)
    }

    //todo: bin details here
    private fun bindProductHunt() {
        //Set title
        if (!story?.name.isNullOrEmpty()) {
            val textView = toolbar.getChildAt(0) as? TextView
            if (textView != null) {
                textView.typeface = Typeface.createFromAsset(assets, "fonts/nunito_semibold.ttf")
                HtmlUtils.parseMarkdownAndSetText(textView, story?.name, markdown
                ) { src, loadingSpan ->
                    GlideApp.with(this@ProductHunt)
                            .asBitmap()
                            .load(src)
                            .transition(BitmapTransitionOptions.withCrossFade())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(ImageSpanTarget(textView, loadingSpan))
                }
            }
        }

        MaterialDialog.Builder(this)
                .theme(Theme.DARK)
                .content(story.toString())
                .positiveText("Cancel")
                .onPositive({ dialog, _ ->
                    dialog.dismiss()
                }).build().show()

        fab.setOnClickListener({
            doFabExpand()
            CustomTabActivityHelper.openCustomTab(
                    this@ProductHunt,
                    CustomTabsIntent.Builder(customTab.session)
                            .setToolbarColor(ContextCompat.getColor(this@ProductHunt,
                                    R.color.product_hunt)).addDefaultShareMenuItem().build(),
                    Uri.parse(story?.discussion_url))
        })

    }

    private fun doFabExpand() {
        // translate the chrome placeholder ui so that it is centered on the FAB
        val fabCenterX = (fab.left + fab.right) / 2
        val fabCenterY = (fab.top + fab.bottom) / 2 - fabExpand.top
        val translateX = fabCenterX - fabExpand.width / 2
        val translateY = fabCenterY - fabExpand.height / 2
        fabExpand.translationX = translateX.toFloat()
        fabExpand.translationY = translateY.toFloat()

        // then reveal the placeholder ui, starting from the center & same dimens as fab
        fabExpand.visibility = View.VISIBLE
        val reveal = ViewAnimationUtils.createCircularReveal(
                fabExpand,
                fabExpand.width / 2,
                fabExpand.height / 2,
                (fab.width / 2).toFloat(),
                Math.hypot((fabExpand.width / 2).toDouble(), (fabExpand.height / 2).toDouble()).toInt().toFloat())
                .setDuration(fabExpandDuration.toLong())

        // translate the placeholder ui back into position along an arc
        val arcMotion = GravityArcMotion()
        arcMotion.minimumVerticalAngle = 70f
        val motionPath = arcMotion.getPath(translateX.toFloat(), translateY.toFloat(), 0f, 0f)
        val position = ObjectAnimator.ofFloat(fabExpand, View.TRANSLATION_X, View
                .TRANSLATION_Y, motionPath)
                .setDuration(fabExpandDuration.toLong())

        // animate from the FAB colour to the placeholder background color
        val background = ObjectAnimator.ofArgb(fabExpand,
                ViewUtils.BACKGROUND_COLOR,
                ContextCompat.getColor(this, R.color.product_hunt),
                ContextCompat.getColor(this, R.color.background_light))
                .setDuration(fabExpandDuration.toLong())

        // fade out the fab (rapidly)
        val fadeOutFab = ObjectAnimator.ofFloat(fab, View.ALPHA, 0f)
                .setDuration(60)

        // play 'em all together with the material interpolator
        val show = AnimatorSet()
        show.interpolator = AnimUtils.getFastOutSlowInInterpolator(this@ProductHunt)
        show.playTogether(reveal, background, position, fadeOutFab)
        show.start()
    }

    //Adapter for recyclerview
    internal inner class ProductHuntAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getItemCount(): Int {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    internal inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    internal inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        const val EXTRA_PRODUCT_HUNT = "EXTRA_PRODUCT_HUNT"
    }

}
