package com.krisdb.wearcasts.Async;

import android.content.Context;

import com.krisdb.wearcasts.Utilities.PodcastUtilities;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;
import java.util.concurrent.Callable;

public class GetPodcasts implements Callable<List<PodcastItem>> {
    private final Context context;
    private boolean mHideEmpty, mShowDownloaded;

    public GetPodcasts(final Context context) {
        this.context = context;
        this.mShowDownloaded = false;
        this.mHideEmpty = false;
    }

    public GetPodcasts(final Context context, final Boolean hideEmpty) {
        this.context = context;
        this.mShowDownloaded = false;
        this.mHideEmpty = hideEmpty;
    }

    public GetPodcasts(final Context context, final Boolean hideEmpty, final Boolean showDownloaded) {
        this.context = context;
        this.mHideEmpty = hideEmpty;
        this.mShowDownloaded = showDownloaded;
    }

    @Override
    public List<PodcastItem> call() {
        return PodcastUtilities.GetPodcasts(context, mHideEmpty, mShowDownloaded);
    }
}