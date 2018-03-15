/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import io.clevver.BuildConfig
import io.clevver.R
import io.clevver.api.ClevverUtils
import io.clevver.data.api.dribbble.DribbbleAuthService
import io.clevver.data.api.dribbble.model.AccessToken
import io.clevver.data.api.dribbble.model.User
import io.clevver.data.prefs.DribbblePrefs
import io.clevver.ui.transitions.FabTransform
import io.clevver.ui.transitions.MorphTransform
import io.clevver.util.ScrimUtil
import io.clevver.util.bindView
import io.clevver.util.glide.GlideApp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

@SuppressLint("GoogleAppIndexingApiWarning")
/**
 * Dribbble login for existing users
 */
class DribbbleLogin : Activity() {

    private var isDismissing = false
    private var isLoginFailed = false
    private val container: ViewGroup by bindView(R.id.container)
    private val background: ViewGroup by bindView(R.id.background)
    private val message: TextView by bindView(R.id.login_message)
    private val login: Button by bindView(R.id.login)
    private val loading: ProgressBar by bindView(R.id.loading)
    private val loginFailed: TextView by bindView(R.id.login_failed_message)

    private lateinit var dribbblePrefs: DribbblePrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dribbble_login)

        loading.visibility = View.GONE
        dribbblePrefs = DribbblePrefs[this]

        if (!FabTransform.setup(this, container)) {
            MorphTransform.setup(this, container,
                    ContextCompat.getColor(this, R.color.background_light),
                    resources.getDimensionPixelSize(R.dimen.dialog_corners))
        }

        if (savedInstanceState != null) {
            isLoginFailed = savedInstanceState.getBoolean(STATE_LOGIN_FAILED, false)
            loginFailed.visibility = if (isLoginFailed) View.VISIBLE else View.GONE
        }

        login.setOnClickListener({ doLogin() })
        background.setOnClickListener({ dismiss() })
    }

    override fun onResume() {
        super.onResume()
        checkAuthCallback(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update getIntent() to this new Intent
        setIntent(intent)
    }

    private fun doLogin() {
        showLoading()
        dribbblePrefs.login(this@DribbbleLogin)
    }

    private fun dismiss() {
        isDismissing = true
        setResult(Activity.RESULT_CANCELED)
        finishAfterTransition()
    }

    override fun onBackPressed() {
        dismiss()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBoolean(STATE_LOGIN_FAILED, isLoginFailed)
    }

    internal fun showLoginFailed() {
        isLoginFailed = true
        showLogin()
        loginFailed.visibility = View.VISIBLE
    }

    internal fun showLoggedInUser(token: AccessToken?) {
        val authenticatedUser = dribbblePrefs.getApi().authenticatedUser()
        authenticatedUser.enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                val user = response.body()
                if (user?.id == null) {
                    dribbblePrefs.getApi().getUser(token!!)
                            .enqueue(object : Callback<User?> {
                                override fun onFailure(call: Call<User?>?, t: Throwable?) {
                                    showLoginFailed()
                                    Toast.makeText(applicationContext, t?.localizedMessage, Toast.LENGTH_SHORT)
                                            .show()
                                }

                                override fun onResponse(call: Call<User?>?, response: Response<User?>?) {
                                    if (response != null) {
                                        val signedInUser = response.body()
                                        showToast(signedInUser)
                                    }
                                }
                            })
                } else {
                    showToast(user)
                }
            }

            override fun onFailure(call: Call<User>, t: Throwable?) {
                showLoginFailed()
                Toast.makeText(applicationContext, t?.localizedMessage, Toast.LENGTH_SHORT)
                        .show()
            }
        })
    }

    private fun showToast(user: User?) {
        if (user == null) dribbblePrefs.setLoggedInUser(user) else dribbblePrefs.user = user
        val confirmLogin = Toast(applicationContext)
        val v = LayoutInflater.from(this@DribbbleLogin).inflate(R.layout
                .toast_logged_in_confirmation, null, false)
        (v.findViewById<View>(R.id.name) as TextView).text = user?.name?.toLowerCase()
        // need to use app context here as the activity will be destroyed shortly
        GlideApp.with(applicationContext)
                .load(user?.avatar_url)
                .placeholder(R.drawable.ic_player)
                .circleCrop()
                .transition(withCrossFade())
                .into(v.findViewById<View>(R.id.avatar) as ImageView)
        v.findViewById<View>(R.id.scrim).background = ScrimUtil.makeCubicGradientScrimDrawable(ContextCompat.getColor(this@DribbbleLogin,
                R.color.scrim), 5, Gravity.BOTTOM)
        confirmLogin.view = v
        confirmLogin.setGravity(Gravity.BOTTOM or Gravity.FILL_HORIZONTAL, 0, 0)
        confirmLogin.duration = Toast.LENGTH_LONG
        confirmLogin.show()
    }

    private fun showLoading() {
        TransitionManager.beginDelayedTransition(container)
        message.visibility = View.GONE
        login.visibility = View.GONE
        loginFailed.visibility = View.GONE
        loading.visibility = View.VISIBLE
    }

    private fun showLogin() {
        TransitionManager.beginDelayedTransition(container)
        message.visibility = View.VISIBLE
        login.visibility = View.VISIBLE
        loading.visibility = View.GONE
    }

    private fun checkAuthCallback(intent: Intent?) {
        if (intent != null
                && intent.data != null
                && !TextUtils.isEmpty(intent.data!!.authority)
                && ClevverUtils.LOGIN_CALLBACK == intent.data!!.authority) {
            showLoading()
            val code = intent.data?.getQueryParameter("code")
            if (code.isNullOrEmpty()) {
                Timber.d("Code is null or empty")
                showLoginFailed()
                Toast.makeText(applicationContext, "Access code not found", Toast.LENGTH_SHORT)
                        .show()
            } else {
                getAccessToken(code!!)
            }
        }
    }

    private fun getAccessToken(code: String) {
        val dribbbleAuthApi = Retrofit.Builder()
                .baseUrl(ClevverUtils.DRIBBBLE_AUTH_SERVICE_API)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DribbbleAuthService::class.java)

        val accessTokenCall = dribbbleAuthApi.getAccessToken(BuildConfig.DRIBBBLE_CLIENT_ID,
                BuildConfig.DRIBBBLE_CLIENT_SECRET, code)
        accessTokenCall.enqueue(object : Callback<AccessToken> {
            override fun onResponse(call: Call<AccessToken>, response: Response<AccessToken>) {
                if (response.body() == null) {
                    showLoginFailed()
                    Toast.makeText(applicationContext, response.message(), Toast.LENGTH_SHORT)
                            .show()
                    return
                }
                isLoginFailed = false
                dribbblePrefs.setAccessToken(response.body()!!.access_token)
                showLoggedInUser(response.body())
                setResult(Activity.RESULT_OK)
                finishAfterTransition()
            }

            override fun onFailure(call: Call<AccessToken>, t: Throwable?) {
                Timber.e(t)
                showLoginFailed()
                Toast.makeText(applicationContext, t?.localizedMessage, Toast.LENGTH_SHORT)
                        .show()
            }
        })
    }

    companion object {
        private const val STATE_LOGIN_FAILED = "STATE_LOGIN_FAILED"
    }
}
