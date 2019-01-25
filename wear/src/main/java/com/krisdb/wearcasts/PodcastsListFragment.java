package com.krisdb.wearcasts;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class PodcastsListFragment extends Fragment {

    private WearableRecyclerView mPodcastsList = null;
    private int mPlaylistId, mVisits;
    private Activity mActivity;
    private TextView mEmptyView;
    private PodcastsAdapter mAdapter;
    private WeakReference<Activity> mActivityRef;

    public static PodcastsListFragment newInstance() {
        return new PodcastsListFragment();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mActivity = getActivity();
        mActivityRef = new WeakReference<>(mActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        final View listView = inflater.inflate(R.layout.fragment_podcast_list, container, false);

        mPodcastsList = listView.findViewById(R.id.podcast_list);
        mPodcastsList.setEdgeItemsCenteringEnabled(true);
        /*
        mPodcastsList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    final Intent intentPlaylist = new Intent();
                    intentPlaylist.setAction("fragment");
                    intentPlaylist.putExtra("hide_paging_indicator", true);
                    LocalBroadcastManager.getInstance(mActivity).sendBroadcast(intentPlaylist);
                }
            }
        });
        */

        final WearableLinearLayoutManager layoutManager = new WearableLinearLayoutManager(mActivity);
        mPodcastsList.setLayoutManager(layoutManager);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        mVisits = prefs.getInt("visits", 0);

        String emptyText = mActivity.getString(R.string.empty_podcast_list);

        if (mVisits < 10) {
            emptyText = emptyText.concat("\n\n").concat(mActivity.getString(R.string.empty_podcast_list2));
            listView.findViewById(R.id.podcast_list_swipe_left).setVisibility(View.VISIBLE);
        }

        mEmptyView = listView.findViewById(R.id.empty_podcast_list);
        mEmptyView.setText(emptyText);

        mPlaylistId = (getArguments() != null) ? getArguments().getInt("playlistId") : mActivity.getResources().getInteger(R.integer.playlist_default);

        if (prefs.getBoolean("syncOnStart", false))
            handleNetwork();

        RefreshContent();
       return listView;
    }

    private void handleNetwork()
    {
        if (CommonUtils.getActiveNetwork(mActivity) == null)
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                alert.setMessage(getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"), 1);
                        dialog.dismiss();
                    }
                });

                alert.setNegativeButton(getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        }
        else if (CommonUtils.HighBandwidthNetwork(mActivity) == false)
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                alert.setMessage(getString(R.string.alert_episode_network_no_high_bandwidth));
                alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"), 1);
                        dialog.dismiss();
                    }
                });

                alert.setNegativeButton(getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        }
        else
        {
            new AsyncTasks.SyncPodcasts(mActivity, 0, false,
                    new Interfaces.BackgroundSyncResponse() {
                        @Override
                        public void processFinish(final int count, final int downloads) {
                            RefreshContent();
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                new AsyncTasks.SyncPodcasts(mActivity, 0, false,
                        new Interfaces.BackgroundSyncResponse() {
                            @Override
                            public void processFinish(final int count, final int downloads) {
                                RefreshContent();
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }


    @Override
    public void onActivityCreated(final Bundle icicle) {

        super.onActivityCreated(icicle);
    }

    private void RefreshContent() {
        if (isAdded() == false) return;

        new AsyncTasks.DisplayPodcasts(mActivity,
                new Interfaces.PodcastsResponse() {
                    @Override
                    public void processFinish(final List<PodcastItem> podcasts) {
                        mAdapter = new PodcastsAdapter(mActivity, podcasts, mPlaylistId);
                        mPodcastsList.setAdapter(mAdapter);
                        showCopy(podcasts.size());
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


        //final ItemTouchHelper itemTouchhelper = new ItemTouchHelper(new PodcastsSwipeController(mActivity, mAdapter, podcasts));
        //itemTouchhelper.attachToRecyclerView(mPodcastsList);

    }

    private void showCopy(final int number)
    {
        if (number > 0)
        {
            if (mEmptyView != null)
                mEmptyView.setVisibility(TextView.GONE);

            if (mActivity.findViewById(R.id.podcast_list_swipe_left) != null)
                mActivity.findViewById(R.id.podcast_list_swipe_left).setVisibility(View.GONE);
        }
        else {
            if (mEmptyView != null)
                mEmptyView.setVisibility(TextView.VISIBLE);

            if (mActivity.findViewById(R.id.podcast_list_swipe_left) != null)
                mActivity.findViewById(R.id.podcast_list_swipe_left).setVisibility(mVisits < 10 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null)
            showCopy(mAdapter.refreshContent());
    }
}
