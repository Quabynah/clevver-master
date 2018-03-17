package io.clevver.api

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import io.clevver.BuildConfig
import io.clevver.R
import io.clevver.data.prefs.DribbblePrefs

/**
 * Clevver object class for
 */
object ClevverUtils {
    //App downloads directory
    const val CLEVVER_DOWNLOAD_DIR = "Clevver"

    //Dribbble
    const val DRIBBBLE_AUTH_SERVICE_API = "https://dribbble.com/"
    const val DRIBBBLE_SEARCH_SERVICE_API = "https://api.dribbble.com/"
    const val LOGIN_CALLBACK = "clevver-auth-callback"
    private const val AUTH_REDIRECT_URL = "clevver%3A%2F%2F$LOGIN_CALLBACK"
    private const val PART_ONE = "https://dribbble.com/oauth/authorize?client_id=${BuildConfig.DRIBBBLE_CLIENT_ID}"
    private const val PART_TWO = "&redirect_uri=$AUTH_REDIRECT_URL"
    private const val PART_THREE = "&scope=public+write+comment+upload"

    /**
     * Login endpoint for Clevver
     */
    const val LOGIN_ENDPOINT_DRIBBBLE = PART_ONE + PART_TWO + PART_THREE


    //Product Hunt
    const val PRODUCT_HUNT_SERVICE_API = "https://api.producthunt.com/"

    //Common
    const val TYPE_DRIBBBLE = 1


    /**
     * Logout user
     */
    fun doLogout(context: Context, type: Int) {
        val materialDialog = MaterialDialog.Builder(context)
                .theme(Theme.DARK)
                .content("Do you want to logout of this session?")
                .positiveText("Logout")
                .negativeText("Cancel")
                .icon(context.resources.getDrawable(R.drawable.ic_launcher_512px))
                .onPositive({ dialog, _ ->
                    dialog.dismiss()
                    when (type) {
                        TYPE_DRIBBBLE -> DribbblePrefs[context].logout(context)
                    }
                })
                .onNegative({ dialog, _ ->
                    dialog.dismiss()
                })
                .build()
        materialDialog.show()
    }

    /**
     * Creates a loading dialog
     */
    fun getLoadingDialog(context: Context): MaterialDialog = MaterialDialog.Builder(context)
            .theme(Theme.DARK)
            .content(R.string.fetching_image)
            .progress(true, 0)
            .cancelable(false)
            .canceledOnTouchOutside(true)
            .build()
}