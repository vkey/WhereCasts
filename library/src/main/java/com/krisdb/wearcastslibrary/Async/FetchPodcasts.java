package com.krisdb.wearcastslibrary.Async;

import com.krisdb.wearcastslibrary.ChannelParser;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class FetchPodcasts implements Callable<List<PodcastItem>> {
    private List<PodcastItem> mPodcasts;

    public FetchPodcasts(List<PodcastItem> podcasts) {
        mPodcasts = podcasts;
    }

    @Override
    public List<PodcastItem> call() {

        final List<PodcastItem> podcasts = new ArrayList<>();

        for (PodcastItem podcast : mPodcasts) {

            if(podcast.getChannel().getRSSUrl() != null) {
                podcast.setChannel(ChannelParser.parse(podcast.getChannel().getTitle(), podcast.getChannel().getDescription(), podcast.getChannel().getRSSUrl().toString()));
                podcasts.add(podcast);
            }
        }

        return podcasts;
    }
}