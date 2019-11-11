package com.krisdb.wearcasts.Async;

import android.content.Context;

import com.krisdb.wearcasts.Utilities.PlaylistsUtilities;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;
import java.util.concurrent.Callable;

public class DisplayPlaylistEpisodes implements Callable<List<PodcastItem>> {
    private final Context context;
    private int mPlayListId;

    public DisplayPlaylistEpisodes(final Context context, final int playlistId) {
        this.context = context;
        this.mPlayListId = playlistId;
    }

    @Override
    public List<PodcastItem> call() {

        return PlaylistsUtilities.GetEpisodes(context, mPlayListId);
    }
}