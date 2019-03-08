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

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodes;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetLocalFiles;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.GetRoundedLogo;
import static com.krisdb.wearcastslibrary.DateUtils.GetDisplayDate;

public class PlaylistsUtilities {

    public static Boolean playlistIsEmpty(final Context ctx, final int playlistId)
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

    public static Boolean assignedToPlaylist(final Context ctx, final int episodeId)
    {
        Boolean output;
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT [id] FROM tbl_playlists_xref WHERE [episode_id] = ?", new String[]{String.valueOf(episodeId)});

        output = cursor.moveToFirst();

        cursor.close();
        db.close();
        sdb.close();

        return output;
    }

    public static List<PodcastItem> getPlaylistItems(final Context ctx, final int playlistId, final Boolean isLocal) {
        return (isLocal) ? GetEpisodes(ctx, 0, playlistId) : GetLocalFiles(ctx);
    }

    public static List<PlaylistItem> getPlaylists(final Context ctx) {
        return getPlaylists(ctx, false);    }

    public static List<PlaylistItem> getPlaylists(final Context ctx, final Boolean hideEmpty)
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


    public static String getPlaylistName(final Context ctx, final int playlistId) {
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

}
