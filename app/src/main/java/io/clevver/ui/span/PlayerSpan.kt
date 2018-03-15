/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.ui.span

import `in`.uncod.android.bypass.style.TouchableUrlSpan
import android.content.Intent
import android.content.res.ColorStateList
import android.text.TextUtils
import android.view.View
import io.clevver.ui.PlayerActivity

/**
 * A span for marking up a dribbble player
 */
class PlayerSpan(url: String,
                 private val playerName: String,
                 private val playerId: Long,
                 private val playerUsername: String?,
                 textColor: ColorStateList,
                 pressedBackgroundColor: Int) : TouchableUrlSpan(url, textColor, pressedBackgroundColor) {

    override fun onClick(view: View) {
        val intent = Intent(view.context, PlayerActivity::class.java)
        intent.putExtra(PlayerActivity.EXTRA_PLAYER_NAME, playerName)
        if (playerId > 0L) {
            intent.putExtra(PlayerActivity.EXTRA_PLAYER_ID, playerId)
        }
        if (!TextUtils.isEmpty(playerUsername)) {
            intent.putExtra(PlayerActivity.EXTRA_PLAYER_USERNAME, playerUsername)
        }
        view.context.startActivity(intent)
    }
}
