package com.krisdb.wearcasts;

import android.content.Context;
import android.net.ConnectivityManager;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcastslibrary.Async.FetchPodcast;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.Date;

public class Utilities {

    public static void sendEpisode(final Context ctx, final PodcastItem episode)
    {
        sendEpisode(ctx, episode, false);
    }

    public static void sendEpisode(final Context ctx, final PodcastItem episode, final Boolean autoDownload)
    {
        final PutDataMapRequest dataMapRequest = PutDataMapRequest.create("/episodeimport");
        final DataMap dataMap = dataMapRequest.getDataMap();
        dataMap.putString("title", episode.getTitle());
        dataMap.putString("description",CommonUtils.CleanDescription(episode.getDescription()));
        if (episode.getMediaUrl() != null)
            dataMap.putString("mediaurl", episode.getMediaUrl().toString());
        if (episode.getEpisodeUrl() != null)
            dataMap.putString("url", episode.getEpisodeUrl().toString());
        dataMap.putString("pubDate", episode.getPubDate());
        dataMap.putInt("duration", episode.getDuration());
        dataMap.putLong("time", new Date().getTime());
        dataMap.putInt("playlistid", episode.getPlaylistId());
        dataMap.putBoolean("auto_download", autoDownload);
        dataMap.putString("thumb_url", episode.getThumbnailUrl() != null ? episode.getThumbnailUrl().toString() : null);

        CommonUtils.DeviceSync(ctx, dataMapRequest);
   }

    public static void TogglePremiumOnWatch(final Context ctx, final Boolean purchased) {
        TogglePremiumOnWatch(ctx, purchased, false);
    }

    public static void TogglePremiumOnWatch(final Context ctx, final Boolean purchased, final Boolean showConfirm) {
        final PutDataMapRequest dataMap = PutDataMapRequest.create("/premium");
        dataMap.getDataMap().putBoolean("unlock", purchased);
        dataMap.getDataMap().putBoolean("confirm", showConfirm);

        CommonUtils.DeviceSync(ctx, dataMap);
    }

    public static void SendToWatch(final Context ctx, final PodcastItem podcast) {

        if (podcast.getChannel().getThumbnailUrl() != null) {

            final PutDataMapRequest dataMap = PutDataMapRequest.create("/podcastimport");
            dataMap.getDataMap().putString("title", podcast.getChannel().getTitle());
            dataMap.getDataMap().putString("rss_url", podcast.getChannel().getRSSUrl().toString());
            dataMap.getDataMap().putLong("time", new Date().getTime());

            dataMap.getDataMap().putString("site_url", (podcast.getChannel().getSiteUrl() != null) ? podcast.getChannel().getSiteUrl().toString() : "");

            dataMap.getDataMap().putString("thumbnail_url", podcast.getChannel().getThumbnailUrl().toString());
            //dataMap.getDataMap().putString("thumbnail_name", CommonUtils.GetThumbnailName(podcast.getTitle()));
            //dataMap.getDataMap().putString("thumbnail_name", podcast.getChannel().getThumbnailName());

            CommonUtils.DeviceSync(ctx, dataMap);
        }
        else {
            CommonUtils.executeAsync(new FetchPodcast(podcast.getChannel().getTitle(), podcast.getChannel().getRSSUrl().toString()), (p) -> {
                final PutDataMapRequest dataMap = PutDataMapRequest.create("/podcastimport");
                dataMap.getDataMap().putString("title", p.getChannel().getTitle());
                dataMap.getDataMap().putString("rss_url", p.getChannel().getRSSUrl().toString());
                dataMap.getDataMap().putLong("time", new Date().getTime());

                dataMap.getDataMap().putString("site_url", (p.getChannel().getSiteUrl() != null) ? p.getChannel().getSiteUrl().toString() : "");

                if (p.getChannel().getThumbnailUrl() != null)
                {
                    dataMap.getDataMap().putString("thumbnail_url", p.getChannel().getThumbnailUrl().toString());
                    //dataMap.getDataMap().putString("thumbnail_name", p.getChannel().getThumbnailName());
                }

                CommonUtils.DeviceSync(ctx, dataMap);
            });
        }
    }

    public static boolean IsNetworkConnected(Context ctx) {
        final ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm != null && cm.getActiveNetworkInfo() != null;
    }
}