/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.prefs

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.text.TextUtils
import android.widget.Toast
import com.google.gson.GsonBuilder
import io.clevver.BuildConfig
import io.clevver.R
import io.clevver.api.ClevverUtils
import io.clevver.data.api.AuthInterceptor
import io.clevver.data.api.DenvelopingConverter
import io.clevver.data.api.github.GithubService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

/**
 * Storing github user state.
 */
class GitHubPrefs private constructor(context: Context) {
    private val prefs: SharedPreferences

    private var accessToken: String? = null
    private var username: String? = null
    var isLoggedIn = false
        private set
    private var api: GithubService? = null
    private var loginStatusListeners: MutableList<AppLoginStatusListener>? = null

    init {
        prefs = context.applicationContext.getSharedPreferences(DRIBBBLE_PREF, Context
                .MODE_PRIVATE)
        accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        isLoggedIn = !TextUtils.isEmpty(accessToken)
        if (isLoggedIn) {
            accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
            username = prefs.getString(KEY_USERNAME, null)
        }
    }

    interface AppLoginStatusListener {
        fun onGithubLogin()
        fun onGithubLogout()
    }

    fun setAccessToken(accessToken: String) {
        if (accessToken.isNotEmpty()) {
            this.accessToken = accessToken
            isLoggedIn = true
            prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply()
            createApi()
            dispatchLoginEvent()
        }
    }

    fun setCurrentUser(username: String) {
        if (username.isNotEmpty()) {
            this.username = username
            prefs.edit().putString(KEY_USERNAME, username).apply()
            createApi()
            dispatchLoginEvent()
        }
    }

    fun getApi(): GithubService {
        if (api == null) createApi()
        return api!!
    }

    fun logout(context: Context) {
        isLoggedIn = false
        accessToken = null
        val editor = prefs.edit()
        editor.putString(KEY_ACCESS_TOKEN, null)
        editor.apply()
        createApi()
        dispatchLogoutEvent()
        Toast.makeText(context, R.string.behance_logged_out,
                Toast.LENGTH_SHORT).show()
    }

    fun login(context: Activity) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LOGIN_URL)))
    }

    fun addLoginStatusListener(listener: AppLoginStatusListener) {
        if (loginStatusListeners == null) {
            loginStatusListeners = ArrayList(0)
        }
        loginStatusListeners!!.add(listener)
    }

    fun removeLoginStatusListener(listener: AppLoginStatusListener) {
        if (loginStatusListeners != null) {
            loginStatusListeners!!.remove(listener)
        }
    }

    private fun dispatchLoginEvent() {
        if (loginStatusListeners != null && !loginStatusListeners!!.isEmpty()) {
            for (listener in loginStatusListeners!!) {
                listener.onGithubLogin()
            }
        }
    }

    private fun dispatchLogoutEvent() {
        if (loginStatusListeners != null && !loginStatusListeners!!.isEmpty()) {
            for (listener in loginStatusListeners!!) {
                listener.onGithubLogout()
            }
        }
    }

    private fun createApi() {
        val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(getAccessToken()))
                .build()
        val gson = GsonBuilder()
                .create()
        api = Retrofit.Builder()
                .baseUrl(ClevverUtils.GITHUB_ENDPOINT)
                .client(client)
                .addConverterFactory(DenvelopingConverter(gson))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(GithubService::class.java)
    }

    private fun getAccessToken(): String {
        return if (accessToken.isNullOrEmpty()) BuildConfig.DRIBBBLE_CLIENT_ACCESS_TOKEN else accessToken!!
    }

    companion object {
        const val LOGIN_URL = ClevverUtils.LOGIN_ENDPOINT_GITHUB
        private const val DRIBBBLE_PREF = "DRIBBBLE_PREF"
        private const val KEY_ACCESS_TOKEN = "KEY_ACCESS_TOKEN"
        private const val KEY_USERNAME = "KEY_USERNAME"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var singleton: GitHubPrefs? = null

        operator fun get(context: Context): GitHubPrefs {
            if (singleton == null) {
                synchronized(GitHubPrefs::class.java) {
                    singleton = GitHubPrefs(context)
                }
            }
            return singleton!!
        }
    }

}
