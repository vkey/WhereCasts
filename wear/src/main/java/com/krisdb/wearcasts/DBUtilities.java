package com.krisdb.wearcasts;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.ArrayMap;

import com.krisdb.wearcastslibrary.ChannelItem;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.GetRoundedLogo;
import static com.krisdb.wearcastslibrary.DateUtils.GetDisplayDate;

public class DBUtilities {
    private static final String mEpisodeColumns = "tbl_podcast_episodes.id,tbl_podcast_episodes.pid,tbl_podcast_episodes.title,tbl_podcast_episodes.description,tbl_podcast_episodes.url,tbl_podcast_episodes.mediaurl,tbl_podcast_episodes.pubDate,tbl_podcast_episodes.read,tbl_podcast_episodes.finished,tbl_podcast_episodes.position,tbl_podcast_episodes.duration,tbl_podcast_episodes.download";

    static Boolean playlistIsEmpty(final Context ctx, final int playlistId)
    {
        Boolean output;
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT [id] FROM tbl_playlists_xref WHERE [playlist_id] = ?", new String[]{String.valueOf(playlistId)});

        output = !cursor.moveToFirst();

        cursor.close();
        db.close();
        sdb.close();

        return output;
    }

    static List<PodcastItem> getPlaylistItems(final Context ctx, final int playlistId, final Boolean isLocal) {
        return (isLocal) ? DBUtilities.GetEpisodes(ctx, 0, playlistId) : DBUtilities.GetLocalFiles(ctx);
    }

    static List<PlaylistItem> getPlaylists(final Context ctx) {
        return getPlaylists(ctx, false);    }

     static List<PlaylistItem> getPlaylists(final Context ctx, final Boolean hideEmpty)
    {
        final List<PlaylistItem> playlists = new ArrayList<>();
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        String sql = hideEmpty ?
                "SELECT tbl_playlists.playlist_id, tbl_playlists.name FROM tbl_playlists JOIN tbl_playlists_xref ON tbl_playlists_xref.playlist_id = tbl_playlists.playlist_id GROUP BY tbl_playlists.playlist_id HAVING count(tbl_playlists_xref.playlist_id) > 0"  :
                "SELECT [playlist_id], [name] FROM tbl_playlists";

        final Cursor cursor = sdb.rawQuery(sql, null);

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                final PlaylistItem playlist = new PlaylistItem();
                playlist.setID(cursor.getInt(0));
                playlist.setName(cursor.getString(1));
                playlists.add(playlist);
                cursor.moveToNext();
            }
        }
        cursor.close();
        db.close();
        sdb.close();

        return playlists;
    }

    static void SaveEpisodeValue(final Context ctx, final PodcastItem episode, final String field, long value) {
        if (ctx == null || episode == null || field == null) return;

        final ContentValues cv = new ContentValues();
        cv.put(field, value);

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        db.update(cv, episode.getEpisodeId());
        db.close();
    }

    public static void SaveEpisodeValue(final Context ctx, final PodcastItem episode, final String field, Integer value) {
        final ContentValues cv = new ContentValues();
        cv.put(field, value);

        new DBPodcastsEpisodes(ctx).update(cv, episode.getEpisodeId());
    }

    public static void SaveEpisodeValue(final Context ctx, final PodcastItem episode, final String field, String value) {
        final ContentValues cv = new ContentValues();
        cv.put(field, value);

        new DBPodcastsEpisodes(ctx).update(cv, episode.getEpisodeId());
    }

    static PodcastItem GetEpisodeByDownloadID(final Context ctx, final int downloadId) {
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);

        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [downloadid] = ?"), new String[]{String.valueOf(downloadId)});

        PodcastItem podcast = new PodcastItem();

        if (cursor.moveToFirst()) {
            podcast = SetPodcastEpisode(cursor);
        }

        cursor.close();
        db.close();
        sdb.close();

        return podcast;
    }

    static int GetDownloadIDByEpisode(final Context ctx, final PodcastItem episode) {
        int downloadId = -1;

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT [downloadid] FROM [tbl_podcast_episodes] WHERE [id] = ?", new String[]{String.valueOf(episode.getEpisodeId())});

        if (cursor.moveToFirst())
            downloadId = cursor.getInt(0);

        cursor.close();
        db.close();
        sdb.close();

        return downloadId;
    }

    static PodcastItem GetEpisodeByTitle(final Context ctx, final String title) {
        PodcastItem episode = null;

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [title] = ?"), new String[]{String.valueOf(title)});

        if (cursor.moveToFirst())
            episode = SetPodcastEpisode(cursor);

        cursor.close();
        db.close();
        sdb.close();

        return episode;
    }

    static Boolean HasNewEpisodes(final Context ctx) {
        Boolean output;
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT id FROM [tbl_podcast_episodes] WHERE [new] = 1", null);

        output = cursor.moveToFirst();

        cursor.close();
        db.close();
        sdb.close();

        return output;
    }

    static int GetEpisodeValue(final Context ctx, final PodcastItem episode, final String field) {

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT [".concat(field).concat("] FROM [tbl_podcast_episodes] WHERE [id] = ?"),
                new String[]{String.valueOf(episode.getEpisodeId())
                });

        int value = 0;

        if (cursor.moveToFirst())
            value = cursor.getInt(0);

        cursor.close();
        db.close();
        sdb.close();

        return value;
    }

    public static PodcastItem GetEpisodePlaying(Context ctx) {

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [playing] = 1"), null);

        PodcastItem episode = null;
        if (cursor.moveToFirst()) {
            episode = DBUtilities.SetPodcastEpisode(cursor);
        }

        cursor.close();
        db.close();
        sdb.close();

        return episode;
    }

    public static Boolean HasNewEpisodes(final Context ctx, final int podcastId) {

        Boolean output;
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();
        final Cursor cursor = sdb.rawQuery("SELECT [id] FROM [tbl_podcast_episodes] WHERE [new] = 1 AND [pid] = ?", new String[]{String.valueOf(podcastId)});

        output = cursor.moveToFirst();

        cursor.close();
        sdb.close();
        db.close();
        return  output;
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

    static void TrimEpisodes(final Context ctx, final PodcastItem podcast) {
        //Log.d(ctx.getPackageName(), "Trimming " + podcast.getPodcastId());
        final SQLiteDatabase sdb = DatabaseHelper.select(ctx);

        final String numberOfEpisode = PreferenceManager.getDefaultSharedPreferences(ctx).getString("pref_episode_limit", ctx.getString(R.string.episode_list_default));

        sdb.execSQL("DELETE FROM [tbl_podcast_episodes] WHERE [id] IN (SELECT [id] FROM [tbl_podcast_episodes] WHERE [pid] = ? ORDER BY [pubdate] DESC LIMIT 1000 OFFSET ".concat(numberOfEpisode).concat(")"),
                new String[]{String.valueOf(podcast.getPodcastId())});
    }

    static int UnplayedEpisodeCount(final Context ctx, final int podcastId) {

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery(
                "SELECT [id] FROM [tbl_podcast_episodes] WHERE [finished] = 0 AND [pid] = ?",
                new String[]{String.valueOf(podcastId)
                });

        final int count = cursor.getCount();

        cursor.close();
        db.close();
        sdb.close();

        return count;
    }

    static int NewEpisodeCount(final Context ctx, final int podcastId) {

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery(
                "SELECT [id] FROM [tbl_podcast_episodes] WHERE [new] = 1 AND [pid] = ?",
                new String[]{String.valueOf(podcastId)
                });

        final int count = cursor.getCount();

        cursor.close();
        db.close();
        sdb.close();

        return count;
    }

    static PodcastItem GetPodcast(final Context ctx, final int podcastId) {
        final PodcastItem podcast = new PodcastItem();
        final DBPodcasts db = new DBPodcasts(ctx);
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

        return podcast;
    }

    static ChannelItem GetChannel(final Context ctx, final int podcastId) {
        final ChannelItem channel = new ChannelItem();
        final DBPodcasts db = new DBPodcasts(ctx);
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

    static PodcastItem getNextEpisodeNotDownloaded(final Context ctx, final PodcastItem episode) {
        PodcastItem output;

        final List<PodcastItem> episodes = DBUtilities.GetEpisodes(ctx, episode.getPodcastId());

        final int size = episodes.size() - 1;
        final int episodeId = episode.getEpisodeId();
        int index = 0;
        for (int i = 0; i <= size; i++) {
            if (episodeId == episodes.get(i).getEpisodeId()) {
                index = i;
                break;
            }
        }

        output = getNextEpisodeNotDownloaded(episodes, index - 1);

        return output;
    }

    private static PodcastItem getNextEpisodeNotDownloaded(final List<PodcastItem> episodes, int index) {

        if (episodes.get(index).getIsTitle())
            return null;

        if (episodes.get(index).getIsDownloaded() == false)
            return episodes.get(index);

        return getNextEpisodeNotDownloaded(episodes, index - 1);
    }

    static List<PodcastItem> GetPodcasts(final Context ctx) {
        return GetPodcasts(ctx, false);
    }

        static List<PodcastItem> GetPodcasts(final Context ctx, final Boolean hideEmpty) {
        List<PodcastItem> podcasts = new ArrayList<>();
        //final Gson gson = new Gson();

        //final String cache = CacheUtils.getPodcastsCache(ctx);

        //if (cache == null)
        {

            final DBPodcasts db = new DBPodcasts(ctx);
            final SQLiteDatabase sdb = db.select();

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

            int orderId = Integer.valueOf(prefs.getString("pref_display_podcasts_sort_order", String.valueOf(ctx.getResources().getInteger(R.integer.default_podcasts_sort_order))));

            final String orderString = Utilities.GetOrderClause(orderId);
            final int latestEpisodesSortOrderID = Enums.SortOrder.LATESTEPISODES.getSorderOrderCode();

            final Cursor cursor = sdb.rawQuery("SELECT [id],[title],[url],[thumbnail_url],[thumbnail_name] FROM [tbl_podcasts] ORDER BY ".concat(orderString), null);

            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {

                    if (hideEmpty && NewEpisodeCount(ctx, cursor.getInt(0)) == 0) {
                        cursor.moveToNext();
                        continue;
                    }

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

                    podcast.setNewCount(DBUtilities.NewEpisodeCount(ctx, podcast.getPodcastId()));
                    podcast.setDisplayThumbnail(GetRoundedLogo(ctx, podcast.getChannel(), R.drawable.ic_thumb_default));

                    if (orderId == latestEpisodesSortOrderID)
                        podcast.setLatestEpisode(DBUtilities.GetLatestEpisode(ctx, cursor.getInt(0)));

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

    static PodcastItem GetEpisode(final Context ctx, final int episodeId)
    {
        PodcastItem episode = new PodcastItem();

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery(
                "SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE id = ?"),
                new String[] { String.valueOf(episodeId) }
        );

        if (cursor.moveToFirst())
            episode = SetPodcastEpisode(cursor);

        episode.setChannel(DBUtilities.GetChannel(ctx, episode.getPodcastId()));

        cursor.close();
        db.close();
        sdb.close();

        return episode;
    }

    static PodcastItem GetPlayingEpisode(final Context ctx)
    {
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [playing] = 1"), null);

        PodcastItem podcast = new PodcastItem();

        if (cursor.moveToFirst()) {
            podcast = SetPodcastEpisode(cursor);
        }
        cursor.close();
        db.close();
        sdb.close();

        return podcast;
    }

    static PodcastItem GetLatestEpisode(final Context ctx, final int podcastId)
    {
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery(
                "SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE pid = ? ORDER BY [pubDate] DESC LIMIT 1"),
                new String[] { String.valueOf(podcastId) }
        );

        PodcastItem podcast = new PodcastItem();

        if (cursor.moveToFirst())
            podcast = SetPodcastEpisode(cursor);

        cursor.close();
        db.close();
        sdb.close();

        return podcast;
    }

    static List<PodcastItem> GetEpisodesWithDownloads(final Context ctx, final int podcastId) {
        final List<PodcastItem> podcasts = new ArrayList<>();

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [download] = 1 AND [pid] = ? ORDER BY [pubDate] DESC"), new String[]{String.valueOf(podcastId)});

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {

                final PodcastItem podcast = SetPodcastEpisode(cursor);
                podcast.setChannel(GetChannel(ctx, cursor.getInt(1)));
                podcasts.add(podcast);

                cursor.moveToNext();
            }
        }

        cursor.close();
        db.close();
        sdb.close();

        return podcasts;
    }

    static List<PodcastItem> GetLocalFiles(final Context ctx) {
        List<PodcastItem> episodes = new ArrayList<>();
        final PodcastItem titleItem = new PodcastItem();

        ChannelItem channelItem = new ChannelItem();
        channelItem.setTitle(ctx.getString(R.string.playlist_title_local));
        titleItem.setIsTitle(true);
        titleItem.setChannel(channelItem);
        episodes.add(titleItem);
        final File dirLocal = new File(GetLocalDirectory());

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


    static String getPlaylistName(final Context ctx, final int playlistId) {
        String name = null;
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();
        final Cursor cursor = sdb.rawQuery("SELECT [name] FROM [tbl_playlists] WHERE [playlist_id] = ? ", new String[]{String.valueOf(playlistId)});

        if (cursor.moveToFirst())
            name = cursor.getString(0);

        cursor.close();
        sdb.close();
        db.close();

        return name;
    }

    static Boolean episodeExists(final Context ctx, final String url) {
        Boolean exists;
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();
        final Cursor cursor = sdb.rawQuery("SELECT [id] FROM [tbl_podcast_episodes] WHERE [mediaurl] = ? ", new String[]{url});

        exists = cursor.moveToFirst();

        cursor.close();
        sdb.close();
        db.close();

        return exists;
    }
    static Boolean playlistExists(final Context ctx, final int playlistId) {
        Boolean exists = false;
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();
        final Cursor cursor = sdb.rawQuery("SELECT [playlist_id] FROM [tbl_playlists] WHERE [playlist_id] = ? ", new String[]{String.valueOf(playlistId)});

        if (cursor.moveToFirst())
            exists = true;

        cursor.close();
        sdb.close();
        db.close();

        return exists;
    }

    static List<PodcastItem> GetEpisodes(final Context ctx, final int podcastId) {
        return GetEpisodes(ctx, podcastId, ctx.getResources().getInteger(R.integer.playlist_default));
    }

    static List<PodcastItem> GetEpisodes(final Context ctx, final int podcastId, final String orderBy) {
        return GetEpisodes(ctx, podcastId, ctx.getResources().getInteger(R.integer.playlist_default), false, 200, orderBy);
    }

    static List<PodcastItem> GetEpisodesFiltered(final Context ctx, final int podcastId) {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        final Boolean hidePlayed = prefs.getBoolean("pref_" + podcastId + "_hide_played", false);

        final int numberOfEpisode = Integer.valueOf(prefs.getString("pref_episode_limit", ctx.getString(R.string.episode_list_default)));

        return GetEpisodes(ctx, podcastId, ctx.getResources().getInteger(R.integer.playlist_default), hidePlayed, numberOfEpisode, null);
    }


    static Boolean HasEpisodes(final Context ctx, final int podcastId, final int playlistId) {
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        Boolean output;

        Cursor cursor;

        if (playlistId != 0)
            cursor = sdb.rawQuery("SELECT id FROM [tbl_playlists_xref] WHERE [playlist_id] = ?", new String[]{String.valueOf(playlistId)});
        else
            cursor = sdb.rawQuery("SELECT id FROM [tbl_podcast_episodes] WHERE [pid] = ?", new String[]{String.valueOf(podcastId)});

        output = cursor.moveToFirst();

        sdb.close();
        db.close();

        return output;
    }

    static List<PodcastItem> GetEpisodes(final Context ctx, final int podcastId, final int playlistId) {
            return GetEpisodes(ctx, podcastId, playlistId, false, Integer.MAX_VALUE, null);
    }

    static List<PodcastItem> GetEpisodes(final Context ctx, final int podcastId, final int playlistId, final boolean hidePlayed, final int limit, final String orderBy) {
            return GetEpisodes(ctx, podcastId, playlistId, hidePlayed, limit, null, orderBy);
    }

    static List<PodcastItem> SearchEpisodes(final Context ctx, final int podcastId, final String query) {
            return GetEpisodes(ctx, podcastId, ctx.getResources().getInteger(R.integer.playlist_default), false, Integer.MAX_VALUE, query, null);
    }

    static List<PodcastItem> GetEpisodes(final Context ctx, final int podcastId, final int playlistId, final boolean hidePlayed, final int limit, String query, final String orderBy) {
        final Resources resources = ctx.getResources();

        List<PodcastItem> episodes = new ArrayList<>();

        final PodcastItem titleItem = new PodcastItem();
        titleItem.setTitle("");
        titleItem.setIsTitle(true);
        ChannelItem channelItem = new ChannelItem();

        if (playlistId == resources.getInteger(R.integer.playlist_downloads))
            channelItem.setTitle(ctx.getString(R.string.playlist_title_downloads));
        else if (playlistId == resources.getInteger(R.integer.playlist_playerfm)) //third party
            channelItem.setTitle(ctx.getString(R.string.third_party_title_playerfm));
        else if (playlistId == resources.getInteger(R.integer.playlist_inprogress))
            channelItem.setTitle(ctx.getString(R.string.playlist_title_inprogress));
        else if (playlistId == resources.getInteger(R.integer.playlist_upnext))
            channelItem.setTitle(ctx.getString(R.string.playlist_title_upnext));
        else if (playlistId == resources.getInteger(R.integer.playlist_unplayed))
            channelItem.setTitle(ctx.getString(R.string.playlist_title_unplayed));
        else if (playlistId > ctx.getResources().getInteger(R.integer.playlist_default) && DBUtilities.playlistExists(ctx, playlistId))
            channelItem.setTitle(getPlaylistName(ctx, playlistId));
        else
            channelItem = GetChannel(ctx, podcastId);

        if (playlistId == ctx.getResources().getInteger(R.integer.playlist_default))
            titleItem.setDisplayThumbnail(GetRoundedLogo(ctx , channelItem, R.drawable.ic_thumb_title_default));

        titleItem.setPodcastId(podcastId);

        titleItem.setChannel(channelItem);
        episodes.add(titleItem);

        if (playlistId != resources.getInteger(R.integer.playlist_local)) {

            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
            final SQLiteDatabase sdb = db.select();

            Cursor cursor;

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            int order;

            if (playlistId == resources.getInteger(R.integer.playlist_default)) {
                final int podcastOrder = Integer.valueOf(prefs.getString("pref_" + podcastId + "_sort_order", String.valueOf(ctx.getResources().getInteger(R.integer.default_episodes_sort_order))));
                if (podcastOrder != 0)
                    order = podcastOrder;
                else
                    order = Integer.valueOf(prefs.getString("pref_display_episodes_sort_order", String.valueOf(ctx.getResources().getInteger(R.integer.default_episodes_global_sort_order)))); //global sort order
            }
            else
                order = Integer.valueOf(prefs.getString("pref_display_playlist_sort_order", String.valueOf(ctx.getResources().getInteger(R.integer.default_playlist_sort_order))));

        final String orderString = orderBy == null ?  Utilities.GetOrderClause(order) : orderBy;

        if (playlistId == resources.getInteger(R.integer.playlist_default)) //regular episodes
            {
                if (hidePlayed)
                    cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [finished] = 0 AND [pid] = ? ORDER BY ".concat(orderString).concat(" LIMIT ".concat(String.valueOf(limit)))), new String[]{String.valueOf(podcastId)});
                else if (query != null) {
                    query = "%".concat(query.toLowerCase()).concat("%");
                    cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [pid] = ? AND ([title] LIKE ? OR [description] LIKE ?) ORDER BY [pubDate]"), new String[]{String.valueOf(podcastId), query, query});
                }
                else
                    cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [pid] = ? ORDER BY ".concat(orderString).concat(" LIMIT ".concat(String.valueOf(limit)))), new String[]{String.valueOf(podcastId)});
            }
            else if (playlistId == resources.getInteger(R.integer.playlist_downloads)) //downloads can also be in progress, so need separate query for downloads
                cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [download] = 1 AND [downloadid] = 0 ORDER BY ".concat(orderString)), null);
            else if (playlistId == resources.getInteger(R.integer.playlist_unplayed))
                cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [finished] = 0 ORDER BY ".concat(orderString)), null);
            else if (playlistId == resources.getInteger(R.integer.playlist_inprogress))
                cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [position] > 0 ORDER BY ".concat(orderString)), null);
            else if (playlistId > resources.getInteger(R.integer.playlist_default) || playlistId <= resources.getInteger(R.integer.playlist_playerfm))
                cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM tbl_podcast_episodes INNER JOIN tbl_playlists_xref ON tbl_playlists_xref.episode_id = tbl_podcast_episodes.id WHERE tbl_playlists_xref.playlist_id = ? ORDER BY ".concat(orderString)), new String[]{String.valueOf(playlistId)});
            else
                cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [upnext] = 1 ORDER BY ".concat(orderString)), null);

            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    PodcastItem episode = SetPodcastEpisode(cursor);

                    episode.setDisplayDate(GetDisplayDate(ctx, cursor.getString(6)));

                    if (playlistId == resources.getInteger(R.integer.playlist_inprogress) && episode.getPosition() > 0) //in progress
                        episode.setTitle(episode.getTitle().concat(" (").concat(DateUtils.FormatPositionTime(episode.getPosition())).concat(")"));

                    episode.setChannel(GetChannel(ctx, cursor.getInt(1)));

                    if (playlistId != resources.getInteger(R.integer.playlist_default))
                        episode.setDisplayThumbnail(GetRoundedLogo(ctx, episode.getChannel(), R.drawable.ic_thumb_playlist_default));

                    episodes.add(episode);
                    cursor.moveToNext();
                }
            }

            cursor.close();
            sdb.close();
            db.close();
        }
        else
        {
            episodes = GetLocalFiles(ctx);
        }

        return episodes;
    }

    static PodcastItem SetPodcastEpisode(final Cursor cursor)
    {
        final PodcastItem episode = new PodcastItem();
        final ColumnIndexCache cache = new ColumnIndexCache();

        episode.setEpisodeId(cursor.getInt(cache.getColumnIndex(cursor, "id")));
        episode.setPodcastId(cursor.getInt(cache.getColumnIndex(cursor, "pid")));
        episode.setTitle(cursor.getString(cache.getColumnIndex(cursor, "title")));

        if (cursor.getString(3) != null)
            episode.setDescription(cursor.getString(cache.getColumnIndex(cursor, "description")));

        if (cursor.getString(4) != null)
            episode.setEpisodeUrl(cursor.getString(cache.getColumnIndex(cursor, "url")));

        if (cursor.getString(5) != null)
            episode.setMediaUrl(cursor.getString(cache.getColumnIndex(cursor, "mediaurl")));

        episode.setPubDate(cursor.getString(cache.getColumnIndex(cursor, "pubDate")));
        episode.setRead(cursor.getInt(cache.getColumnIndex(cursor, "read")) == 1);
        episode.setFinished(cursor.getInt(cache.getColumnIndex(cursor, "finished")) == 1);
        episode.setPosition(cursor.getInt(cache.getColumnIndex(cursor, "position")));
        episode.setDuration(cursor.getInt(cache.getColumnIndex(cursor, "duration")));
        episode.setDisplayDuration(DateUtils.FormatPositionTime(cursor.getInt(cache.getColumnIndex(cursor, "duration"))));
        episode.setIsDownloaded(cursor.getInt(cache.getColumnIndex(cursor, "download")) == 1);

        return episode;
    }
}
