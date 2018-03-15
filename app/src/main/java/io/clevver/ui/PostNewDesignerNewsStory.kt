/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.clevver.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.ShareCompat
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import io.clevver.R
import io.clevver.data.api.designernews.PostStoryService
import io.clevver.data.prefs.DesignerNewsPrefs
import io.clevver.ui.transitions.FabTransform
import io.clevver.ui.transitions.MorphTransform
import io.clevver.ui.widget.BottomSheet
import io.clevver.ui.widget.ObservableScrollView
import io.clevver.util.AnimUtils
import io.clevver.util.ImeUtils
import io.clevver.util.ShortcutHelper
import io.clevver.util.bindView


/**
 * Post new Designer news
 */
class PostNewDesignerNewsStory : Activity() {

    private val bottomSheet: BottomSheet by bindView(R.id.bottom_sheet)
    private val bottomSheetContent: ViewGroup by bindView(R.id.bottom_sheet_content)
    private val sheetTitle: TextView by bindView(R.id.title)
    private val scrollContainer: ObservableScrollView by bindView(R.id.scroll_container)
    private val title: EditText by bindView(R.id.new_story_title)
    private val urlLabel: TextInputLayout by bindView(R.id.new_story_url_label)
    private val url: EditText by bindView(R.id.new_story_url)
    private val commentLabel: TextInputLayout by bindView(R.id.new_story_comment_label)
    private val comment: EditText by bindView(R.id.new_story_comment)
    private val post: Button by bindView(R.id.new_story_post)
    private var appBarElevation: Float = 0.toFloat()

    private val isShareIntent: Boolean
        get() = intent != null && Intent.ACTION_SEND == intent.action

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_new_designer_news_story)
        if (!FabTransform.setup(this, bottomSheetContent)) {
            MorphTransform.setup(this, bottomSheetContent,
                    ContextCompat.getColor(this, R.color.background_light), 0)
        }

        appBarElevation = resources.getDimension(R.dimen.z_app_bar)

        bottomSheet!!.registerCallback(object : BottomSheet.Callbacks() {
            override fun onSheetDismissed() {
                // After a drag dismiss, finish without the shared element return transition as
                // it no longer makes sense.  Let the launching window know it's a drag dismiss so
                // that it can restore any UI used as an entering shared element
                setResult(RESULT_DRAG_DISMISSED)
                finish()
            }
        })

        scrollContainer!!.setListener { scrollY ->
            if (scrollY != 0 && sheetTitle!!.translationZ != appBarElevation) {
                sheetTitle!!.animate()
                        .translationZ(appBarElevation)
                        .setStartDelay(0L)
                        .setDuration(80L)
                        .setInterpolator(AnimUtils.getFastOutSlowInInterpolator(this@PostNewDesignerNewsStory))
                        .start()
            } else if (scrollY == 0 && sheetTitle!!.translationZ == appBarElevation) {
                sheetTitle!!.animate()
                        .translationZ(0f)
                        .setStartDelay(0L)
                        .setDuration(80L)
                        .setInterpolator(AnimUtils.getFastOutSlowInInterpolator(this@PostNewDesignerNewsStory))
                        .start()
            }
        }

        // check for share intent
        if (isShareIntent) {
            val intentReader = ShareCompat.IntentReader.from(this)
            url!!.setText(intentReader.text)
            title!!.setText(intentReader.subject)
        }
        if (!hasSharedElementTransition()) {
            // when launched from share or app shortcut there is no shared element transition so
            // animate up the bottom sheet to establish the spatial model i.e. that it can be
            // dismissed downward
            overridePendingTransition(R.anim.post_story_enter, R.anim.post_story_exit)
            bottomSheetContent!!.viewTreeObserver.addOnPreDrawListener(
                    object : ViewTreeObserver.OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            bottomSheetContent!!.viewTreeObserver.removeOnPreDrawListener(this)
                            bottomSheetContent!!.translationY = bottomSheetContent!!.height.toFloat()
                            bottomSheetContent!!.animate()
                                    .translationY(0f)
                                    .setStartDelay(120L)
                                    .setDuration(240L).interpolator = AnimUtils.getLinearOutSlowInInterpolator(this@PostNewDesignerNewsStory)
                            return false
                        }
                    })
        }
        ShortcutHelper.reportPostUsed(this)

        bottomSheet.setOnClickListener({ dismiss() })

        comment.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                commentTextChanged(s.toString())
            }
        })

        url.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                urlTextChanged(s.toString())
            }
        })

        title.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                titleTextChanged(s.toString())
            }
        })

        post.setOnClickListener({ postNewStory() })

        url.setOnEditorActionListener({ v, actionId, event ->
            onEditorAction(v, actionId, event)
            return@setOnEditorActionListener true
        })

        comment.setOnEditorActionListener({ v, actionId, event ->
            onEditorAction(v, actionId, event)
            return@setOnEditorActionListener true
        })

    }

    override fun onPause() {
        // customize window animations
        overridePendingTransition(R.anim.post_story_enter, R.anim.post_story_exit)
        super.onPause()
    }

    override fun onBackPressed() {
        dismiss()
    }


    private fun dismiss() {
        if (!hasSharedElementTransition()) {
            bottomSheetContent!!.animate()
                    .translationY(bottomSheetContent!!.height.toFloat())
                    .setDuration(160L)
                    .setInterpolator(AnimUtils.getFastOutLinearInInterpolator(this@PostNewDesignerNewsStory))
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            finish()
                        }
                    })
        } else {
            finishAfterTransition()
        }
    }

    protected fun titleTextChanged(text: CharSequence) {
        setPostButtonState()
    }

    protected fun urlTextChanged(text: CharSequence) {
        val emptyUrl = TextUtils.isEmpty(text)
        comment!!.isEnabled = emptyUrl
        commentLabel!!.isEnabled = emptyUrl
        comment!!.isFocusableInTouchMode = emptyUrl
        setPostButtonState()
    }

    private fun commentTextChanged(text: CharSequence) {
        val emptyComment = TextUtils.isEmpty(text)
        url!!.isEnabled = emptyComment
        urlLabel!!.isEnabled = emptyComment
        url!!.isFocusableInTouchMode = emptyComment
        setPostButtonState()
    }

    private fun postNewStory() {
        if (DesignerNewsPrefs.get(this).isLoggedIn) {
            ImeUtils.hideIme(title!!)
            val postIntent = Intent(PostStoryService.ACTION_POST_NEW_STORY, null,
                    this, PostStoryService::class.java)
            postIntent.putExtra(PostStoryService.EXTRA_STORY_TITLE, title!!.text.toString())
            postIntent.putExtra(PostStoryService.EXTRA_STORY_URL, url!!.text.toString())
            postIntent.putExtra(PostStoryService.EXTRA_STORY_COMMENT, comment!!.text.toString())
            postIntent.putExtra(PostStoryService.EXTRA_BROADCAST_RESULT,
                    intent.getBooleanExtra(PostStoryService.EXTRA_BROADCAST_RESULT, false))
            startService(postIntent)
            setResult(RESULT_POSTING)
            finishAfterTransition()
        } else {
            //todo: designer news login
           /* val login = Intent(this, DesignerNewsLogin::class.java)
            MorphTransform.addExtras(login, ContextCompat.getColor(this, R.color.designer_news), 0)
            val options = ActivityOptions.makeSceneTransitionAnimation(
                    this, post, getString(R.string.transition_designer_news_login))
            startActivity(login, options.toBundle())*/
        }
    }

    private fun hasSharedElementTransition(): Boolean {
        val transition = window.sharedElementEnterTransition
        return transition != null && !transition.targets.isEmpty()
    }

    private fun setPostButtonState() {
        post!!.isEnabled = !TextUtils.isEmpty(title!!.text) && (!TextUtils.isEmpty(url!!.text) || !TextUtils.isEmpty(comment!!.text))
    }

    private fun onEditorAction(textView: TextView, actionId: Int, event: KeyEvent): Boolean {
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            postNewStory()
            return true
        }
        return false
    }

    companion object {

        const val RESULT_DRAG_DISMISSED = 3
        const val RESULT_POSTING = 4
    }

}
