package com.krisdb.wearcasts.Utilities;


import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.preference.PreferenceManager;
import android.util.ArrayMap;

import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Databases.DatabaseHelper;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
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

import static com.krisdb.wearcasts.Utilities.DBUtilities.GetChannel;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylistName;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.playlistExists;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.GetRoundedLogo;
import static com.krisdb.wearcastslibrary.DateUtils.GetDisplayDate;

public class EpisodeUtilities {
    private static final String mEpisodeColumns = "tbl_podcast_episodes.id,tbl_podcast_episodes.pid,tbl_podcast_episodes.title,tbl_podcast_episodes.description,tbl_podcast_episodes.url,tbl_podcast_episodes.mediaurl,tbl_podcast_episodes.pubDate,tbl_podcast_episodes.read,tbl_podcast_episodes.finished,tbl_podcast_episodes.position,tbl_podcast_episodes.duration,tbl_podcast_episodes.download";

    public static void markPlayed(final Context ctx, final PodcastItem episode)
    {
        final ContentValues cv = new ContentValues();
        cv.put("finished", 1);
        cv.put("position", 0);

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        db.update(cv, episode.getEpisodeId());
        db.close();

        CommonUtils.showToast(ctx, ctx.getString(R.string.alert_marked_played));
    }

    public static void markUnplayed(final Context ctx, final PodcastItem episode)
    {
        final ContentValues cv = new ContentValues();
        cv.put("finished", 0);

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        db.update(cv, episode.getEpisodeId());
        db.close();

        CommonUtils.showToast(ctx, ctx.getString(R.string.alert_marked_unplayed));
    }

    public static void SaveEpisodeValue(final Context ctx, final PodcastItem episode, final String field, long value) {
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

    public static PodcastItem GetEpisodeByDownloadID(final Context ctx, final int downloadId) {
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);

        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM tbl_podcast_episodes WHERE tbl_podcast_episodes.downloadid = ?"), new String[]{String.valueOf(downloadId)});

        PodcastItem podcast = new PodcastItem();

        if (cursor.moveToFirst()) {
            podcast = SetPodcastEpisode(cursor);
        }

        cursor.close();
        db.close();
        sdb.close();

        return podcast;
    }
    static List<PodcastItem> GetDownloadedEpisodes(final Context ctx, final int podcastId) {
        return GetDownloadedEpisodes(ctx, podcastId, -1);
    }

    static List<PodcastItem> GetDownloadedEpisodes(final Context ctx, final int podcastId, final int count) {
        final List<PodcastItem> episodes = new ArrayList<>();

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();
        Cursor cursor;

        if (count== -1)
            cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM tbl_podcast_episodes WHERE tbl_podcast_episodes.pid = ? AND tbl_podcast_episodes.download = 0 AND tbl_podcast_episodes.downloadid = 0"), new String[]{String.valueOf(podcastId)});
        else
            cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM tbl_podcast_episodes WHERE tbl_podcast_episodes.pid = ? AND tbl_podcast_episodes.download = 0 AND tbl_podcast_episodes.downloadid = 0 LIMIT -1 OFFSET ".concat(String.valueOf(count))), new String[]{String.valueOf(podcastId)});

        if (cursor.moveToFirst()) {
            episodes.add(SetPodcastEpisode(cursor));
            cursor.moveToNext();
        }

        cursor.close();
        db.close();
        sdb.close();

        return episodes;
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

    public static PodcastItem GetEpisodeByTitle(final Context ctx, final String title) {
        PodcastItem episode = null;

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM tbl_podcast_episodes WHERE tbl_podcast_episodes.title = ?"), new String[]{String.valueOf(title)});

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

    public static int GetEpisodeValue(final Context ctx, final PodcastItem episode, final String field) {

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

        final Cursor cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM tbl_podcast_episodes.tbl_podcast_episodes WHERE tbl_podcast_episodes.playing = 1"), null);

        PodcastItem episode = null;
        if (cursor.moveToFirst()) {
            episode = EpisodeUtilities.SetPodcastEpisode(cursor);
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

     public static void TrimEpisodes(final Context ctx, final PodcastItem podcast) {
        //Log.d(ctx.getPackageName(), "Trimming " + podcast.getPodcastId());
        final SQLiteDatabase sdb = DatabaseHelper.select(ctx);

        final String numberOfEpisode = PreferenceManager.getDefaultSharedPreferences(ctx).getString("pref_episode_limit", ctx.getString(R.string.episode_list_default));

        //sdb.execSQL("DELETE FROM [tbl_podcast_episodes] WHERE [id] IN (SELECT [id] FROM [tbl_podcast_episodes] WHERE [pid] = ? ORDER BY [pubdate] DESC LIMIT 1000 OFFSET ".concat(numberOfEpisode).concat(")"),
        sdb.execSQL("DELETE FROM [tbl_podcast_episodes] WHERE [id] IN (SELECT [id] FROM [tbl_podcast_episodes] WHERE [pid] = ? AND downloadid = 0 AND download = 0 AND position = 0 AND playing = 0 ORDER BY [pubdate] DESC LIMIT 1000 OFFSET ".concat(numberOfEpisode).concat(") AND [id] NOT IN (SELECT pe.id FROM tbl_podcast_episodes AS pe JOIN tbl_playlists_xref AS pex ON pe.id == pex.episode_id WHERE pe.pid = 49)"),
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
        int count;

        try (
                SQLiteDatabase sdb = db.select();
                Cursor cursor = sdb.rawQuery("SELECT [id] FROM [tbl_podcast_episodes] WHERE [new] = 1 AND [pid] = ?", new String[]{String.valueOf(podcastId) }))
        {
            count = cursor.getCount();
        } catch (SQLiteException ex) {
            count = 0;
        } finally {
            db.close();
        }

        return count;
    }

    public static PodcastItem getNextEpisodeNotDownloaded(final Context ctx, final PodcastItem episode) {
        PodcastItem output;

        final List<PodcastItem> episodes = EpisodeUtilities.GetEpisodes(ctx, episode.getPodcastId());

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

    public static PodcastItem GetEpisode(final Context ctx, final int episodeId) {
        return GetEpisode(ctx, episodeId, Integer.MAX_VALUE);
    }

    public static PodcastItem GetEpisode(final Context ctx, final int episodeId, final int playlistId)
    {
        PodcastItem episode = new PodcastItem();

        try {
            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
            final SQLiteDatabase sdb = db.select();

            final Cursor cursor = sdb.rawQuery(
                    "SELECT ".concat(mEpisodeColumns).concat(" FROM tbl_podcast_episodes WHERE tbl_podcast_episodes.id = ?"),
                    new String[]{String.valueOf(episodeId)}
            );

            if (cursor.moveToFirst())
                episode = SetPodcastEpisode(cursor);

            episode.setChannel(GetChannel(ctx, episode.getPodcastId()));

            //third party: add playlist
            if (playlistId == ctx.getResources().getInteger(R.integer.playlist_playerfm))
                episode.setPlaylistId(ctx.getResources().getInteger(R.integer.playlist_playerfm));

            cursor.close();
            db.close();
            sdb.close();
        }
        catch(SQLiteException ex)
        {
            ex.printStackTrace();
        }

        return episode;
    }

    public static PodcastItem GetPlayingEpisode(final Context ctx)
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

    public static PodcastItem GetLatestEpisode(final Context ctx, final int podcastId)
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

    public static List<PodcastItem> GetEpisodesWithDownloads(final Context ctx, final int podcastId) {
        return GetEpisodesWithDownloads(ctx, podcastId, -1);
    }

    public static List<PodcastItem> GetEpisodesWithDownloads(final Context ctx, final int podcastId, final int count) {
        final List<PodcastItem> podcasts = new ArrayList<>();

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();
        Cursor cursor;

        if (count == -1)
            cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [download] = 1 AND [pid] = ? ORDER BY [pubDate] DESC"), new String[]{String.valueOf(podcastId)});
        else
            cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [download] = 1 AND [pid] = ? ORDER BY [pubDate] DESC LIMIT -1 OFFSET ".concat(String.valueOf(count))), new String[]{String.valueOf(podcastId)});

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

    public static List<PodcastItem> GetLocalFiles(final Context ctx) {
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

    public static Boolean episodeExists(final Context ctx, final String url) {

        if (url == null) return false;

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

    public static List<PodcastItem> GetEpisodes(final Context ctx, final int podcastId) {
        return GetEpisodes(ctx, podcastId, ctx.getResources().getInteger(R.integer.playlist_default));
    }

    public static List<PodcastItem> GetEpisodes(final Context ctx, final int podcastId, final String orderBy) {
        return GetEpisodes(ctx, podcastId, ctx.getResources().getInteger(R.integer.playlist_default), false, 200, orderBy);
    }

    public static List<PodcastItem> GetEpisodesFiltered(final Context ctx, final int podcastId) {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        final Boolean hidePlayed = prefs.getBoolean("pref_" + podcastId + "_hide_played", false);

        final int numberOfEpisode = Integer.valueOf(prefs.getString("pref_episode_limit", ctx.getString(R.string.episode_list_default)));

        return GetEpisodes(ctx, podcastId, ctx.getResources().getInteger(R.integer.playlist_default), hidePlayed, numberOfEpisode, null);
    }


    public static Boolean HasDownloadedFiles(final Context ctx, final int podcastId) {
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        Boolean output;

        final Cursor cursor = sdb.rawQuery("SELECT id FROM [tbl_podcast_episodes] WHERE [pid] = ? AND [download] = 1 AND [downloadid] = 0", new String[]{String.valueOf(podcastId)});
        output = cursor.moveToFirst();

        cursor.close();
        sdb.close();
        db.close();

        return output;
    }

    public static Boolean HasEpisodes(final Context ctx, final int podcastId, final int playlistId) {
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        Boolean output;

        final boolean showOnlyDownloads = PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("pref_display_show_downloaded_episodes", false);
        String downloadsOnlySelect = "";

        if (showOnlyDownloads)
            downloadsOnlySelect = " AND tbl_podcast_episodes.download = 1";

        Cursor cursor;

        if (playlistId == ctx.getResources().getInteger(R.integer.playlist_downloads))
            cursor = sdb.rawQuery("SELECT id FROM [tbl_podcast_episodes] WHERE [download] = 1 AND [downloadid] = 0", null);
        else if (playlistId == ctx.getResources().getInteger(R.integer.playlist_inprogress))
            cursor = sdb.rawQuery("SELECT tbl_podcast_episodes.id FROM tbl_podcast_episodes WHERE tbl_podcast_episodes.position > 0".concat(downloadsOnlySelect), null);
        //else if (playlistId == ctx.getResources().getInteger(R.integer.playlist_radio))
            //cursor = sdb.rawQuery("SELECT id FROM [tbl_podcast_episodes] WHERE [radio] > 0", null);
        else if (playlistId != 0)
            if (showOnlyDownloads)
                cursor = sdb.rawQuery("SELECT tbl_podcast_episodes.id FROM tbl_playlists_xref INNER JOIN tbl_podcast_episodes ON tbl_podcast_episodes.id = tbl_playlists_xref.episode_id WHERE tbl_playlists_xref.playlist_id = ?".concat(downloadsOnlySelect), new String[]{String.valueOf(playlistId)});
            else
               cursor = sdb.rawQuery("SELECT id FROM [tbl_playlists_xref] WHERE [playlist_id] = ?", new String[]{String.valueOf(playlistId)});
        else
            cursor = sdb.rawQuery("SELECT tbl_podcast_episodes.id FROM tbl_podcast_episodes WHERE tbl_podcast_episodes.pid = ?".concat(downloadsOnlySelect), new String[]{String.valueOf(podcastId)});

        output = cursor.moveToFirst();

        cursor.close();
        sdb.close();
        db.close();

        return output;
    }

    public static List<PodcastItem> GetEpisodes(final Context ctx, final int podcastId, final int playlistId) {
            return GetEpisodes(ctx, podcastId, playlistId, false, Integer.MAX_VALUE, null);
    }

    public static List<PodcastItem> GetEpisodes(final Context ctx, final int podcastId, final int playlistId, final boolean hidePlayed, final int limit, final String orderBy) {
            return GetEpisodes(ctx, podcastId, playlistId, hidePlayed, limit, null, orderBy);
    }

    public static List<PodcastItem> SearchEpisodes(final Context ctx, final int podcastId, final String query) {
            return GetEpisodes(ctx, podcastId, ctx.getResources().getInteger(R.integer.playlist_default), false, Integer.MAX_VALUE, query, null);
    }

    public static List<PodcastItem> GetEpisodes(final Context ctx, final int podcastId, final int playlistId, final boolean hidePlayed, final int limit, String query, final String orderBy) {
        final Resources resources = ctx.getResources();

        List<PodcastItem> episodes = new ArrayList<>();

        final PodcastItem titleItem = new PodcastItem();
        titleItem.setTitle("");
        titleItem.setIsTitle(true);
        ChannelItem channelItem = new ChannelItem();

        if (playlistId == resources.getInteger(R.integer.playlist_downloads))
            channelItem.setTitle(ctx.getString(R.string.playlist_title_downloads));
        else if (playlistId == resources.getInteger(R.integer.playlist_playerfm)) //third party: title
            channelItem.setTitle(ctx.getString(R.string.third_party_title_playerfm));
        else if (playlistId == resources.getInteger(R.integer.playlist_inprogress))
            channelItem.setTitle(ctx.getString(R.string.playlist_title_inprogress));
        //else if (playlistId == resources.getInteger(R.integer.playlist_radio))
            //channelItem.setTitle(ctx.getString(R.string.playlist_title_radio));
        else if (playlistId == resources.getInteger(R.integer.playlist_upnext))
            channelItem.setTitle(ctx.getString(R.string.playlist_title_upnext));
        else if (playlistId == resources.getInteger(R.integer.playlist_unplayed))
            channelItem.setTitle(ctx.getString(R.string.playlist_title_unplayed));
        else if (playlistId > ctx.getResources().getInteger(R.integer.playlist_default) && playlistExists(ctx, playlistId))
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

        final boolean showOnlyDownloads = prefs.getBoolean("pref_display_show_downloaded_episodes", false);

        String downloadsOnlySelect = "";

        if (showOnlyDownloads)
            downloadsOnlySelect = " AND tbl_podcast_episodes.download = 1";

        final int truncateWords = ctx.getResources().getInteger(R.integer.episode_truncate_words);
        final int playlistDefault = ctx.getResources().getInteger(R.integer.playlist_default);

        if (playlistId == resources.getInteger(R.integer.playlist_default)) //regular episodes
            {
                if (hidePlayed)
                    cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [finished] = 0 ".concat(downloadsOnlySelect).concat(" AND [pid] = ? ORDER BY ").concat(orderString).concat(" LIMIT ".concat(String.valueOf(limit)))), new String[]{String.valueOf(podcastId)});
                else if (query != null) {
                    query = "%".concat(query.toLowerCase()).concat("%");
                    cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [pid] = ?".concat(downloadsOnlySelect).concat(" AND ([title] LIKE ? OR [description] LIKE ?) ORDER BY [pubDate]")), new String[]{String.valueOf(podcastId), query, query});
                }
                else
                    cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [pid] = ?".concat(downloadsOnlySelect).concat(" ORDER BY ").concat(orderString).concat(" LIMIT ".concat(String.valueOf(limit)))), new String[]{String.valueOf(podcastId)});
            }
            else if (playlistId == resources.getInteger(R.integer.playlist_downloads)) //downloads can also be in progress, so need separate query for downloads
                cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [download] = 1 AND [downloadid] = 0 ORDER BY ".concat(orderString)), null);
            else if (playlistId == resources.getInteger(R.integer.playlist_unplayed))
                cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [finished] = 0 ORDER BY ".concat(orderString)), null);
            else if (playlistId == resources.getInteger(R.integer.playlist_inprogress))
                cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [position] > 0".concat(downloadsOnlySelect).concat(" ORDER BY ").concat(orderString)), null);
            else if (playlistId > resources.getInteger(R.integer.playlist_default) || playlistId <= resources.getInteger(R.integer.playlist_playerfm))
                cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(",tbl_playlists_xref.playlist_id FROM tbl_podcast_episodes INNER JOIN tbl_playlists_xref ON tbl_playlists_xref.episode_id = tbl_podcast_episodes.id WHERE tbl_playlists_xref.playlist_id = ?".concat(downloadsOnlySelect).concat(" ORDER BY ").concat(orderString)), new String[]{String.valueOf(playlistId)});
            else
                cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [upnext] = 1 ORDER BY ".concat(orderString)), null);

            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    final PodcastItem episode = SetPodcastEpisode(cursor);

                    if (playlistId == resources.getInteger(R.integer.playlist_playerfm))//third party
                        episode.setPlaylistId(cursor.getInt(12));

                    if (playlistId != playlistDefault)
                        episode.setTitle(CommonUtils.truncateWords(episode.getTitle(), truncateWords));

                    episode.setDisplayDate(GetDisplayDate(ctx, cursor.getString(6)));

                    //if (playlistId == resources.getInteger(R.integer.playlist_inprogress) && episode.getPosition() > 0) //in progress
                    //episode.setTitle(episode.getTitle().concat(" (").concat(DateUtils.FormatPositionTime(episode.getPosition())).concat(")"));

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

    public static List<PodcastItem> GetEpisodesAfter(final Context ctx, final PodcastItem episode) {
        List<PodcastItem> episodes = new ArrayList<>();

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        int order;

        final int podcastOrder = Integer.valueOf(prefs.getString("pref_" + episode.getPodcastId() + "_sort_order", String.valueOf(ctx.getResources().getInteger(R.integer.default_episodes_sort_order))));
        if (podcastOrder != 0)
            order = podcastOrder;
        else
            order = Integer.valueOf(prefs.getString("pref_display_episodes_sort_order", String.valueOf(ctx.getResources().getInteger(R.integer.default_episodes_global_sort_order)))); //global sort order

        final String orderString = Utilities.GetOrderClause(order);

        final Cursor cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [pid] = ?".concat(" ORDER BY ").concat(orderString)), new String[]{String.valueOf(episode.getPodcastId())});

        int rowNumber = 0, count = 0;
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                final PodcastItem e = SetPodcastEpisode(cursor);
                if (e.getEpisodeId() == episode.getEpisodeId())
                    rowNumber = count;
                else
                    count++;

                episodes.add(SetPodcastEpisode(cursor));
                cursor.moveToNext();
            }
        }

        cursor.close();
        db.close();
        sdb.close();

        episodes = episodes.subList(rowNumber, episodes.size());

        return episodes;
    }


    static PodcastItem SetPodcastEpisode(final Cursor cursor)
    {
        final PodcastItem episode = new PodcastItem();
        final DBUtilities.ColumnIndexCache cache = new DBUtilities.ColumnIndexCache();

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
        //episode.setIsRadio(cursor.getInt(cache.getColumnIndex(cursor, "radio")) == 1);

        return episode;
    }
}
