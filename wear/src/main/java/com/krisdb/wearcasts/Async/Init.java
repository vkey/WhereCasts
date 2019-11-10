package com.krisdb.wearcasts.Async;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import androidx.core.content.ContextCompat;

import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.DBUtilities;
import com.krisdb.wearcasts.Utilities.Utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.HasEpisodes;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;

public class Init implements Callable<List<Integer>> {
    private final Context context;

    public Init(Context context) {
        this.context = context;
    }

    @Override
    public List<Integer> call() {

        final Context ctx = context;
        List<Integer> playlistIds = new ArrayList<>();
        final Resources resources = ctx.getResources();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        final boolean hideEmpty = prefs.getBoolean("pref_hide_empty_playlists", false);
        final boolean localFiles = (ContextCompat.checkSelfPermission(ctx, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && DBUtilities.GetLocalFiles(ctx).size() > 1);
        final boolean hasPremium = Utilities.hasPremium(ctx);

        playlistIds.add(-1);

        //third party: add check for playlist
        if (HasEpisodes(ctx, 0, resources.getInteger(R.integer.playlist_playerfm)))
            playlistIds.add(resources.getInteger(R.integer.playlist_playerfm));

        if (hasPremium) {

            final List<PlaylistItem> playlists = getPlaylists(ctx, hideEmpty);

            if (prefs.getBoolean("pref_display_show_downloaded_episodes", false)) {
                for (final PlaylistItem playlist : playlists)
                    if (HasEpisodes(ctx, 0, playlist.getID()))
                        playlistIds.add(playlist.getID());
            } else {
                for (final PlaylistItem playlist : playlists)
                    playlistIds.add(playlist.getID());
            }
        }
        if (localFiles)
            playlistIds.add(resources.getInteger(R.integer.playlist_local));

        if (!hideEmpty || HasEpisodes(ctx, 0, resources.getInteger(R.integer.playlist_inprogress)))
            playlistIds.add(resources.getInteger(R.integer.playlist_inprogress));

        if (!hideEmpty || HasEpisodes(ctx, 0, resources.getInteger(R.integer.playlist_downloads)))
            playlistIds.add(resources.getInteger(R.integer.playlist_downloads));

        return playlistIds;
    }
}