package com.krisdb.wearcasts.Async;

import android.content.Context;

import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Utilities.Utilities;

import java.util.concurrent.Callable;

public class Unsubscribe implements Callable<Boolean> {
    private final Context context;
    private int mPodcastID;

    public Unsubscribe(final Context context, final int podcastId)
    {
        this.context = context;
        this.mPodcastID = podcastId;
    }
    @Override
    public Boolean call() {
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(context);
        db.deletePodcast(mPodcastID);
        Utilities.DeleteFiles(context, mPodcastID);
        db.unsubscribe(context, mPodcastID);
        db.close();

        Utilities.SetPodcstRefresh(context);

        return false;
    }
}

