/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.clevver.R;
import io.clevver.data.Source;


/**
 * Manage saving and retrieving data sources from disk.
 */
public class SourceManager {
	
	public static final String SOURCE_DRIBBBLE_POPULAR = "SOURCE_DRIBBBLE_POPULAR";
	public static final String SOURCE_DRIBBBLE_FOLLOWING = "SOURCE_DRIBBBLE_FOLLOWING";
	public static final String SOURCE_DRIBBBLE_USER_LIKES = "SOURCE_DRIBBBLE_USER_LIKES";
	public static final String SOURCE_DRIBBBLE_USER_SHOTS = "SOURCE_DRIBBBLE_USER_SHOTS";
	public static final String SOURCE_DRIBBBLE_RECENT = "SOURCE_DRIBBBLE_RECENT";
	public static final String SOURCE_DRIBBBLE_DEBUTS = "SOURCE_DRIBBBLE_DEBUTS";
	public static final String SOURCE_DRIBBBLE_ANIMATED = "SOURCE_DRIBBBLE_ANIMATED";
	public static final String SOURCE_BEHANCE_PROJECTS = "SOURCE_BEHANCE_PROJECTS";
	public static final String SOURCE_PRODUCT_HUNT = "SOURCE_PRODUCT_HUNT";
	public static final String SOURCE_UNSPLASH = "SOURCE_UNSPLASH";
	public static final String SOURCE_GITHUB = "SOURCE_GITHUB";
	private static final String SOURCES_PREF = "SOURCES_PREF";
	private static final String KEY_SOURCES = "KEY_SOURCES";
	
	public static List<Source> getSources(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(SOURCES_PREF, Context.MODE_PRIVATE);
		Set<String> sourceKeys = prefs.getStringSet(KEY_SOURCES, null);
		if (sourceKeys == null) {
			setupDefaultSources(context, prefs.edit());
			return getDefaultSources(context);
		}
		
		List<Source> sources = new ArrayList<>(sourceKeys.size());
		for (String sourceKey : sourceKeys) {
			if (sourceKey.startsWith(Source.DribbbleSearchSource.DRIBBBLE_QUERY_PREFIX)) {
				sources.add(new Source.DribbbleSearchSource(
						sourceKey.replace(Source.DribbbleSearchSource.DRIBBBLE_QUERY_PREFIX, ""),
						prefs.getBoolean(sourceKey, false)));
			} else {
				sources.add(getSource(context, sourceKey, prefs.getBoolean(sourceKey, false)));
			}
		}
		Collections.sort(sources, new Source.SourceComparator());
		return sources;
	}
	
	public static void addSource(Source toAdd, Context context) {
		SharedPreferences prefs = context.getSharedPreferences(SOURCES_PREF, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		Set<String> sourceKeys = prefs.getStringSet(KEY_SOURCES, null);
		sourceKeys.add(toAdd.key);
		editor.putStringSet(KEY_SOURCES, sourceKeys);
		editor.putBoolean(toAdd.key, toAdd.active);
		editor.apply();
	}
	
	public static void updateSource(Source source, Context context) {
		SharedPreferences.Editor editor =
				context.getSharedPreferences(SOURCES_PREF, Context.MODE_PRIVATE).edit();
		editor.putBoolean(source.key, source.active);
		editor.apply();
	}
	
	public static void removeSource(Source source, Context context) {
		SharedPreferences prefs = context.getSharedPreferences(SOURCES_PREF, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		Set<String> sourceKeys = prefs.getStringSet(KEY_SOURCES, null);
		sourceKeys.remove(source.key);
		editor.putStringSet(KEY_SOURCES, sourceKeys);
		editor.remove(source.key);
		editor.apply();
	}
	
	private static void setupDefaultSources(Context context, SharedPreferences.Editor editor) {
		ArrayList<Source> defaultSources = getDefaultSources(context);
		Set<String> keys = new HashSet<>(defaultSources.size());
		for (Source source : defaultSources) {
			keys.add(source.key);
			editor.putBoolean(source.key, source.active);
		}
		editor.putStringSet(KEY_SOURCES, keys);
		editor.commit();
	}
	
	@Nullable
	private static Source getSource(Context context, String key, boolean active) {
		for (Source source : getDefaultSources(context)) {
			if (source.key.equals(key)) {
				source.active = active;
				return source;
			}
		}
		return null;
	}
	
	private static ArrayList<Source> getDefaultSources(Context context) {
		ArrayList<Source> defaultSources = new ArrayList<>(14);
		defaultSources.add(new Source.DribbbleSource(SOURCE_DRIBBBLE_POPULAR, 100,
				context.getString(R.string.source_dribbble_popular), true));
		defaultSources.add(new Source.DribbbleSource(SOURCE_DRIBBBLE_FOLLOWING, 101,
				context.getString(R.string.source_dribbble_following), false));
		defaultSources.add(new Source.DribbbleSource(SOURCE_DRIBBBLE_USER_SHOTS, 102,
				context.getString(R.string.source_dribbble_user_shots), false));
		defaultSources.add(new Source.DribbbleSource(SOURCE_DRIBBBLE_USER_LIKES, 103,
				context.getString(R.string.source_dribbble_user_likes), false));
		defaultSources.add(new Source.DribbbleSource(SOURCE_DRIBBBLE_RECENT, 104,
				context.getString(R.string.source_dribbble_recent), false));
		defaultSources.add(new Source.DribbbleSource(SOURCE_DRIBBBLE_DEBUTS, 105,
				context.getString(R.string.source_dribbble_debuts), false));
		defaultSources.add(new Source.DribbbleSource(SOURCE_DRIBBBLE_ANIMATED, 106,
				context.getString(R.string.source_dribbble_animated), false));
		defaultSources.add(new Source.DribbbleSearchSource(context.getString(R.string
				.source_dribbble_search_material_design), false));
		defaultSources.add(new Source.DribbbleSearchSource(context.getString(R.string
				.source_dribbble_search_e_commerce), false));
		defaultSources.add(new Source.DribbbleSearchSource(context.getString(R.string
				.source_dribbble_search_dashboard), false));
		defaultSources.add(new Source.DribbbleSearchSource(context.getString(R.string.source_dribbble_search_animations), false));
		// 200 sort order range left for dribbble searches
		defaultSources.add(new Source(SOURCE_PRODUCT_HUNT, 300,
				context.getString(R.string.source_product_hunt), R.drawable.ic_product_hunt, false));
		defaultSources.add(new Source(SOURCE_BEHANCE_PROJECTS, 400, context.getString(R.string
				.behance_projects), R.drawable.ic_firebase, false));
		defaultSources.add(new Source(SOURCE_UNSPLASH, 401, context.getString(R.string
				.unsplash_collections), R.drawable.ic_firebase, false));
		defaultSources.add(new Source(SOURCE_GITHUB, 402, context.getString(R.string
				.github_repos), R.drawable.ic_firebase, false));
		return defaultSources;
	}
	
}
