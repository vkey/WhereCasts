package com.krisdb.wearcasts;


import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.FetchPodcast;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.Date;
import java.util.List;

public class Utilities {

    static void sendEpisode(final Context ctx, final PodcastItem episode)
    {
        final PutDataMapRequest dataMap = PutDataMapRequest.create("/episodeimport");
        dataMap.getDataMap().putString("title", episode.getTitle());
        dataMap.getDataMap().putString("description", episode.getDescription());
        if (episode.getMediaUrl() != null)
            dataMap.getDataMap().putString("mediaurl", episode.getMediaUrl().toString());
        if (episode.getEpisodeUrl() != null)
            dataMap.getDataMap().putString("url", episode.getEpisodeUrl().toString());
        dataMap.getDataMap().putString("pubDate", episode.getPubDate());
        dataMap.getDataMap().putInt("duration", episode.getDuration());
        dataMap.getDataMap().putLong("time", new Date().getTime());
        dataMap.getDataMap().putInt("playlistid", episode.getPlaylistId());

        if (episode.getPlaylistId() == 0)
            CommonUtils.DeviceSync(ctx, dataMap, ctx.getString(R.string.alert_episode_added), Toast.LENGTH_SHORT);
   }

    static void SendToWatch(final Context ctx, final PodcastItem podcast)
    {
        SendToWatch(ctx, podcast, true);
    }

    static void TogglePremiumOnWatch(final Context ctx, final Boolean purchased) {
        TogglePremiumOnWatch(ctx, purchased, false);
    }

    static void TogglePremiumOnWatch(final Context ctx, final Boolean purchased, final Boolean showConfirm) {
        SystemClock.sleep(1500);

        final PutDataMapRequest dataMap = PutDataMapRequest.create("/premium");
        dataMap.getDataMap().putBoolean("unlock", purchased);
        dataMap.getDataMap().putBoolean("confirm", showConfirm);

        CommonUtils.DeviceSync(ctx, dataMap);
    }

    static void SendToWatch(final Context ctx, final PodcastItem podcast, final Boolean showToast) {

        if (podcast.getChannel().getThumbnailUrl() != null) {

            final PutDataMapRequest dataMap = PutDataMapRequest.create("/podcastimport");
            dataMap.getDataMap().putString("title", podcast.getChannel().getTitle());
            dataMap.getDataMap().putString("rss_url", podcast.getChannel().getRSSUrl().toString());
            dataMap.getDataMap().putLong("time", new Date().getTime());

            dataMap.getDataMap().putString("site_url", (podcast.getChannel().getSiteUrl() != null) ? podcast.getChannel().getSiteUrl().toString() : "");

            dataMap.getDataMap().putString("thumbnail_url", podcast.getChannel().getThumbnailUrl().toString());
            dataMap.getDataMap().putString("thumbnail_name", podcast.getChannel().getThumbnailName());

            CommonUtils.DeviceSync(ctx, dataMap, showToast ? ctx.getString(R.string.alert_podcast_added, podcast.getChannel().getTitle()) : null, Toast.LENGTH_SHORT);
        }
        else {
            new FetchPodcast(podcast.getChannel().getTitle(), podcast.getChannel().getRSSUrl().toString(), new Interfaces.FetchPodcastResponse() {

                @Override
                public void processFinish(final PodcastItem podcast) {

                    final PutDataMapRequest dataMap = PutDataMapRequest.create("/podcastimport");
                    dataMap.getDataMap().putString("title", podcast.getChannel().getTitle());
                    dataMap.getDataMap().putString("rss_url", podcast.getChannel().getRSSUrl().toString());
                    dataMap.getDataMap().putLong("time", new Date().getTime());

                    dataMap.getDataMap().putString("site_url", (podcast.getChannel().getSiteUrl() != null) ? podcast.getChannel().getSiteUrl().toString() : "");

                    if (podcast.getChannel().getThumbnailUrl() != null)
                    {
                        dataMap.getDataMap().putString("thumbnail_url", podcast.getChannel().getThumbnailUrl().toString());
                        dataMap.getDataMap().putString("thumbnail_name", podcast.getChannel().getThumbnailName());
                    }
                    CommonUtils.DeviceSync(ctx, dataMap, ctx.getString(R.string.alert_podcast_added, podcast.getChannel().getTitle()), Toast.LENGTH_SHORT);
                }

                @Override
                public void processFinish(List<PodcastItem> podcasts) {
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public static void ShowPlayingNotification(final Context ctx)
    {
        final Intent notificationIntent = new Intent(ctx, PhoneMainActivity.class);
        notificationIntent.setFlags(Notification.FLAG_ONGOING_EVENT);
        notificationIntent.setFlags(Notification.FLAG_NO_CLEAR);
        notificationIntent.setFlags(Notification.FLAG_FOREGROUND_SERVICE);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        final NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(ctx)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(prefs.getString("podcast_title", ""))
                        .setContentText(prefs.getString("episode_title", ""))
                        .setOngoing(true)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setAutoCancel(false)
                        .setContentIntent(PendingIntent.getActivity(ctx, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT));

        NotificationManagerCompat.from(ctx).notify(100, notificationBuilder.build());
    }

    public static boolean IsNetworkConnected(Context ctx) {
        final ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm != null && cm.getActiveNetworkInfo() != null;
    }
}