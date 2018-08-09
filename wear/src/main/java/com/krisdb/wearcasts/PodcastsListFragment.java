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
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class PodcastsListFragment extends Fragment {

    private WearableRecyclerView mPodcastsList = null;
    private int mPlaylistId, mVisits;
    private Activity mActivity;
    private TextView mEmptyView;
    private ConnectivityManager mConnectivityManager;
    private Handler mNetworkHandler = new Handler();
    private PodcastsAdapter mAdapter;

    public static PodcastsListFragment newInstance() {
        return new PodcastsListFragment();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mActivity = getActivity();
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
        if (PreferenceManager.getDefaultSharedPreferences(mActivity).getBoolean("pref_high_bandwidth", true) == false)
        {
            new AsyncTasks.SyncPodcasts(mActivity, 0, false,
                    new Interfaces.BackgroundSyncResponse() {
                        @Override
                        public void processFinish(final int count, final int downloads) {
                            RefreshContent();
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return;
        }

        mNetworkHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        mConnectivityManager.bindProcessToNetwork(null);
                        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
                        mNetworkHandler.removeMessages(1);

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

                        break;
                }
            }
        };

        mConnectivityManager = (ConnectivityManager)mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        final Network activeNetwork = mConnectivityManager.getActiveNetwork();

        if (activeNetwork != null) {
            int bandwidth = mConnectivityManager.getNetworkCapabilities(activeNetwork).getLinkDownstreamBandwidthKbps();

                if (bandwidth < getResources().getInteger(R.integer.minimum_bandwidth))
            {
                final NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();

                mConnectivityManager.requestNetwork(request, mNetworkCallback);
                showToast(mActivity.getApplicationContext(), getString(R.string.alert_episode_network_search));
                mNetworkHandler.sendMessageDelayed( mNetworkHandler.obtainMessage(1), 10000);
            }
            else {
                new AsyncTasks.SyncPodcasts(mActivity, 0, false,
                        new Interfaces.BackgroundSyncResponse() {
                            @Override
                            public void processFinish(final int count, final int downloads) {
                                RefreshContent();
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
        else
        {
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

    final ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            new AsyncTasks.SyncPodcasts(mActivity, 0, false,
                    new Interfaces.BackgroundSyncResponse() {
                        @Override
                        public void processFinish(final int count, final int downloads) {
                            RefreshContent();
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            mNetworkHandler.removeMessages(1);
        }
    };

    @Override
    public void onStop() {
        super.onStop();
        if (mConnectivityManager != null) {
            mConnectivityManager.bindProcessToNetwork(null);
            try {mConnectivityManager.unregisterNetworkCallback(mNetworkCallback); }
            catch(Exception ignored){}
        }
    }

    @Override
    public void onActivityCreated(final Bundle icicle) {

        super.onActivityCreated(icicle);
    }

    private void RefreshContent() {
        if (isAdded() == false) return;

        final List<PodcastItem> podcasts = DBUtilities.GetPodcasts(mActivity);
        mAdapter = new PodcastsAdapter(mActivity, podcasts, mPlaylistId);
        mPodcastsList.setAdapter(mAdapter);

        //final ItemTouchHelper itemTouchhelper = new ItemTouchHelper(new PodcastsSwipeController(mActivity, mAdapter, podcasts));
        //itemTouchhelper.attachToRecyclerView(mPodcastsList);

        showCopy(podcasts.size());
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
