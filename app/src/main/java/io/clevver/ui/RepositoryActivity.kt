/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.transition.TransitionManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import io.clevver.R
import io.clevver.api.ClevverUtils
import io.clevver.data.api.github.GithubService
import io.clevver.data.api.github.model.GithubRepo
import io.clevver.data.prefs.GitHubPrefs
import io.clevver.ui.recyclerview.SlideInItemAnimator
import io.clevver.util.bindView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

/**
 * [GithubRepo] screen: Displays a list of github repositories
 */
class RepositoryActivity : Activity() {
    private val container: ViewGroup by bindView(R.id.container)
    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val grid: RecyclerView by bindView(R.id.grid)
    private val loading: ProgressBar by bindView(android.R.id.empty)
    private val noRepos: TextView by bindView(R.id.no_repos)

    private lateinit var adapter: GithubRepositoryAdapter
    private lateinit var prefs: GitHubPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repository)
        setActionBar(toolbar)
        toolbar.setNavigationOnClickListener({ onBackPressed() })
        prefs = GitHubPrefs[this]
        initRecyclerView()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.github, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val githubLogin = menu?.findItem(R.id.menu_github_login)
        githubLogin?.title = if (prefs.isLoggedIn) getString(R.string.logout) else getString(R.string.connect_with_github)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.menu_github_login) {
            return if (prefs.isLoggedIn) {
                ClevverUtils.doLogout(this, ClevverUtils.TYPE_GITHUB)
                true
            } else {
                prefs.login(this@RepositoryActivity)
                true
            }
        }
        return super.onOptionsItemSelected(item)
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

    private fun checkAuthCallback(intent: Intent?) {
        if (intent != null
                && intent.data != null
                && !TextUtils.isEmpty(intent.data!!.authority)
                && ClevverUtils.LOGIN_CALLBACK == intent.data!!.authority) {
            val code = intent.data?.getQueryParameter("code")
            if (code.isNullOrEmpty()) {
                Timber.d("Code is null or empty")
                Snackbar.make(container, "Failed to login", Snackbar.LENGTH_LONG).show()
            } else {
                getAccessToken(code!!)
            }
        }
    }

    private fun getAccessToken(code: String) {
        //todo: do something with code
        prefs.setAccessToken(code)

        /* val dribbbleAuthApi = Retrofit.Builder()
                 .baseUrl(PlaidUtils.DRIBBBLE_AUTH_SERVICE_API)
                 .addConverterFactory(GsonConverterFactory.create())
                 .build()
                 .create(DribbbleAuthService::class.java)

         val accessTokenCall = dribbbleAuthApi.getAccessToken(BuildConfig.DRIBBBLE_CLIENT_ID,
                 BuildConfig.DRIBBBLE_CLIENT_SECRET,
                 code)
         accessTokenCall.enqueue(object : Callback<AccessToken> {
             override fun onResponse(call: Call<AccessToken>, response: Response<AccessToken>) {
                 if (response.body() == null) {
                     showLoginFailed()
                     return
                 }
                 isLoginFailed = false
                 dribbblePrefs.setAccessToken(response.body()!!.access_token)
                 showLoggedInUser(response.body())
                 setResult(Activity.RESULT_OK)
                 finishAfterTransition()
             }

             override fun onFailure(call: Call<AccessToken>, t: Throwable) {
                 Timber.e(t, t.message)
                 showLoginFailed()
             }
         })*/
    }

    private fun initRecyclerView() {
        adapter = GithubRepositoryAdapter()
        grid.adapter = adapter
        val columns = resources.getInteger(R.integer.num_columns)
        val layoutManager = GridLayoutManager(this, columns)
        grid.layoutManager = layoutManager
        grid.setHasFixedSize(true)
        grid.itemAnimator = SlideInItemAnimator()

        setupRetrofit()
    }

    private fun setupRetrofit() {
        //Create retrofit from builder
        val retrofit = Retrofit.Builder()
                .baseUrl(ClevverUtils.GITHUB_ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        val githubService = retrofit.create(GithubService::class.java)

        //Setup service and calls
        if (isConnected()) {
            Toast.makeText(applicationContext, "Fetching data", Toast.LENGTH_SHORT).show()
            githubService.getRepos("fs-opensource")
                    .enqueue(object : Callback<List<GithubRepo>?> {
                        override fun onFailure(call: Call<List<GithubRepo>?>?, t: Throwable?) {
                            Timber.e(t)
                            Toast.makeText(applicationContext, t?.localizedMessage, Toast.LENGTH_LONG).show()
                        }

                        override fun onResponse(call: Call<List<GithubRepo>?>?, response: Response<List<GithubRepo>?>?) {
                            if (response == null || !response.isSuccessful) {
                                Toast.makeText(applicationContext, response?.message(),
                                        Toast.LENGTH_LONG).show()
                                return
                            } else {
                                val body = response.body()
                                val repos = ArrayList<GithubRepo>(0)
                                if (body != null && body.isNotEmpty()) {
                                    for (item in body) {
                                        repos.add(item)
                                    }

                                    //Add to adapter
                                    adapter.addAndResort(repos)
                                    updateUI()
                                }
                            }
                        }
                    })
        } else {
            Snackbar.make(container, "No internet connection", Snackbar.LENGTH_INDEFINITE).show()
            updateUI()
        }

    }

    private fun updateUI() {
        //update UI to reflect changes
        if (adapter.itemCount > 0) {
            TransitionManager.beginDelayedTransition(container)
            loading.visibility = View.GONE
            noRepos.visibility = View.GONE
        } else {
            TransitionManager.beginDelayedTransition(container)
            loading.visibility = View.VISIBLE
            noRepos.visibility = View.VISIBLE
        }
    }

    private fun isConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }

    //Adapter for the Github repositories
    internal inner class GithubRepositoryAdapter : RecyclerView.Adapter<GithubRepositoryViewHolder>() {

        private val repositories: ArrayList<GithubRepo> = ArrayList(0)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GithubRepositoryViewHolder {
            return GithubRepositoryViewHolder(layoutInflater.inflate(R.layout.github_item_repos,
                    parent, false))
        }

        override fun onBindViewHolder(holder: GithubRepositoryViewHolder, position: Int) {
            val githubRepo = repositories[position]
            holder.name.text = githubRepo.name

            holder.itemView.setOnClickListener({
                val intent = Intent(this@RepositoryActivity, RepositoryDetailsActivity::class.java)
                intent.putExtra(RepositoryDetailsActivity.EXTRA_REPO, githubRepo)
                startActivityForResult(intent, REPO_VIEW)
            })
        }

        override fun getItemCount(): Int {
            return repositories.size
        }

        fun addAndResort(newRepos: List<GithubRepo>) {
            if (newRepos.isEmpty()) return
            val size = repositories.size
            var add = true
            for (repo in newRepos) {
                for (i in 0 until size) {
                    if (repo == repositories[i]) {
                        add = false
                    }
                }

                if (add) {
                    repositories.add(repo)
                    notifyItemRangeChanged(0, newRepos.size)
                }
            }
        }

    }

    internal inner class GithubRepositoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.repo_title)
        var desc: TextView = itemView.findViewById(R.id.repo_desc)
    }

    companion object {
        private const val REPO_VIEW = 234
    }
}
