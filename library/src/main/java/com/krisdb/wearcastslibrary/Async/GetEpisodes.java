package com.krisdb.wearcastslibrary.Async;

import com.krisdb.wearcastslibrary.FeedParser;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;
import java.util.concurrent.Callable;

public class GetEpisodes implements Callable<List<PodcastItem>> {
    private PodcastItem mPodcast;
    private List<PodcastItem> mEpisodes;
    private int mCount;

    public GetEpisodes(final PodcastItem podcast, final int count)
    {
        this.mPodcast = podcast;
        this.mCount = count;
    }

    @Override
    public List<PodcastItem> call() {
        return FeedParser.parse(mPodcast, mCount);
    }
}