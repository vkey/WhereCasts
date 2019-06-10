package com.krisdb.wearcasts.Controllers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.krisdb.wearcasts.Adapters.EpisodesAdapter;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.PlaylistsUtilities;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.SaveEpisodeValue;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.playlistIsEmpty;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class EpisodesSwipeController extends ItemTouchHelper.Callback {

    private List<PodcastItem> mEpisodes;
    private static WeakReference<Activity> mActivityRef;
    private EpisodesAdapter mAdapter;
    private ConnectivityManager mManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private TimeOutHandler mTimeOutHandler;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(7);
    private static int mNoNetworkPosition;
    private String mQuery;

    public EpisodesSwipeController(final Activity ctx, final EpisodesAdapter adapter, final String query, final List<PodcastItem> episodes)
    {
        mActivityRef = new WeakReference<>(ctx);
        mAdapter = adapter;
        mEpisodes = episodes;
        mQuery = query;
        mManager = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTimeOutHandler = new TimeOutHandler(this);
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, ItemTouchHelper.LEFT);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        final int position = viewHolder.getAdapterPosition();
        mNoNetworkPosition = position;
        final PodcastItem episode = mEpisodes.get(position);
        final Context ctx = mActivityRef.get();

        if (episode.getIsTitle())
        {
            if (episode.getChannel().getThumbnailUrl() != null) {
                CommonUtils.showToast(ctx, ctx.getString(R.string.alert_refreshing_thumb));
                new AsyncTasks.SaveLogo(ctx, episode.getChannel().getThumbnailUrl().toString(), episode.getChannel().getThumbnailName(), true,
                        new Interfaces.AsyncResponse() {
                            @Override
                            public void processFinish() {
                                mAdapter.refreshItem2(mEpisodes,0);
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            mAdapter.refreshItem2(mEpisodes,0);
        }
        else {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

            final int swipeActionId = Integer.valueOf(Objects.requireNonNull(prefs.getString("pref_episodes_swipe_action", "0")));

            //Toggle play/unplayed
            if (swipeActionId == 0) {
                SaveEpisodeValue(ctx, episode, "finished", episode.getFinished() ? 0 : 1);

                final int podcastId = mEpisodes.get(position).getPodcastId();

                final boolean hidePlayed = prefs.getBoolean("pref_" + podcastId + "_hide_played", false);

                if (hidePlayed) {
                    new com.krisdb.wearcasts.AsyncTasks.DisplayEpisodes(ctx, podcastId, mQuery,
                            new Interfaces.PodcastsResponse() {
                                @Override
                                public void processFinish(final List<PodcastItem> episodes) {
                                    mEpisodes = episodes;
                                    mAdapter.refreshList(episodes);
                                }
                            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                else {
                    mEpisodes.get(position).setFinished(!episode.getFinished());
                    mAdapter.refreshItem2(mEpisodes, position);
                }
            }
            else if (swipeActionId == -1) //Download
            {
                if (CommonUtils.getActiveNetwork(ctx) == null)
                {
                    if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                        final AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
                        alert.setMessage(ctx.getString(R.string.alert_episode_network_notfound));
                        alert.setPositiveButton(ctx.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final SharedPreferences.Editor editor = prefs.edit();
                                editor.putInt("no_network_position", position);
                                editor.apply();

                                mActivityRef.get().startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), 102);
                                dialog.dismiss();
                            }
                        });

                        alert.setNegativeButton(ctx.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
                    }
                }
                else if (prefs.getBoolean("initialDownload", true) && Utilities.BluetoothEnabled()) {
                    if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                        final AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
                        alert.setMessage(ctx.getString(R.string.confirm_initial_download_message));
                        alert.setPositiveButton(ctx.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("pref_disable_bluetooth", true);
                                editor.apply();
                                downloadEpisode(position);
                                dialog.dismiss();
                            }
                        });
                        alert.setNegativeButton(ctx.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                downloadEpisode(position);
                                dialog.dismiss();
                            }
                        }).show();

                        final SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("initialDownload", false);
                        editor.apply();
                    }
                }
                else if (prefs.getBoolean("pref_disable_bluetooth", false) && Utilities.BluetoothEnabled() && Utilities.disableBluetooth(ctx)) {
                    unregisterNetworkCallback();

                    CommonUtils.showToast(ctx, ctx.getString(R.string.alert_episode_network_waiting));
                    mNoNetworkPosition = position;
                    mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(final Network network) {
                            mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);
                            downloadEpisode(position);
                        }
                    };

                    final NetworkRequest request = new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .build();

                    mManager.requestNetwork(request, mNetworkCallback);

                    mTimeOutHandler.sendMessageDelayed(
                            mTimeOutHandler.obtainMessage(MESSAGE_CONNECTIVITY_TIMEOUT),
                            NETWORK_CONNECTIVITY_TIMEOUT_MS);
                }
                else
                    downloadEpisode(position);
            }
            else //Add to playlist
            {
                if (prefs.getBoolean("pref_hide_empty_playlists", false) && playlistIsEmpty(ctx, swipeActionId)) {
                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("refresh_vp", true);
                    editor.apply();
                }

                final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
                db.addEpisodeToPlaylist(swipeActionId, episode.getEpisodeId());
                db.close();

                showToast(ctx, ctx.getString(R.string.alert_episode_playlist_added, PlaylistsUtilities.getPlaylistName(ctx, swipeActionId)));

                mAdapter.refreshItem2(mEpisodes, position);
            }
        }
    }

    private static class TimeOutHandler extends Handler {
        private final WeakReference<EpisodesSwipeController> mActivityWeakReference;

        TimeOutHandler(final EpisodesSwipeController controller) {
            mActivityWeakReference = new WeakReference<>(controller);
        }

        @Override
        public void handleMessage(final Message msg) {
            final EpisodesSwipeController controller = mActivityWeakReference.get();

            if (controller != null) {
                switch (msg.what) {
                    case MESSAGE_CONNECTIVITY_TIMEOUT:
                        final Activity ctx = mActivityRef.get();

                        if (ctx != null && !ctx.isFinishing()) {
                            final AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
                            alert.setMessage(ctx.getString(R.string.alert_episode_network_notfound));
                            alert.setPositiveButton(ctx.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
                                    editor.putInt("no_network_position", mNoNetworkPosition);
                                    editor.apply();

                                    ctx.startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), 102);
                                    dialog.dismiss();
                                }
                            });

                            alert.setNegativeButton(ctx.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Utilities.enableBluetooth(ctx);
                                    dialog.dismiss();
                                }
                            }).show();
                        }
                        controller.unregisterNetworkCallback();
                        break;
                }
            }
        }
    }

    private void unregisterNetworkCallback() {
        if (mNetworkCallback != null) {
            mManager.unregisterNetworkCallback(mNetworkCallback);
            mManager.bindProcessToNetwork(null);
            mNetworkCallback = null;
        }
    }

    private void downloadEpisode(final int position) {
        final Context ctx = mActivityRef.get();

        long downloadId = Utilities.startDownload(ctx, mEpisodes.get(position));

        mEpisodes.get(position).setDownloadId((int)downloadId);
        mAdapter.refreshItem2(mEpisodes, position);
    }
}
