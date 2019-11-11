package com.krisdb.wearcasts.Async;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcasts.Models.SyncPodcastsResponse;
import com.krisdb.wearcasts.Utilities.Processor;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodesWithDownloads;
import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcast;
import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcasts;

public class SyncPodcasts implements Callable<SyncPodcastsResponse> {
    private final Context context;
    private int mPodcastId;
    private boolean mOpmlImport = false;

    public SyncPodcasts(final Context context) {
        this.context = context;
        this.mPodcastId = 0;
    }

    public SyncPodcasts(final Context context, final boolean opmlImport) {
        this.context = context;
        this.mPodcastId = 0;
        this.mOpmlImport = opmlImport;
    }

    public SyncPodcasts(final Context context, final int podcastId) {
        this.context = context;
        this.mPodcastId = podcastId;
    }

    @Override
    public SyncPodcastsResponse call() {

        final Processor processor = new Processor(context);
        processor.downloadEpisodes = new ArrayList<>();

        final SyncPodcastsResponse syncPodcastsResponse = new SyncPodcastsResponse();
        syncPodcastsResponse.setNewEpisodeCount(0);
        syncPodcastsResponse.setDownloads(0);
        syncPodcastsResponse.setDownloadEpisodes(new ArrayList<>());

        if (mPodcastId > 0) {
            final PodcastItem podcast = GetPodcast(context, mPodcastId);
            processor.processEpisodes(podcast);
            syncPodcastsResponse.setNewEpisodeCount(processor.newEpisodesCount);
            syncPodcastsResponse.setDownloads(processor.downloadCount);
        } else {
            final List<PodcastItem> podcasts = GetPodcasts(context);
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            for (final PodcastItem podcast : podcasts) {

                processor.processEpisodes(podcast);

                syncPodcastsResponse.setNewEpisodeCount(processor.newEpisodesCount);
                syncPodcastsResponse.setDownloads(processor.downloadCount);

                final int downloadsToDeleteNumber = Integer.valueOf(prefs.getString("pref_" + podcast.getPodcastId() + "_downloads_saved", "0"));

                if (downloadsToDeleteNumber > 0) {
                    List<PodcastItem> downloads1 = GetEpisodesWithDownloads(context, podcast.getPodcastId(), downloadsToDeleteNumber);

                    if (downloads1.size() > 0) {
                        for (final PodcastItem download : downloads1)
                            Utilities.DeleteMediaFile(context, download);
                    }
                }

                final int autoDeleteID = Integer.valueOf(prefs.getString("pref_downloads_auto_delete", "1"));

                if (autoDeleteID > Enums.AutoDelete.PLAYED.getAutoDeleteID()) {
                    List<PodcastItem> downloads2 = GetEpisodesWithDownloads(context, podcast.getPodcastId());

                    for (final PodcastItem download : downloads2) {
                        final Date downloadDate = DateUtils.ConvertDate(download.getDownloadDate(), "yyyy-MM-dd HH:mm:ss");
                        final Date compareDate = DateUtils.addHoursToDate(new Date(), autoDeleteID);

                        if (downloadDate.after(compareDate))
                            Utilities.DeleteMediaFile(context, download);
                    }

                }

                if (mOpmlImport) {
                    final PutDataMapRequest dataMapRequest = PutDataMapRequest.create("/opmlimport_episodes");
                    final DataMap dataMap = dataMapRequest.getDataMap();
                    dataMap.putString("podcast_title_episodes", podcast.getChannel().getTitle());
                    CommonUtils.DeviceSync(context, dataMapRequest);
                }
            }
        }

        if (processor.downloadEpisodes.size() > 0)
            syncPodcastsResponse.setDownloadEpisodes(processor.downloadEpisodes);

        if (mPodcastId == 0) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString("last_podcast_sync_date", new Date().toString());
            editor.apply();
        }

        return syncPodcastsResponse;
    }
}