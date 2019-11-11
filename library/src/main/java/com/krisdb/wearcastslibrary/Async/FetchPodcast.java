package com.krisdb.wearcastslibrary.Async;

import com.krisdb.wearcastslibrary.ChannelParser;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.concurrent.Callable;

public class FetchPodcast implements Callable<PodcastItem> {
    private String mTitle, mUrl;

    public FetchPodcast(String title, String url) {
        this.mTitle = title;
        this.mUrl = url;
    }

    @Override
    public PodcastItem call() {

        final PodcastItem podcast = new PodcastItem();
        podcast.setChannel(ChannelParser.parse(mTitle, null, mUrl));

        return podcast;
    }
}