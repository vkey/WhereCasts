package com.krisdb.wearcasts.Async;

import android.content.Context;
import android.media.MediaPlayer;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.Date;
import java.util.concurrent.Callable;

import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcast;
import static com.krisdb.wearcastslibrary.CommonUtils.getCurrentPosition;

public class SyncWithMobileDevice implements Callable<Boolean> {
    private final Context context;
    private PodcastItem mEpisode;
    private MediaPlayer mMediaPlayer;
    private Boolean mFinished;

    public SyncWithMobileDevice(final Context context, final PodcastItem episode, final MediaPlayer mp, final Boolean finished) {
        this.context = context;
        mEpisode= episode;
        mMediaPlayer = mp;
        mFinished = finished;
    }

    @Override
    public Boolean call() {
        final PutDataMapRequest dataMap = PutDataMapRequest.create("/syncdevice");

        if (mMediaPlayer == null || mEpisode == null) return null;

        final PodcastItem podcast = GetPodcast(context, mEpisode.getPodcastId());

        if (podcast == null) return null;

        if (podcast.getChannel() != null)
            dataMap.getDataMap().putString("podcast_title", podcast.getChannel().getTitle());

        if (mEpisode.getMediaUrl() != null)
            dataMap.getDataMap().putString("episode_url", mEpisode.getMediaUrl().toString());

        dataMap.getDataMap().putString("episode_title", mEpisode.getTitle());

        dataMap.getDataMap().putInt("position", getCurrentPosition(mMediaPlayer));
        dataMap.getDataMap().putInt("duration", mMediaPlayer.getDuration());
        dataMap.getDataMap().putInt("id", mFinished ? 0 : mEpisode.getEpisodeId());
        dataMap.getDataMap().putLong("time", new Date().getTime());

        CommonUtils.DeviceSync(context, dataMap);

        return false;
    }
}