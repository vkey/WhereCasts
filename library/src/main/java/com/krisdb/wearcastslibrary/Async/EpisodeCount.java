package com.krisdb.wearcastslibrary.Async;

import android.content.Context;

import com.krisdb.wearcastslibrary.FeedParser;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.concurrent.Callable;

public class EpisodeCount implements Callable<Integer> {
    private final Context context;
    private PodcastItem mPodcast;

    public EpisodeCount(final Context context, final PodcastItem podcast)
    {
        this.context = context;
        this.mPodcast = podcast;
    }

    @Override
    public Integer call() {
        return FeedParser.parse(mPodcast, 1).size();
    }
}