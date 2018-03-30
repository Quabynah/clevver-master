/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.prefs

import android.annotation.SuppressLint
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
import io.clevver.data.api.dribbble.DribbbleService
import io.clevver.data.api.dribbble.model.User
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Storing dribbble user state.
 */
class DribbblePrefs private constructor(private val context: Context) {
    private val prefs: SharedPreferences

    private var accessToken: String? = null
    private var cache: Cache? = null
    var isLoggedIn = false
        private set
    var userId: Long? = 0
        private set
    var userName: String? = null
        private set
    var userUsername: String? = null
        private set
    var userAvatar: String? = null
        private set
    private var userType: String? = null
    private var api: DribbbleService? = null
    private var loginStatusListeners: MutableList<DribbbleLoginStatusListener>? = null

    var user: User
        get() = User.Builder()
                .setId(if (userId == null) System.currentTimeMillis() else userId!!)
                .setName(userName)
                .setUsername(userUsername)
                .setAvatarUrl(userAvatar)
                .setType(userType)
                .build()
        set(value) {
            userName = value.name
            userUsername = value.username
            userId = value.id
            userAvatar = value.avatar_url
            userType = value.type
            val editor = prefs.edit()
            editor.putLong(KEY_USER_ID, if (userId == null) System.currentTimeMillis() else userId!!)
            editor.putString(KEY_USER_NAME, userName)
            editor.putString(KEY_USER_USERNAME, userUsername)
            editor.putString(KEY_USER_AVATAR, userAvatar)
            editor.putString(KEY_USER_TYPE, userType)
            editor.apply()
        }

    init {
        prefs = context.applicationContext.getSharedPreferences(DRIBBBLE_PREF, Context
                .MODE_PRIVATE)
        accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        isLoggedIn = !TextUtils.isEmpty(accessToken)
        if (isLoggedIn) {
            userId = prefs.getLong(KEY_USER_ID, 0L)
            userName = prefs.getString(KEY_USER_NAME, null)
            userUsername = prefs.getString(KEY_USER_USERNAME, null)
            userAvatar = prefs.getString(KEY_USER_AVATAR, null)
            userType = prefs.getString(KEY_USER_TYPE, null)
        }
    }

    interface DribbbleLoginStatusListener {
        fun onDribbbleLogin()
        fun onDribbbleLogout()
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

    fun setLoggedInUser(value: User?) {
        userName = value?.name
        userUsername = value?.username
        userId = value?.id
        userAvatar = value?.avatar_url
        userType = value?.type
        val editor = prefs.edit()
        editor.putLong(KEY_USER_ID, if (userId == null) System.currentTimeMillis() else userId!!)
        editor.putString(KEY_USER_NAME, userName)
        editor.putString(KEY_USER_USERNAME, userUsername)
        editor.putString(KEY_USER_AVATAR, userAvatar)
        editor.putString(KEY_USER_TYPE, userType)
        editor.apply()
    }

    fun userCanPost(): Boolean {
        return CREATIVE_TYPES.contains(userType)
    }

    fun getApi(): DribbbleService {
        if (api == null) createApi()
        return api!!
    }

    fun logout(context: Context) {
        isLoggedIn = false
        accessToken = null
        userId = 0L
        userName = null
        userUsername = null
        userAvatar = null
        userType = null
        val editor = prefs.edit()
        editor.putString(KEY_ACCESS_TOKEN, null)
        editor.putLong(KEY_USER_ID, 0L)
        editor.putString(KEY_USER_NAME, null)
        editor.putString(KEY_USER_AVATAR, null)
        editor.putString(KEY_USER_TYPE, null)
        editor.apply()
        createApi()
        dispatchLogoutEvent()
        Toast.makeText(context, R.string.dribbble_logged_out,
                Toast.LENGTH_SHORT).show()
    }

    fun login(context: Context) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LOGIN_URL)))
    }

    fun addLoginStatusListener(listener: DribbbleLoginStatusListener) {
        if (loginStatusListeners == null) {
            loginStatusListeners = ArrayList(0)
        }
        loginStatusListeners!!.add(listener)
    }

    fun removeLoginStatusListener(listener: DribbbleLoginStatusListener) {
        if (loginStatusListeners != null) {
            loginStatusListeners!!.remove(listener)
        }
    }

    private fun dispatchLoginEvent() {
        if (loginStatusListeners != null && loginStatusListeners!!.isNotEmpty()) {
            for (listener in loginStatusListeners!!) {
                listener.onDribbbleLogin()
            }
        }
    }

    private fun dispatchLogoutEvent() {
        if (loginStatusListeners != null && loginStatusListeners!!.isNotEmpty()) {
            for (listener in loginStatusListeners!!) {
                listener.onDribbbleLogout()
            }
        }
    }

    private fun createApi() {
        val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(getAccessToken()))
                .build()
        val gson = GsonBuilder()
                .setDateFormat(DribbbleService.DATE_FORMAT)
                .create()

        api = Retrofit.Builder()
                .baseUrl(ClevverUtils.DRIBBBLE_SEARCH_SERVICE_API)
                .client(client)
                .addConverterFactory(DenvelopingConverter(gson))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(DribbbleService::class.java)
    }

    fun getAccessToken(): String {
        return if (accessToken.isNullOrEmpty()) BuildConfig.DRIBBBLE_CLIENT_ACCESS_TOKEN else accessToken!!
    }

    companion object {
        const val LOGIN_URL = ClevverUtils.LOGIN_ENDPOINT_DRIBBBLE
        private const val DRIBBBLE_PREF = "DRIBBBLE_PREF"
        private const val KEY_ACCESS_TOKEN = "KEY_ACCESS_TOKEN"
        private const val KEY_USER_ID = "KEY_USER_ID"
        private const val KEY_USER_NAME = "KEY_USER_NAME"
        private const val KEY_USER_USERNAME = "KEY_USER_USERNAME"
        private const val KEY_USER_AVATAR = "KEY_USER_AVATAR"
        private const val KEY_USER_TYPE = "KEY_USER_TYPE"

        private val CREATIVE_TYPES = Arrays.asList("Player", "Team")

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var singleton: DribbblePrefs? = null

        operator fun get(context: Context): DribbblePrefs {
            if (singleton == null) {
                synchronized(DribbblePrefs::class.java) {
                    singleton = DribbblePrefs(context)
                }
            }
            return singleton!!
        }
    }

}
