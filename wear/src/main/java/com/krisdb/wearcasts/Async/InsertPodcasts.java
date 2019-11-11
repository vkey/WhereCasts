package com.krisdb.wearcasts.Async;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.DBUtilities;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;
import java.util.concurrent.Callable;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodes;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.SearchEpisodes;

public class InsertPodcasts implements Callable<Void> {
    private final Context context;
    private List<PodcastItem> mPodcasts;

    public InsertPodcasts(final Context context, final List<PodcastItem> podcasts) {
        this.context = context;
        this.mPodcasts = podcasts;
    }

    @Override
    public Void call() {

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(context);

        for(final PodcastItem podcast : mPodcasts)
            DBUtilities.insertPodcast(context, db, podcast, false, false);

        db.close();

        return null;
    }
}