package com.krisdb.wearcasts.Utilities;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.preference.PreferenceManager;

import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.ChannelItem;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetLatestEpisode;
import static com.krisdb.wearcastslibrary.CommonUtils.GetRoundedLogo;

public class PodcastUtilities {

    public static int GetPodcastCount(final Context context)
    {
        int count = 0;
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(context);
        final SQLiteDatabase sdb = db.select();
        final Cursor cursor = sdb.rawQuery("SELECT COUNT(id) FROM [tbl_podcasts]",null);

        if (cursor.moveToFirst())
            count = cursor.getInt(0);

        cursor.close();
        db.close();
        sdb.close();

        return count;
    }

    public static PodcastItem GetPodcast(final Context ctx, final int podcastId) {
        final PodcastItem podcast = new PodcastItem();

        try {
            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
            final SQLiteDatabase sdb = db.select();

            final Cursor cursor = sdb.rawQuery(
                    "SELECT [id],[title],[url],[thumbnail_url],[thumbnail_name],[description] FROM [tbl_podcasts] WHERE [id] = ?",
                    new String[]{String.valueOf(podcastId)});

            if (cursor.moveToFirst()) {
                podcast.setPodcastId(cursor.getInt(0));

                final ChannelItem channel = new ChannelItem();
                channel.setTitle(cursor.getString(1));
                channel.setRSSUrl(cursor.getString(2));
                if (cursor.getString(3) != null && cursor.getString(4) != null) {
                    channel.setThumbnailUrl(cursor.getString(3));
                    channel.setThumbnailName(cursor.getString(4));
                }
                podcast.setChannel(channel);

                if (cursor.getString(5) != null)
                    podcast.setDescription(cursor.getString(5));
            }

            cursor.close();
            db.close();
            sdb.close();
        }
        catch (SQLiteException ex)
        {
            ex.printStackTrace();
        }

        return podcast;
    }

    public static List<PodcastItem> GetPodcasts(final Context ctx) {
        return GetPodcasts(ctx, false, false);
    }

    public static List<PodcastItem> GetPodcasts(final Context ctx, final Boolean hideEmpty, final Boolean showDownloaded) {
        List<PodcastItem> podcasts = new ArrayList<>();
        //final Gson gson = new Gson();

        //final String cache = CacheUtils.getPodcastsCache(ctx);

        //if (cache == null)
        {

            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
            final SQLiteDatabase sdb = db.select();

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

            int orderId = Integer.valueOf(prefs.getString("pref_display_podcasts_sort_order", String.valueOf(ctx.getResources().getInteger(R.integer.default_podcasts_sort_order))));

            final String orderString = Utilities.GetOrderClause(orderId, "tbl_podcasts");
            final int latestEpisodesSortOrderID = Enums.SortOrder.LATESTEPISODES.getSorderOrderCode();

            String sql;
            if (hideEmpty && showDownloaded)
                sql = "SELECT tbl_podcasts.id,tbl_podcasts.title,tbl_podcasts.url,tbl_podcasts.thumbnail_url,tbl_podcasts.thumbnail_name"
                        .concat(",(select distinct count(tbl_podcast_episodes.pid ) from tbl_podcast_episodes where tbl_podcast_episodes.pid = tbl_podcasts.id and tbl_podcast_episodes.New = 1) ")
                        .concat("FROM [tbl_podcasts] ")
                        .concat("JOIN tbl_podcast_episodes ")
                        .concat("ON tbl_podcast_episodes.pid = tbl_podcasts.id ")
                        .concat("WHERE tbl_podcast_episodes.new = 1 AND tbl_podcast_episodes.download = 1 ")
                        .concat("GROUP BY tbl_podcast_episodes.pid ")
                        .concat("ORDER BY ")
                        .concat(orderString);
            else if (hideEmpty)
                sql = "SELECT tbl_podcasts.id,tbl_podcasts.title,tbl_podcasts.url,tbl_podcasts.thumbnail_url,tbl_podcasts.thumbnail_name"
                        .concat(",(select distinct count(tbl_podcast_episodes.pid ) from tbl_podcast_episodes where tbl_podcast_episodes.pid = tbl_podcasts.id and tbl_podcast_episodes.New = 1)")
                        .concat("FROM [tbl_podcasts] ")
                        .concat("JOIN tbl_podcast_episodes ")
                        .concat("ON tbl_podcast_episodes.pid = tbl_podcasts.id ")
                        .concat("WHERE tbl_podcast_episodes.new = 1 ")
                        .concat("GROUP BY tbl_podcast_episodes.pid ")
                        .concat("ORDER BY ")
                        .concat(orderString);
            else if (showDownloaded)
                sql = "SELECT tbl_podcasts.id,tbl_podcasts.title,tbl_podcasts.url,tbl_podcasts.thumbnail_url,tbl_podcasts.thumbnail_name"
                        .concat(",(select distinct count(tbl_podcast_episodes.pid ) from tbl_podcast_episodes where tbl_podcast_episodes.pid = tbl_podcasts.id and tbl_podcast_episodes.New = 1) ")
                        .concat("FROM [tbl_podcasts] ")
                        .concat("JOIN tbl_podcast_episodes ")
                        .concat("ON tbl_podcast_episodes.pid = tbl_podcasts.id ")
                        .concat("WHERE tbl_podcast_episodes.download = 1 ")
                        .concat("GROUP BY tbl_podcast_episodes.pid ")
                        .concat("ORDER BY ")
                        .concat(orderString);
            else
                sql = "SELECT tbl_podcasts.id,tbl_podcasts.title,tbl_podcasts.url,tbl_podcasts.thumbnail_url,tbl_podcasts.thumbnail_name"
                        .concat(",(select distinct count(tbl_podcast_episodes.pid ) from tbl_podcast_episodes where tbl_podcast_episodes.pid = tbl_podcasts.id and tbl_podcast_episodes.New = 1) ")
                        .concat("FROM [tbl_podcasts] ")
                        .concat("ORDER BY ")
                        .concat(orderString);

            /*
                sql = "SELECT tbl_podcasts.id,tbl_podcasts.title,tbl_podcasts.url,tbl_podcasts.thumbnail_url,tbl_podcasts.thumbnail_name"
                        .concat(",(select distinct count(tbl_podcast_episodes.pid ) from tbl_podcast_episodes where tbl_podcast_episodes.pid = tbl_podcasts.id and tbl_podcast_episodes.New = 1) ")
                        .concat("FROM [tbl_podcasts] ")
                        .concat("LEFT JOIN tbl_podcast_episodes ")
                        .concat("ON tbl_podcast_episodes.pid = tbl_podcasts.id ")
                        .concat("GROUP BY tbl_podcast_episodes.pid ")
                        .concat("ORDER BY ")
                        .concat(orderString);
*/
            final Cursor cursor = sdb.rawQuery(sql, null);

            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {

                    final PodcastItem podcast = new PodcastItem();

                    podcast.setPodcastId(cursor.getInt(0));

                    final ChannelItem channel = new ChannelItem();
                    channel.setTitle(cursor.getString(1));
                    channel.setRSSUrl(cursor.getString(2));
                    if (cursor.getString(3) != null && cursor.getString(4) != null) {
                        channel.setThumbnailUrl(cursor.getString(3));
                        channel.setThumbnailName(cursor.getString(4));
                    }
                    podcast.setChannel(channel);

                    podcast.setDisplayThumbnail(GetRoundedLogo(ctx, podcast.getChannel()));
                    podcast.setNewCount(cursor.getInt(5));
                    if (orderId == latestEpisodesSortOrderID)
                        podcast.setLatestEpisode(GetLatestEpisode(ctx, cursor.getInt(0)));

                    podcasts.add(podcast);

                    cursor.moveToNext();
                }
            }
            cursor.close();
            db.close();
            sdb.close();

            if (orderId == Enums.SortOrder.NEWEPISODES.getSorderOrderCode()) {
                Collections.sort(podcasts, new Comparator<PodcastItem>() {
                    @Override
                    public int compare(final PodcastItem item1, final PodcastItem item2) {
                        return Integer.compare(item2.getNewCount(), item1.getNewCount());
                    }
                });
            }

            if (orderId == latestEpisodesSortOrderID) {
                try {
                    Collections.sort(podcasts, new Comparator<PodcastItem>() {
                        @Override
                        public int compare(final PodcastItem item1, final PodcastItem item2) {
                            return DateUtils.ConvertDate(item2.getLatestEpisode().getPubDate()).compareTo(DateUtils.ConvertDate(item1.getLatestEpisode().getPubDate()));
                        }
                    });
                } catch (Exception ex) {
                    //CommonUtils.showToast(ctx, ctx.getString(R.string.alert_error_sorting_latest_episodes));
                }
            }

            //final JsonElement element = gson.toJsonTree(podcasts, new TypeToken<List<PodcastItem>>() { }.getType());
            //final JsonArray jsonArray = element.getAsJsonArray();
            //CacheUtils.savePodcastsCache(ctx, jsonArray.toString());
        }
        //else {
        //podcasts = gson.fromJson(cache, new TypeToken<List<PodcastItem>>() {}.getType());
        //for (final PodcastItem podcast : podcasts)
        //podcast.setDisplayThumbnail(GetRoundedLogo(ctx, podcast.getChannel(), R.drawable.ic_thumb_default));
        //}

        return podcasts;
    }
}
