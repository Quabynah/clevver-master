/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data

import android.content.Context
import android.net.ConnectivityManager
import com.google.gson.Gson
import io.clevver.BuildConfig
import io.clevver.api.ClevverUtils
import io.clevver.data.api.AuthInterceptor
import io.clevver.data.api.DenvelopingConverter
import io.clevver.data.api.dribbble.DribbbleSearchConverter
import io.clevver.data.api.dribbble.DribbbleSearchService
import io.clevver.data.api.dribbble.DribbbleService
import io.clevver.data.api.producthunt.ProductHuntService
import io.clevver.data.prefs.DribbblePrefs
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base class for loading data; extending types are responsible for providing implementations of
 * [.onDataLoaded] to do something with the data and [.cancelLoading] to
 * cancel any activity.
 */
abstract class BaseDataManager<T>(private val context: Context) : DataLoadingSubject {

    private val loadingCount: AtomicInteger = AtomicInteger(0)
    val dribbblePrefs: DribbblePrefs = DribbblePrefs[context]
    private var dribbbleSearchApi: DribbbleSearchService? = null
    private var productHuntApi: ProductHuntService? = null
    private var loadingCallbacks: MutableList<DataLoadingSubject.DataLoadingCallbacks>? = null

    private var cache: Cache? = null
    private var dribbbleClient: OkHttpClient? = null
    private var cachedDribbbleClient: OkHttpClient? = null

    private var phClient: OkHttpClient? = null
    private var cachedPhClient: OkHttpClient? = null


    val dribbbleApi: DribbbleService
        get() = dribbblePrefs.getApi()

    abstract fun onDataLoaded(data: T)

    abstract fun cancelLoading()

    override val isDataLoading: Boolean
        get() = loadingCount.get() > 0

    fun getProductHuntApi(): ProductHuntService? {
        if (productHuntApi == null) createProductHuntApi()
        return productHuntApi
    }

    fun getDribbbleSearchApi(): DribbbleSearchService? {
        if (dribbbleSearchApi == null) createDribbbleSearchApi()
        return dribbbleSearchApi
    }

    override fun registerCallback(callback: DataLoadingSubject.DataLoadingCallbacks) {
        if (loadingCallbacks == null) {
            loadingCallbacks = ArrayList<DataLoadingSubject.DataLoadingCallbacks>(1)
        }
        loadingCallbacks!!.add(callback)
    }

    override fun unregisterCallback(callback: DataLoadingSubject.DataLoadingCallbacks) {
        if (loadingCallbacks != null && loadingCallbacks!!.contains(callback)) {
            loadingCallbacks!!.remove(callback)
        }
    }

    protected fun loadStarted() {
        if (0 == loadingCount.getAndIncrement()) {
            dispatchLoadingStartedCallbacks()
        }
    }

    protected fun loadFinished() {
        if (0 == loadingCount.decrementAndGet()) {
            dispatchLoadingFinishedCallbacks()
        }
    }

    protected fun resetLoadingCount() {
        loadingCount.set(0)
    }

    protected fun dispatchLoadingStartedCallbacks() {
        if (loadingCallbacks == null || loadingCallbacks!!.isEmpty()) return
        for (loadingCallback in loadingCallbacks!!) {
            loadingCallback.dataStartedLoading()
        }
    }

    protected fun dispatchLoadingFinishedCallbacks() {
        if (loadingCallbacks == null || loadingCallbacks!!.isEmpty()) return
        for (loadingCallback in loadingCallbacks!!) {
            loadingCallback.dataFinishedLoading()
        }
    }

    private fun createDribbbleSearchApi() {
        dribbbleSearchApi = if (isConnected()) {
            Retrofit.Builder()
                    .baseUrl(ClevverUtils.DRIBBBLE_AUTH_SERVICE_API)
                    .addConverterFactory(DribbbleSearchConverter.Factory())
                    .build()
                    .create(DribbbleSearchService::class.java)
        } else {
            val httpClient = OkHttpClient.Builder()
                    .addInterceptor(provideOfflineCacheInterceptor())
                    .addNetworkInterceptor(provideCacheInterceptor())
                    .cache(provideCache())
                    .build()
            Retrofit.Builder()
                    .baseUrl(ClevverUtils.DRIBBBLE_AUTH_SERVICE_API)
                    .addConverterFactory(DribbbleSearchConverter.Factory())
                    .client(httpClient)
                    .build()
                    .create(DribbbleSearchService::class.java)
        }
    }

    private fun createProductHuntApi() {
        val clientBuilder = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(BuildConfig.PRODUCT_HUNT_DEVELOPER_TOKEN))
                .addInterceptor(provideOfflineCacheInterceptor())
                .addNetworkInterceptor(provideCacheInterceptor())
        val gson = Gson()
        productHuntApi = if (isConnected()) {
            phClient = clientBuilder.build()
            Retrofit.Builder()
                    .baseUrl(ClevverUtils.PRODUCT_HUNT_SERVICE_API)
                    .client(phClient!!)
                    .addConverterFactory(DenvelopingConverter(gson))
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
                    .create(ProductHuntService::class.java)
        } else {
            cachedPhClient = clientBuilder.addInterceptor(provideForcedOfflineCacheInterceptor())
                    .build()
            Retrofit.Builder()
                    .baseUrl(ClevverUtils.PRODUCT_HUNT_SERVICE_API)
                    .client(cachedPhClient!!)
                    .addConverterFactory(DenvelopingConverter(gson))
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
                    .create(ProductHuntService::class.java)
        }
    }

    private fun provideCacheInterceptor(): Interceptor {
        return Interceptor { chain ->
            val response = chain.proceed(chain.request())
            val cacheControl: CacheControl = if (isConnected()) {
                CacheControl.Builder()
                        .maxAge(0, TimeUnit.SECONDS)
                        .build()
            } else {
                CacheControl.Builder()
                        .maxStale(7, TimeUnit.DAYS)
                        .build()
            }

            response.newBuilder()
                    .removeHeader(HEADER_PRAGMA)
                    .removeHeader(HEADER_CACHE_CONTROL)
                    .header(HEADER_CACHE_CONTROL, cacheControl.toString())
                    .build()
        }
    }

    private fun provideCache(): Cache {
        if (cache == null) {
            try {
                //10 MB cache
                cache = Cache(File(context.cacheDir, HTTP_CACHE), 10.times(1024).times(1024))
            } catch (ex: Exception) {
            }
        }
        return cache!!
    }

    private fun provideOfflineCacheInterceptor(): Interceptor {
        return Interceptor { chain ->
            var request = chain.request()
            if (!isConnected()) {
                val cacheControl: CacheControl = CacheControl.Builder()
                        .maxStale(7, TimeUnit.DAYS)
                        .build()

                request = request.newBuilder()
                        .removeHeader(HEADER_PRAGMA)
                        .removeHeader(HEADER_CACHE_CONTROL)
                        .cacheControl(cacheControl)
                        .build()
            }

            chain.proceed(request)
        }
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

    //Clean cache and requests
    private fun clean() {
        if (dribbbleClient != null) {
            dribbbleClient?.dispatcher()?.cancelAll()
        }

        if (cachedDribbbleClient != null) {
            cachedDribbbleClient?.dispatcher()?.cancelAll()
        }

        if (phClient != null) {
            phClient?.dispatcher()?.cancelAll()
        }

        if (cachedPhClient != null) {
            cachedPhClient?.dispatcher()?.cancelAll()
        }

        if (cache != null) {
            try {
                cache?.evictAll()
            } catch (e: IOException) {
            }
        }

        dribbbleClient = null
        cachedDribbbleClient = null
        phClient = null
        cachedPhClient = null
        cache = null
    }

    //Get internet connection
    private fun isConnected(): Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val networkInfo = cm?.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        } catch (ex: Exception) {
            Timber.w(ex)
        }
        return false
    }

    companion object {
        private const val HTTP_CACHE = "http-cache"
        private const val HEADER_PRAGMA = "Pragma"
        private const val HEADER_CACHE_CONTROL = "Cache-control"

        fun setPage(items: List<PlaidItem>, page: Int) {
            for (item in items) {
                item.page = page
            }
        }

        fun setDataSource(items: List<PlaidItem>, dataSource: String) {
            for (item in items) {
                item.dataSource = dataSource
            }
        }
    }

    //	https://dribbble.com/oauth/authorize?client_id=bab76ece2a31199149ea6d5412c3285701e2b6d8b41f0c4cd34403cc7f5998e1&scope=public+write+comment+upload

}
