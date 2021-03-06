package com.krisdb.wearcasts.Async;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcasts;
import static com.krisdb.wearcastslibrary.CommonUtils.GetPodcastsThumbnailDirectory;

public class SyncArt implements Callable<Boolean> {
    private final Context context;
    private boolean mOpmlImport = false;

    public SyncArt(final Context context) {
        this.context = context;
    }

    public SyncArt(final Context context, final boolean opmlImport) {
        this.context = context;
        this.mOpmlImport = opmlImport;
    }

    @Override
    public Boolean call() {
        final File dirThumbs = new File(GetPodcastsThumbnailDirectory(context));

        final List<PodcastItem> podcasts = GetPodcasts(context);

        if (!dirThumbs.exists())
            dirThumbs.mkdirs();
        else
            Utilities.deleteAllThumbnails(context);

        for (final PodcastItem podcast : podcasts) {
            if (podcast.getChannel().getThumbnailUrl() != null) {
                CommonUtils.SavePodcastLogo(context, podcast.getChannel().getThumbnailUrl().toString(), GetPodcastsThumbnailDirectory(context), CommonUtils.GetThumbnailName(podcast.getPodcastId()), context.getResources().getInteger(R.integer.podcast_art_download_width));

                if (mOpmlImport) {
                    final PutDataMapRequest dataMapRequest = PutDataMapRequest.create("/opmlimport_art");
                    final DataMap dataMap = dataMapRequest.getDataMap();
                    dataMap.putString("podcast_title_art", podcast.getChannel().getTitle());
                    CommonUtils.DeviceSync(context, dataMapRequest);
                }
            }
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_thumbnail_sync_date", new Date().toString());
        editor.apply();
        return false;
    }
}
