package com.krisdb.wearcasts.Controllers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;

import com.krisdb.wearcasts.Adapters.EpisodesAdapter;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.PlaylistsUtilities;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodes;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.SaveEpisodeValue;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.playlistIsEmpty;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class EpisodesSwipeController extends Callback {

    private List<PodcastItem> mEpisodes;
    private Context mContext;
    private EpisodesAdapter mAdapter;

    public EpisodesSwipeController(final Context ctx, final EpisodesAdapter adapter, final List<PodcastItem> episodes)
    {
        mContext = ctx;
        mAdapter = adapter;
        mEpisodes = episodes;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, ItemTouchHelper.LEFT);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        final int position = viewHolder.getAdapterPosition();
        final PodcastItem episode = mEpisodes.get(position);

        if (episode.getIsTitle())
        {
            if (episode.getChannel().getThumbnailUrl() != null) {
                CommonUtils.showToast(mContext, mContext.getString(R.string.alert_refreshing_thumb));
                new AsyncTasks.SaveLogo(mContext, episode.getChannel().getThumbnailUrl().toString(), episode.getChannel().getThumbnailName(), true,
                        new Interfaces.AsyncResponse() {
                            @Override
                            public void processFinish() {
                                mAdapter.refreshItem(0);
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            mAdapter.refreshItem(0);
        }
        else {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            final int swipeActionId = Integer.valueOf(prefs.getString("pref_episodes_swipe_action", "0"));

            if (swipeActionId == 0) {
                SaveEpisodeValue(mContext, episode, "finished", episode.getFinished() ? 0 : 1);

                final boolean hidePlayed = prefs.getBoolean("pref_" + episode.getPodcastId() + "_hide_played", false);
                final int numberOfEpisode = Integer.valueOf(prefs.getString("pref_episode_limit", mContext.getString(R.string.episode_list_default)));

                mEpisodes = GetEpisodes(mContext, episode.getPodcastId(), hidePlayed, numberOfEpisode, null);

                if (hidePlayed)
                    mAdapter.refreshList(mEpisodes);
                else
                    mAdapter.refreshItem(mEpisodes, position);
            }
            else if (swipeActionId == -1)
            {
                Utilities.startDownload(mContext, episode);
                showToast(mContext, mContext.getString(R.string.alert_episode_download_start));
                mAdapter.refreshItem(mEpisodes, position);
            }
            else
            {
                if (prefs.getBoolean("pref_hide_empty_playlists", false) && playlistIsEmpty(mContext, swipeActionId)) {
                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("refresh_vp", true);
                    editor.apply();
                }

                final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mContext);
                db.addEpisodeToPlaylist(swipeActionId, episode.getEpisodeId());
                db.close();

                showToast(mContext, mContext.getString(R.string.alert_episode_playlist_added, PlaylistsUtilities.getPlaylistName(mContext, swipeActionId)));

                mAdapter.refreshItem(mEpisodes, position);
            }
        }
    }
}
