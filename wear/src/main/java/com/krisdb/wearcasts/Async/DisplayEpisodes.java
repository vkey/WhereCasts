package com.krisdb.wearcasts.Async;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodes;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.SearchEpisodes;

public class DisplayEpisodes implements Callable<List<PodcastItem>> {
    private final Context context;
    private int mPodcastId;
    private String mQuery;

    public DisplayEpisodes(final Context context, final int podcastId, final String query) {
        this.context = context;
        this.mPodcastId = podcastId;
        this.mQuery = query;
    }

    @Override
    public List<PodcastItem> call() {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        final boolean hidePlayed = prefs.getBoolean("pref_" + mPodcastId + "_hide_played", false);

        final int numberOfEpisode = Integer.valueOf(prefs.getString("pref_episode_limit", context.getString(R.string.episode_list_default)));

        if (mQuery == null)
            return GetEpisodes(context, mPodcastId, hidePlayed, numberOfEpisode, null);
        else
            return SearchEpisodes(context, mPodcastId, mQuery);
    }
}