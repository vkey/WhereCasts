package com.krisdb.wearcasts;

import android.content.Context;
import android.content.SharedPreferences;

public class CacheUtils {

    static void deletePodcastsCache(final Context ctx)
    {
       deleteCache(ctx, ctx.getString(R.string.cache_podcast_list));
    }

    static void savePodcastsCache(final Context ctx, final String value)
    {
        saveToCache(ctx, ctx.getString(R.string.cache_podcast_list), value);
    }

    static String getPodcastsCache(final Context ctx)
    {
        return getCache(ctx, ctx.getString(R.string.cache_podcast_list));
    }

    private static void deleteCache(final Context ctx, final String key)
    {
        final SharedPreferences prefs = ctx.getSharedPreferences(ctx.getPackageName().concat(".cache"), Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, null);
        editor.apply();
    }

    static void saveToCache(final Context ctx, final String key, final String value)
    {
        final SharedPreferences prefs = ctx.getSharedPreferences(ctx.getPackageName().concat(".cache"), Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    static String getCache(final Context ctx, final String key)
    {
        final SharedPreferences prefs = ctx.getSharedPreferences(ctx.getPackageName().concat(".cache"), Context.MODE_PRIVATE);
        return prefs.getString(key, null);
    }
}
