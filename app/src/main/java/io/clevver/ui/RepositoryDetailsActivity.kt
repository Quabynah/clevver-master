/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import io.clevver.R
import io.clevver.data.api.github.model.GithubRepo

/**
 * Details about a selected repository
 */
class RepositoryDetailsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repository_details)

        val intent = intent
        if (intent.hasExtra(EXTRA_REPO)) {
            val githubRepo = intent.getParcelableExtra<GithubRepo>(EXTRA_REPO)
            bindRepo(githubRepo)
        }

    }

    private fun bindRepo(githubRepo: GithubRepo?) {
        if (githubRepo == null) return

        Toast.makeText(this@RepositoryDetailsActivity, githubRepo.toString(), Toast.LENGTH_SHORT)
                .show()
    }

    companion object {
        const val EXTRA_REPO = "EXTRA_REPO"
    }
}
