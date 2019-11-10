package com.krisdb.wearcasts.Models;

import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

public class SyncPodcastsResponse {
    private int newEpisodeCount = 0, downloads = 0;
    private List<PodcastItem> downloadEpisodes;

    public List<PodcastItem> getDownloadEpisodes() {
        return downloadEpisodes;
    }

    public void setDownloadEpisodes(final List<PodcastItem> episodes) {
        this.downloadEpisodes = episodes;
    }

    public int getNewEpisodeCount() {
        return newEpisodeCount;
    }

    public void setNewEpisodeCount(final int count) {
        this.newEpisodeCount = count;
    }

    public int getDownloads() {
        return downloads;
    }

    public void setDownloads(final int count) {
        this.downloads = count;
    }
}
