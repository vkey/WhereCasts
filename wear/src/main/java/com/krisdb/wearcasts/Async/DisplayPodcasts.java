package com.krisdb.wearcasts.Async;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;
import java.util.concurrent.Callable;

import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcasts;

public class DisplayPodcasts implements Callable<List<PodcastItem>> {
    private final Context context;
    private boolean mHideEmpty;

    public DisplayPodcasts(final Context context, final Boolean hideEmpty) {
        this.context = context;
        this.mHideEmpty = hideEmpty;
    }

    @Override
    public List<PodcastItem> call() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final Boolean showDownloaded = prefs.getBoolean("pref_display_show_downloaded", false);

        return GetPodcasts(context, mHideEmpty, showDownloaded);
    }
}