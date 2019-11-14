package com.krisdb.wearcasts.Utilities;


import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.ArrayMap;

import com.krisdb.wearcasts.Async.SaveLogo;
import com.krisdb.wearcasts.Async.SyncPodcasts;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.ChannelItem;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;

public class DBUtilities {

    public static void insertPodcast(final Context context, final PodcastItem podcast) {
        insertPodcast(context, new DBPodcastsEpisodes(context), podcast, true, true);
    }

    public static void insertPodcast(final Context context, final DBPodcastsEpisodes db, final PodcastItem podcast, final boolean fetchArt, final boolean fetchEpisodes)
    {
        final ContentValues cv = new ContentValues();
        cv.put("title", podcast.getTitle());
        cv.put("url", podcast.getChannel().getRSSUrl().toString());
        cv.put("site_url", podcast.getChannel().getSiteUrl() != null ? podcast.getChannel().getSiteUrl().toString() : null);
        cv.put("dateAdded", DateUtils.GetDate());
        String thumbnailUrl = null;
        String fileName = null;
        if (podcast.getChannel().getThumbnailUrl() != null) {
            thumbnailUrl = podcast.getChannel().getThumbnailUrl().toString();
            fileName = podcast.getChannel().getThumbnailName();
            if (fetchArt)
                CommonUtils.executeSingleThreadAsync(new SaveLogo(context, thumbnailUrl, fileName), (response) -> { });
        }

        cv.put("thumbnail_url", thumbnailUrl);
        cv.put("thumbnail_name", fileName);

        final int podcastId = (int)db.insertPodcast(cv);

        if (fetchEpisodes)
            CommonUtils.executeSingleThreadAsync(new SyncPodcasts(context, podcastId), (response) -> { });
    }

    static ChannelItem GetChannel(final Context ctx, final int podcastId) {
        final ChannelItem channel = new ChannelItem();
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery(
                "SELECT [id],[title],[url],[thumbnail_url],[thumbnail_name],[description] FROM [tbl_podcasts] WHERE [id] = ?",
                new String[]{String.valueOf(podcastId)
                });

        if (cursor.moveToFirst()) {
            channel.setRSSUrl(cursor.getString(2));
            if (cursor.getString(3) != null && cursor.getString(4) != null) {
                channel.setThumbnailUrl(cursor.getString(3));
                channel.setThumbnailName(cursor.getString(4));
            }

            if (cursor.getString(1) != null)
                channel.setTitle(cursor.getString(1));

            if (cursor.getString(5) != null)
                channel.setDescription(cursor.getString(5));
        }

        cursor.close();
        db.close();

        return channel;
    }

    public static List<PodcastItem> GetLocalFiles(final Context ctx) {
        List<PodcastItem> episodes = new ArrayList<>();
        final PodcastItem titleItem = new PodcastItem();

        ChannelItem channelItem = new ChannelItem();
        channelItem.setTitle(ctx.getString(R.string.playlist_title_local));
        titleItem.setIsTitle(true);
        titleItem.setChannel(channelItem);
        episodes.add(titleItem);
        final File dirLocal = new File(GetLocalDirectory(ctx));

        if (dirLocal.exists()) {
            final File[] files = dirLocal.listFiles();

            if (files != null && files.length > 0) {
                for (File file : files) {
                    PodcastItem localEpisode = new PodcastItem();
                    localEpisode.setIsLocal(true);
                    localEpisode.setTitle(file.getName());
                    localEpisode.setPubDate(new Date(file.lastModified()).toString());
                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
                    localEpisode.setPosition(prefs.getInt("local_file_position_".concat(Utilities.GetLocalPositionKey(file.getName())), 0));
                    episodes.add(localEpisode);
                }
            }
        }

        return episodes;
    }

    public static class ColumnIndexCache {
        private final ArrayMap<String, Integer> mMap = new ArrayMap<>();

        int getColumnIndex(final Cursor cursor, final String columnName) {
            if (!mMap.containsKey(columnName))
                mMap.put(columnName, cursor.getColumnIndex(columnName));

            return mMap.get(columnName);
        }

        public void clear() {
            mMap.clear();
        }
    }


}
