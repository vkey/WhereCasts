package com.krisdb.wearcasts.Async;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodesWithDownloads;
import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcasts;

public class CleanupDownloads implements Callable<Boolean> {
    private final Context context;

    public CleanupDownloads(final Context context) {
        this.context = context;
    }

    @Override
    public Boolean call() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final List<PodcastItem> podcasts = GetPodcasts(context);
        final int autoDeleteID = Integer.valueOf(prefs.getString("pref_downloads_auto_delete", "1"));

        for (final PodcastItem podcast : podcasts) {

            final int downloadsToDeleteNumber = Integer.valueOf(prefs.getString("pref_" + podcast.getPodcastId() + "_downloads_saved", "0"));

            if (downloadsToDeleteNumber > 0) {
                List<PodcastItem> downloads1 = GetEpisodesWithDownloads(context, podcast.getPodcastId(), downloadsToDeleteNumber);

                for (final PodcastItem download : downloads1) {
                    Utilities.DeleteMediaFile(context, download);
                    SystemClock.sleep(200);
                }
            }

            if (autoDeleteID > Enums.AutoDelete.PLAYED.getAutoDeleteID()) {
                final List<PodcastItem> downloads2 = GetEpisodesWithDownloads(context, podcast.getPodcastId());

                for (final PodcastItem download : downloads2) {
                    final Date downloadDate = DateUtils.addHoursToDate(DateUtils.ConvertDate(download.getDownloadDate(), "yyyy-MM-dd HH:mm:ss"), autoDeleteID);
                    final Date compareDate = new Date();

                    //if the download date plus expiration hours, is not after today's day it's expired
                    if (downloadDate.before(compareDate)) {
                        Utilities.DeleteMediaFile(context, download);
                        SystemClock.sleep(200);
                    }
                }

            }
        }

        //remove error downloads
        File directoryPath;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
            directoryPath = Environment.getExternalStorageDirectory();
        else
            directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);

        final File directory = new File(directoryPath, context.getString(com.krisdb.wearcastslibrary.R.string.directory_episodes));

        try {
            final List<PodcastItem> episodesDownloaded = GetEpisodesWithDownloads(context);

            if (directory.listFiles() != null) {
                for (final File file : directory.listFiles()) {
                    if (file.getName().contains("-"))
                        file.delete();
                    else if (episodesDownloaded.size() > 0) {
                        boolean exists = false;
                        for (final PodcastItem episodeDownloaded : episodesDownloaded) {
                            if (episodeDownloaded.getEpisodeId() == Integer.valueOf(file.getName().substring(0, file.getName().indexOf(".")))) {
                                exists = true;
                                break;
                            }
                        }

                        if (!exists)
                            file.delete();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }
}
