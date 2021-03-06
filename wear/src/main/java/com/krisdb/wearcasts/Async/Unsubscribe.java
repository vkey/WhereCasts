package com.krisdb.wearcasts.Async;

import android.content.Context;

import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Utilities.PodcastUtilities;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.util.concurrent.Callable;

import static com.krisdb.wearcastslibrary.CommonUtils.GetPodcastsThumbnailDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.GetThumbnailName;

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

        final PodcastItem podcast = PodcastUtilities.GetPodcast(context, mPodcastID);

        final File thumbPodcast = new File(GetPodcastsThumbnailDirectory(context), GetThumbnailName(podcast.getPodcastId()));

        if (thumbPodcast.exists())
            thumbPodcast.delete();

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(context);
        db.deletePodcast(mPodcastID);
        Utilities.DeleteFiles(context, mPodcastID);
        db.unsubscribe(context, mPodcastID);
        db.close();

        Utilities.SetPodcstRefresh(context);

        return false;
    }
}

