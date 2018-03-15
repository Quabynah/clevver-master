/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import io.clevver.R;
import io.clevver.ui.PostNewDesignerNewsStory;

/**
 * Helper for working with launcher shortcuts.
 */
public class ShortcutHelper {
	
	private static final String SEARCH_SHORTCUT_ID = "search";
	private static final String POST_SHORTCUT_ID = "post_dn_story";
	private static final List<String> DYNAMIC_SHORTCUT_IDS
			= Collections.singletonList(POST_SHORTCUT_ID);
	
	private ShortcutHelper() { }
	
	@TargetApi(Build.VERSION_CODES.N_MR1)
	public static void enablePostShortcut(@NonNull Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		
		Intent intent = new Intent(context, PostNewDesignerNewsStory.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		ShortcutInfo postShortcut
				= new ShortcutInfo.Builder(context, POST_SHORTCUT_ID)
				.setShortLabel(context.getString(R.string.shortcut_post_short_label))
				.setLongLabel(context.getString(R.string.shortcut_post_long_label))
				.setDisabledMessage(context.getString(R.string.shortcut_post_disabled))
				.setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_post))
				.setIntent(intent)
				.build();
		shortcutManager.addDynamicShortcuts(Collections.singletonList(postShortcut));
	}
	
	@TargetApi(Build.VERSION_CODES.N_MR1)
	public static void disablePostShortcut(@NonNull Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		shortcutManager.disableShortcuts(DYNAMIC_SHORTCUT_IDS);
	}
	
	@TargetApi(Build.VERSION_CODES.N_MR1)
	public static void reportPostUsed(@NonNull Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		shortcutManager.reportShortcutUsed(POST_SHORTCUT_ID);
	}
	
	@TargetApi(Build.VERSION_CODES.N_MR1)
	public static void reportSearchUsed(@NonNull Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		shortcutManager.reportShortcutUsed(SEARCH_SHORTCUT_ID);
	}
	
}
