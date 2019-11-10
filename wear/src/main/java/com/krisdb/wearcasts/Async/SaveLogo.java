package com.krisdb.wearcasts.Async;

import android.content.Context;

import com.krisdb.wearcastslibrary.CommonUtils;

import java.util.concurrent.Callable;

import static com.krisdb.wearcastslibrary.CommonUtils.GetThumbnailDirectory;

public class SaveLogo implements Callable<Boolean> {
    private final Context context;
    private String mUrl, mFileName;
    private Boolean mForce;

    public SaveLogo(final Context context, final String url, final String filename)
    {
        this.context = context;
        this.mFileName = filename;
        this.mUrl = url;
        this.mForce = false;
    }

    public SaveLogo(final Context context, final String url, final String filename, final Boolean force) {
        this.context = context;
        this.mUrl = url;
        this.mFileName = filename;
        this.mForce = force;
    }

    @Override
    public Boolean call() {
        CommonUtils.SavePodcastLogo(
                context,
                mUrl,
                GetThumbnailDirectory(context),
                mFileName,
                context.getResources().getInteger(com.krisdb.wearcastslibrary.R.integer.podcast_art_download_width),
                mForce
        );

        return false;
    }
}