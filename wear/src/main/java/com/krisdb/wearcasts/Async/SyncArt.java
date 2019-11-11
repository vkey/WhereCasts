package com.krisdb.wearcasts.Async;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcasts;
import static com.krisdb.wearcastslibrary.CommonUtils.GetThumbnailDirectory;

public class SyncArt implements Callable<Boolean> {
    private final Context context;

    public SyncArt(final Context context) {
        this.context = context;
    }

    @Override
    public Boolean call() {
        final File dirThumbs = new File(GetThumbnailDirectory(context));

        final List<PodcastItem> podcasts = GetPodcasts(context);

        if (!dirThumbs.exists())
            dirThumbs.mkdirs();
        else {
            for (final PodcastItem podcast : podcasts)
            {
                if (podcast.getChannel() != null && podcast.getChannel().getThumbnailName() != null) {
                    final File thumb = new File(dirThumbs, podcast.getChannel().getThumbnailName());

                    if (thumb.exists())
                        thumb.delete();
                }
            }
        }

        for (final PodcastItem podcast : podcasts) {
            if (podcast.getChannel().getThumbnailUrl() != null)
                CommonUtils.SavePodcastLogo(context, podcast.getChannel().getThumbnailUrl().toString(), GetThumbnailDirectory(context), podcast.getChannel().getThumbnailName(), context.getResources().getInteger(R.integer.podcast_art_download_width));
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_thumbnail_sync_date", new Date().toString());
        editor.apply();
        return false;
    }
}
