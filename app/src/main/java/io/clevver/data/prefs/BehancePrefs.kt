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
import io.clevver.data.api.behance.BehanceService
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
 * Storing behance user state.
 */
class BehancePrefs private constructor(private val context: Context) {
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
    private var api: BehanceService? = null
    private var loginStatusListeners: MutableList<BehanceLoginStatusListener>? = null

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

    interface BehanceLoginStatusListener {
        fun onBehanceLogin()
        fun onBehanceLogout()
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

    fun getApi(): BehanceService {
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
        Toast.makeText(context, R.string.behance_logged_out,
                Toast.LENGTH_SHORT).show()
    }

    fun login(context: Activity, callback: Int) {
        context.startActivityForResult(Intent(Intent.ACTION_VIEW, Uri.parse(LOGIN_URL)), callback)
    }

    fun addLoginStatusListener(listener: BehanceLoginStatusListener) {
        if (loginStatusListeners == null) {
            loginStatusListeners = ArrayList(0)
        }
        loginStatusListeners!!.add(listener)
    }

    fun removeLoginStatusListener(listener: BehanceLoginStatusListener) {
        if (loginStatusListeners != null) {
            loginStatusListeners!!.remove(listener)
        }
    }

    private fun dispatchLoginEvent() {
        if (loginStatusListeners != null && !loginStatusListeners!!.isEmpty()) {
            for (listener in loginStatusListeners!!) {
                listener.onBehanceLogin()
            }
        }
    }

    private fun dispatchLogoutEvent() {
        if (loginStatusListeners != null && !loginStatusListeners!!.isEmpty()) {
            for (listener in loginStatusListeners!!) {
                listener.onBehanceLogout()
            }
        }
    }

    private fun createApi() {
        val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(getAccessToken()))
        val gson = GsonBuilder()
                .setDateFormat(DribbbleService.DATE_FORMAT)
                .create()

        val cachedClient = client.cache(provideCache())
                .addInterceptor(provideForcedOfflineCacheInterceptor())
                .build()

        api = Retrofit.Builder()
                .baseUrl(ClevverUtils.BEHANCE_API)
                .client(cachedClient)
                .addConverterFactory(DenvelopingConverter(gson))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(BehanceService::class.java)
    }

    private fun provideForcedOfflineCacheInterceptor(): Interceptor {
        return Interceptor { chain ->
            var request = chain.request()
            val cacheControl: CacheControl = CacheControl.Builder()
                    .maxStale(7, TimeUnit.DAYS)
                    .build()

            request = request.newBuilder()
                    .removeHeader(HEADER_PRAGMA)
                    .removeHeader(HEADER_CACHE_CONTROL)
                    .cacheControl(cacheControl)
                    .build()

            chain.proceed(request)
        }
    }

    private fun provideCache(): Cache {
        if (cache == null) {
            try {
                //10 MB cache
                cache = Cache(File(context.cacheDir, "http-cache"), 10.times(1024).times(1024))
            } catch (ex: Exception) {
            }
        }
        return cache!!
    }

    private fun getAccessToken(): String {
        return if (accessToken.isNullOrEmpty()) BuildConfig.DRIBBBLE_CLIENT_ACCESS_TOKEN else accessToken!!
    }

    companion object {
        const val LOGIN_URL = ClevverUtils.LOGIN_ENDPOINT_BEHANCE
        private const val DRIBBBLE_PREF = "DRIBBBLE_PREF"
        private const val KEY_ACCESS_TOKEN = "KEY_ACCESS_TOKEN"
        private const val KEY_USER_ID = "KEY_USER_ID"
        private const val KEY_USER_NAME = "KEY_USER_NAME"
        private const val KEY_USER_USERNAME = "KEY_USER_USERNAME"
        private const val KEY_USER_AVATAR = "KEY_USER_AVATAR"
        private const val KEY_USER_TYPE = "KEY_USER_TYPE"
        private const val HEADER_CACHE_CONTROL = "Cache-control"
        private const val HEADER_PRAGMA = "Pragma"

        private val CREATIVE_TYPES = Arrays.asList("Player", "Team")

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var singleton: BehancePrefs? = null

        operator fun get(context: Context): BehancePrefs {
            if (singleton == null) {
                synchronized(BehancePrefs::class.java) {
                    singleton = BehancePrefs(context)
                }
            }
            return singleton!!
        }
    }

}
