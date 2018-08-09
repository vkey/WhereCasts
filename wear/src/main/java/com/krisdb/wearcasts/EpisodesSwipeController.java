package com.krisdb.wearcasts;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;

import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.CENTER;
import static android.view.Gravity.LEFT;
import static android.view.Gravity.RIGHT;
import static android.view.Gravity.TOP;

public class EpisodesSwipeController extends Callback {

    private List<PodcastItem> mEpisodes;
    private Context mContext;
    private EpisodesAdapter mAdapter;
    private int mPlaylistID;

    EpisodesSwipeController(final Context ctx, final EpisodesAdapter adapter, final List<PodcastItem> episodes, final int playlistId)
    {
        mContext = ctx;
        mAdapter = adapter;
        mEpisodes = episodes;
        mPlaylistID = playlistId;
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

        if (mPlaylistID == mContext.getResources().getInteger(R.integer.playlist_default)) {
            DBUtilities.SaveEpisodeValue(mContext, episode, "finished", episode.getFinished() ? 0 : 1);

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            final Boolean hidePlayed = prefs.getBoolean("pref_" + episode.getPodcastId() + "_hide_played", false);
            final int numberOfEpisode = Integer.valueOf(prefs.getString("pref_episode_limit", mContext.getString(R.string.episode_list_default)));

            mEpisodes = DBUtilities.GetEpisodes(mContext, episode.getPodcastId(), mPlaylistID, hidePlayed, numberOfEpisode, null);
            mAdapter.refreshList(mEpisodes);
        }
    }
}
