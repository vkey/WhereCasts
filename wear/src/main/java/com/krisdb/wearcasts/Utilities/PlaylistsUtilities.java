package com.krisdb.wearcasts.Utilities;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.ChannelItem;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.ArrayList;
import java.util.List;

import static com.krisdb.wearcasts.Utilities.DBUtilities.GetChannel;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetLocalFiles;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.SetPodcastEpisode;
import static com.krisdb.wearcastslibrary.CommonUtils.GetEpisodesThumbnailDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.GetPodcastsThumbnailDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.GetRoundedLogo;
import static com.krisdb.wearcastslibrary.CommonUtils.GetRoundedPlaceholderLogo;
import static com.krisdb.wearcastslibrary.DateUtils.GetDisplayDate;

public class PlaylistsUtilities {
    private static final String mEpisodeColumns = "tbl_podcast_episodes.id,tbl_podcast_episodes.pid,tbl_podcast_episodes.title,tbl_podcast_episodes.description,tbl_podcast_episodes.url,tbl_podcast_episodes.mediaurl,tbl_podcast_episodes.pubDate,tbl_podcast_episodes.read,tbl_podcast_episodes.finished,tbl_podcast_episodes.position,tbl_podcast_episodes.duration,tbl_podcast_episodes.download,tbl_podcast_episodes.dateDownload,tbl_podcast_episodes.downloadid,tbl_podcast_episodes.thumbnail_url";

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

    public static int getPlaylistID(final Context ctx, final int episodeId)
    {
        int playlistId = 0;

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        Cursor cursor = sdb.rawQuery("SELECT [id] FROM tbl_playlists_xref WHERE [episode_id] = ?", new String[]{String.valueOf(episodeId)});

        if (cursor.moveToFirst())
            playlistId = cursor.getInt(0);

        if (playlistId == 0)
        {
            cursor = sdb.rawQuery("SELECT [id] FROM tbl_podcast_episodes WHERE [download] = 1 AND [id] = ?", new String[]{String.valueOf(episodeId)});

            if (cursor.moveToFirst())
                playlistId = ctx.getResources().getInteger(R.integer.playlist_downloads);
        }

        if (playlistId == 0)
        {
            cursor = sdb.rawQuery("SELECT [id] FROM tbl_podcast_episodes WHERE [position] > 0 AND [id] = ?", new String[]{String.valueOf(episodeId)});

            if (cursor.moveToFirst())
                playlistId = ctx.getResources().getInteger(R.integer.playlist_inprogress);
        }

        cursor.close();
        db.close();
        sdb.close();

        return playlistId;
    }

    public static List<PodcastItem> getPlaylistItems(final Context ctx, final int playlistId, final Boolean isLocal) {
        return (isLocal) ? GetEpisodes(ctx, playlistId) : GetLocalFiles(ctx);
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

    public static List<PodcastItem> GetEpisodes(final Context ctx, final int playlistId) {
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
        else if (playlistId == resources.getInteger(R.integer.playlist_upnext))
            channelItem.setTitle(ctx.getString(R.string.playlist_title_upnext));
        else if (playlistId == resources.getInteger(R.integer.playlist_unplayed))
            channelItem.setTitle(ctx.getString(R.string.playlist_title_unplayed));
        else if (playlistId > -1 && playlistExists(ctx, playlistId))
            channelItem.setTitle(getPlaylistName(ctx, playlistId));

        titleItem.setChannel(channelItem);
        episodes.add(titleItem);

        if (playlistId != resources.getInteger(R.integer.playlist_local)) {

            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
            final SQLiteDatabase sdb = db.select();

            Cursor cursor;

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            final int order = Integer.valueOf(prefs.getString("pref_display_playlist_sort_order", String.valueOf(ctx.getResources().getInteger(R.integer.default_playlist_sort_order))));

            String orderString = Utilities.GetOrderClause(order);

            final boolean progressFirst = prefs.getBoolean("pref_playlists_show_progress_first", false);

            if (progressFirst && order != Enums.SortOrder.PROGRESS.getSorderOrderCode())
                orderString = "tbl_podcast_episodes.position DESC,".concat(orderString);

            //final int truncateWords = ctx.getResources().getInteger(R.integer.episode_truncate_words);
            final int truncateLength = ctx.getResources().getInteger(R.integer.episode_truncate_length);

             if (playlistId == resources.getInteger(R.integer.playlist_downloads)) //downloads can also be in progress, so need separate query for downloads
                 cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [download] = 1 AND [downloadid] = 0 ORDER BY ".concat(orderString)), null);
            else if (playlistId == resources.getInteger(R.integer.playlist_unplayed))
                cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [finished] = 0 ORDER BY ".concat(orderString)), null);
            else if (playlistId == resources.getInteger(R.integer.playlist_inprogress))
                cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [position] > 0".concat(" ORDER BY ").concat(orderString)), null);
             else if (playlistId > -1 || playlistId <= resources.getInteger(R.integer.playlist_playerfm))
                 cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(",tbl_playlists_xref.playlist_id FROM tbl_podcast_episodes INNER JOIN tbl_playlists_xref ON tbl_playlists_xref.episode_id = tbl_podcast_episodes.id WHERE tbl_playlists_xref.playlist_id = ?".concat(" ORDER BY ").concat(orderString)), new String[]{String.valueOf(playlistId)});
             else
                 cursor = sdb.rawQuery("SELECT ".concat(mEpisodeColumns).concat(" FROM [tbl_podcast_episodes] WHERE [finished] = 0 ORDER BY ".concat(orderString)), null);

            if (cursor.moveToFirst()) {

                while (!cursor.isAfterLast()) {
                    final PodcastItem episode = SetPodcastEpisode(cursor);

                    if (playlistId == resources.getInteger(R.integer.playlist_playerfm))//third party
                        episode.setPlaylistId(cursor.getInt(12));

                    episode.setTitle(CommonUtils.truncateLength(episode.getTitle(), truncateLength));

                    episode.setDisplayDate(GetDisplayDate(ctx, cursor.getString(6)));

                    episode.setChannel(GetChannel(ctx, cursor.getInt(1)));

                    if (episode.getThumbnailUrl() != null)
                        episode.setDisplayThumbnail(GetRoundedLogo(ctx, GetEpisodesThumbnailDirectory(ctx).concat(CommonUtils.GetThumbnailName(episode.getEpisodeId()))));
                    else if (episode.getChannel().getThumbnailUrl() != null)
                        episode.setDisplayThumbnail(GetRoundedLogo(ctx, GetPodcastsThumbnailDirectory(ctx).concat(CommonUtils.GetThumbnailName(episode.getPodcastId()))));
                    else
                        episode.setDisplayThumbnail(GetRoundedPlaceholderLogo(ctx));

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
