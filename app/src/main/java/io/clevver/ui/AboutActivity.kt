/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui

import `in`.uncod.android.bypass.Bypass
import android.app.Activity
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.RecyclerView
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AlignmentSpan
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.target.Target
import io.clevver.R
import io.clevver.ui.widget.ElasticDragDismissFrameLayout
import io.clevver.ui.widget.InkPageIndicator
import io.clevver.util.HtmlUtils
import io.clevver.util.bindView
import io.clevver.util.customtabs.CustomTabActivityHelper
import io.clevver.util.glide.GlideApp
import java.security.InvalidParameterException

/**
 * About screen. This displays 3 pages in a ViewPager:
 * – About Plaid
 * – Credit Roman for the awesome icon
 * – Credit libraries
 */
class AboutActivity : Activity() {

    private val draggableFrame: ElasticDragDismissFrameLayout by bindView(R.id.draggable_frame)
    private val pager: ViewPager by bindView(R.id.pager)
    private val pageIndicator: InkPageIndicator by bindView(R.id.indicator)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        pager.adapter = AboutPagerAdapter(this@AboutActivity)
        pager.pageMargin = resources.getDimensionPixelSize(R.dimen.spacing_normal)
        pageIndicator.setViewPager(pager)

        draggableFrame.addListener(
                object : ElasticDragDismissFrameLayout.SystemChromeFader(this) {
                    override fun onDragDismissed() {
                        // if we drag dismiss downward then the default reversal of the enter
                        // transition would slide content upward which looks weird. So reverse it.
                        if (draggableFrame.translationY > 0) {
                            window.returnTransition = TransitionInflater.from(this@AboutActivity)
                                    .inflateTransition(R.transition.about_return_downward)
                        }
                        finishAfterTransition()
                    }
                })
    }

    internal class AboutPagerAdapter(private val host: Activity) : PagerAdapter() {

        private var aboutPlaid: View? = null
        var plaidDescription: TextView? = null
        private var aboutIcon: View? = null
        var iconDescription: TextView? = null
        private var aboutLibs: View? = null
        private var aboutDev: View? = null
        var devDescription: TextView? = null
        var nickAvatar: ImageView? = null
        var quabynahAvatar: ImageView? = null
        private var libsList: RecyclerView? = null

        private val layoutInflater: LayoutInflater = LayoutInflater.from(host)
        private val markdown: Bypass = Bypass(host, Bypass.Options())
        private val resources: Resources = host.resources

        override fun instantiateItem(collection: ViewGroup, position: Int): Any {
            val layout = getPage(position, collection)
            collection.addView(layout)
            return layout
        }

        override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
            collection.removeView(view as View)
        }

        override fun getCount(): Int {
            return 4
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view == `object` as View
        }

        private fun getPage(position: Int, parent: ViewGroup): View {
            when (position) {
                0 -> {
                    if (aboutPlaid == null) {
                        aboutPlaid = layoutInflater.inflate(R.layout.about_plaid, parent, false)
                        plaidDescription = aboutPlaid!!.findViewById(R.id.about_description)
                        // fun with spans & markdown
                        val about0 = markdown.markdownToSpannable(resources
                                .getString(R.string.about_plaid_0), plaidDescription, null)
                        val about1 = SpannableString(
                                resources.getString(R.string.about_plaid_1))
                        about1.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                                0, about1.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        val about2 = SpannableString(markdown.markdownToSpannable(resources.getString(R.string.about_plaid_2),
                                plaidDescription, null))
                        about2.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                                0, about2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        val about3 = SpannableString(markdown.markdownToSpannable(resources.getString(R.string.about_plaid_3),
                                plaidDescription, null))
                        about3.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                                0, about3.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        val desc = TextUtils.concat(about0, "\n\n", about1, "\n", about2,
                                "\n\n", about3)
                        HtmlUtils.setTextWithNiceLinks(plaidDescription, desc)
                    }
                    return aboutPlaid!!
                }
                1 -> {
                    if (aboutIcon == null) {
                        aboutIcon = layoutInflater.inflate(R.layout.about_icon, parent, false)
                        iconDescription = aboutIcon!!.findViewById(R.id.icon_description)

                        val icon0 = resources.getString(R.string.about_icon_0)
                        val icon1 = markdown.markdownToSpannable(resources
                                .getString(R.string.about_icon_1), iconDescription, null)
                        val iconDesc = TextUtils.concat(icon0, "\n", icon1)
                        HtmlUtils.setTextWithNiceLinks(iconDescription, iconDesc)
                    }
                    return aboutIcon!!
                }
                2 -> {
                    if (aboutLibs == null) {
                        aboutLibs = layoutInflater.inflate(R.layout.about_libs, parent, false)
                        libsList = aboutLibs!!.findViewById(R.id.libs_list)
                        libsList!!.adapter = LibraryAdapter(host)
                    }
                    return aboutLibs!!
                }
                3 -> {
                    if (aboutDev == null) {
                        aboutDev = layoutInflater.inflate(R.layout.about_dev, parent, false)
                        devDescription = aboutDev!!.findViewById(R.id.dev_description)
                        nickAvatar = aboutDev!!.findViewById(R.id.nick_avatar)
                        quabynahAvatar = aboutDev!!.findViewById(R.id.quabynah_avatar)
                        val spannable = markdown.markdownToSpannable(resources.getString(R.string.about_dev_info)
                                , devDescription, null)
                        HtmlUtils.setTextWithNiceLinks(devDescription, spannable)

                        //todo: set profile pictures here
                        GlideApp.with(host.applicationContext)
                                .load(resources.getString(R.string.nick_url))
                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                .circleCrop()
                                .placeholder(R.drawable.avatar_placeholder)
                                .error(R.drawable.avatar_placeholder)
                                .fallback(R.drawable.avatar_placeholder)
                                .into(nickAvatar!!)

                        GlideApp.with(host.applicationContext)
                                .load(resources.getString(R.string.quabynah_url))
                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                .circleCrop()
                                .placeholder(R.drawable.avatar_placeholder)
                                .error(R.drawable.avatar_placeholder)
                                .fallback(R.drawable.avatar_placeholder)
                                .into(quabynahAvatar!!)
                    }
                    return aboutDev!!
                }
            }
            throw InvalidParameterException()
        }
    }

    private class LibraryAdapter internal constructor(internal val host: Activity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            when (viewType) {
                VIEW_TYPE_INTRO -> return LibraryIntroHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.about_lib_intro, parent, false))
                VIEW_TYPE_LIBRARY -> return createLibraryHolder(parent)
            }
            throw InvalidParameterException()
        }

        private fun createLibraryHolder(parent: ViewGroup): LibraryHolder {
            val holder = LibraryHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.library, parent, false))
            val clickListener: View.OnClickListener = View.OnClickListener {
                val position = holder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return@OnClickListener
                CustomTabActivityHelper.openCustomTab(
                        host,
                        CustomTabsIntent.Builder()
                                .setToolbarColor(ContextCompat.getColor(host, R.color.primary))
                                .addDefaultShareMenuItem()
                                .build(), Uri.parse(libs[position - 1].link))
            }
            holder.itemView.setOnClickListener(clickListener)
            holder.link!!.setOnClickListener(clickListener)
            return holder
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (getItemViewType(position) == VIEW_TYPE_LIBRARY) {
                bindLibrary(holder as LibraryHolder, libs[position - 1]) // adjust for intro
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) VIEW_TYPE_INTRO else VIEW_TYPE_LIBRARY
        }

        override fun getItemCount(): Int {
            return libs.size + 1 // + 1 for the static intro view
        }

        private fun bindLibrary(holder: LibraryHolder, lib: Library) {
            holder.name!!.text = lib.name
            holder.description!!.text = lib.description
            val request = GlideApp.with(holder.image!!.context)
                    .load(lib.imageUrl)
                    .transition(withCrossFade())
                    .placeholder(R.drawable.avatar_placeholder)
            if (lib.circleCrop) {
                request.circleCrop()
            }
            request.into(holder.image!!)
        }

        companion object {

            private const val VIEW_TYPE_INTRO = 0
            private const val VIEW_TYPE_LIBRARY = 1
            internal val libs = arrayOf(
                    Library("Android support libraries",
                            "The Android support libraries offer a number of features that are not built into the framework.",
                            "https://developer.android.com/topic/libraries/support-library",
                            "https://developer.android.com/images/android_icon_125.png",
                            false),
                    Library("ButterKnife",
                            "Bind Android views and callbacks to fields and methods.",
                            "http://jakewharton.github.io/butterknife/",
                            "https://avatars.githubusercontent.com/u/66577",
                            true),
                    Library("Bypass",
                            "Skip the HTML, Bypass takes markdown and renders it directly.",
                            "https://github.com/Uncodin/bypass",
                            "https://avatars.githubusercontent.com/u/1072254",
                            true),
                    Library("Firebase",
                            "Firebase is a wonderful API backend service developed by Google",
                            "https://firebase.google.com/",
                            "https://firebase.google.com/lockup.jpg",
                            true),
                    Library("Material Dialog",
                            "A beautiful, fluid, and customizable dialogs API",
                            "https://github.com/afollestad/material-dialogs",
                            "https://github.com/afollestad/",
                            true),
                    Library("Timber",
                            "Logging for lazy people.",
                            "http://jakewharton.github.io/timber/",
                            "https://avatars.githubusercontent.com/u/82592",
                            false),
                    Library("Flipboard Bottomsheet",
                            "Wonderful material bottomsheet for splendid animations",
                            "https://github.com/Flipboard/bottomsheet/",
                            "https://avatars.githubusercontent.com/u/82592",
                            false),
                    Library("Uber for Developers",
                            "Uber API point for developers",
                            "http://jakewharton.github.io/timber/",
                            "https://avatars.githubusercontent.com/u/82592",
                            false),
                    Library("Facebook for Developers",
                            "Facebook API point for developers",
                            "http://developer.facebook.com/",
                            "https://avatars.githubusercontent.com/u/82592",
                            false),
                    Library("Twitter for Developers",
                            "Twitter API point for developers",
                            "http://dev.twitter.com/",
                            "https://avatars.githubusercontent.com/u/82592",
                            true),
                    Library("Glide", "An image loading and caching library for Android focused on smooth scrolling.",
                            "https://github.com/bumptech/glide/",
                            "https://avatars.githubusercontent.com/u/423539",
                            false),
                    Library("JSoup",
                            "Java HTML Parser, with best of DOM, CSS, and jquery.",
                            "https://github.com/jhy/jsoup/",
                            "https://avatars.githubusercontent.com/u/76934",
                            true),
                    Library("OkHttp",
                            "An HTTP & HTTP/2 client for Android and Java applications.",
                            "http://square.github.io/okhttp/",
                            "https://avatars.githubusercontent.com/u/82592",
                            false),
                    Library("Retrofit",
                            "A type-safe HTTP client for Android and Java.",
                            "http://square.github.io/retrofit/",
                            "https://avatars.githubusercontent.com/u/82592",
                            false))
        }
    }

    internal class LibraryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var image: ImageView? = null
        var name: TextView? = null
        var description: TextView? = null
        var link: Button? = null

        init {
            image = itemView.findViewById(R.id.library_image)
            name = itemView.findViewById(R.id.library_name)
            description = itemView.findViewById(R.id.library_description)
            link = itemView.findViewById(R.id.library_link)
        }
    }

    internal class LibraryIntroHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var intro: TextView = itemView as TextView

    }

    /**
     * Models an open source library we want to credit
     */
    private class Library internal constructor(internal val name: String, internal val description: String, internal val link: String, internal val imageUrl: String, internal val circleCrop: Boolean)

}
