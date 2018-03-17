/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;

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
	public static void reportSearchUsed(@NonNull Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		shortcutManager.reportShortcutUsed(SEARCH_SHORTCUT_ID);
	}
	
}
