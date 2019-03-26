package com.krisdb.wearcasts.Fragments;

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
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.krisdb.wearcasts.Adapters.PodcastsAdapter;
import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Constants;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.fragment.app.Fragment;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.krisdb.wearcastslibrary.CommonUtils.GetRoundedLogo;

public class PodcastsListFragment extends Fragment {

    private WearableRecyclerView mPodcastsList = null;
    private Activity mActivity;
    private TextView mEmptyView;
    private PodcastsAdapter mAdapter;
    private static WeakReference<Activity> mActivityRef;
    private ImageView mLogo;
    /*
    private ConnectivityManager mManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private TimeOutHandler mTimeOutHandler;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(7);
    */
    public static PodcastsListFragment newInstance() {
        return new PodcastsListFragment();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mActivity = getActivity();
        mActivityRef = new WeakReference<>(mActivity);
        //mTimeOutHandler = new TimeOutHandler(this);
        //mManager = (ConnectivityManager)mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        final View listView = inflater.inflate(R.layout.fragment_podcast_list, container, false);

        mPodcastsList = listView.findViewById(R.id.podcast_list);
        mLogo = listView.findViewById(R.id.podcast_list_empty_logo);

        mPodcastsList.setEdgeItemsCenteringEnabled(true);
        mPodcastsList.setLayoutManager(new WearableLinearLayoutManager(mActivity));
        //mPodcastsList.setLayoutManager(new WearableLinearLayoutManager(mActivity, new ScrollingLayoutPodcasts()));

        if (PreferenceManager.getDefaultSharedPreferences(mActivity).getBoolean("syncOnStart", false))
            handleNetwork();

        RefreshContent();

        return listView;
    }

    private void handleNetwork()
    {
        //final SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(mActivity);

        if (CommonUtils.getActiveNetwork(mActivity) == null)
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                alert.setMessage(getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), 1);
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
                        startActivityForResult(new Intent(Constants.WifiIntent),1);
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
        /*
        else if (prefs.getBoolean("pref_high_bandwidth", true) && !CommonUtils.HighBandwidthNetwork(mActivity))
        {
            unregisterNetworkCallback();
            //CommonUtils.showToast(mActivity, mActivity.getString(R.string.alert_episode_network_search));

            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(final Network network) {
                    mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);

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
        */
        else
        {
            new AsyncTasks.SyncPodcasts(mActivity, 0, false,
                    new Interfaces.BackgroundSyncResponse() {
                        @Override
                        public void processFinish(final int newEpisodeCount, final int downloads, final List<PodcastItem> downloadEpisodes) {
                            RefreshContent();
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /*
    private static class TimeOutHandler extends Handler {
        private final WeakReference<PodcastsListFragment> mActivityWeakReference;
        private final PodcastsListFragment mFragment;

        TimeOutHandler(final PodcastsListFragment fragment) {
            mActivityWeakReference = new WeakReference<>(fragment);
            mFragment = fragment;
        }

        @Override
        public void handleMessage(final Message msg) {
            final PodcastsListFragment fragment = mActivityWeakReference.get();

            if (fragment != null) {
                switch (msg.what) {
                    case MESSAGE_CONNECTIVITY_TIMEOUT:
                        final Activity ctx = mActivityRef.get();
                        if (ctx != null && !ctx.isFinishing()) {
                            final AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
                            alert.setMessage(ctx.getString(R.string.alert_episode_network_no_high_bandwidth));
                            alert.setPositiveButton(ctx.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mFragment.startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), 1);
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

                        fragment.unregisterNetworkCallback();
                        break;
                }
            }
        }
    }

    private void releaseHighBandwidthNetwork() {
        mManager.bindProcessToNetwork(null);
        unregisterNetworkCallback();
    }

    private void unregisterNetworkCallback() {
        if (mNetworkCallback != null) {
            mManager.unregisterNetworkCallback(mNetworkCallback);
            mNetworkCallback = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                new AsyncTasks.SyncPodcasts(mActivity, 0, false,
                        new Interfaces.BackgroundSyncResponse() {
                            @Override
                            public void processFinish(final int newEpisodeCount, final int downloads, final List<PodcastItem> downloadEpisodes) {
                                RefreshContent();
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }
*/
    @Override
    public void onActivityCreated(final Bundle icicle) {

        super.onActivityCreated(icicle);
    }

    private void RefreshContent() {
        if (!isAdded()) return;

        final Boolean hideEmpty = PreferenceManager.getDefaultSharedPreferences(mActivity).getBoolean("pref_hide_empty", false);

        new AsyncTasks.DisplayPodcasts(mActivity, hideEmpty,
                new Interfaces.PodcastsResponse() {
                    @Override
                    public void processFinish(final List<PodcastItem> podcasts) {
                        mAdapter = new PodcastsAdapter(mActivity, podcasts);
                        mPodcastsList.setAdapter(mAdapter);
                        showCopy(podcasts.size());
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showCopy(final int number)
    {
        if (!isAdded()) return;

        final ImageView swipeLeftView = mActivity.findViewById(R.id.podcast_list_swipe_left);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        if (number > 0)
        {
            if (mEmptyView != null)
                mEmptyView.setVisibility(TextView.GONE);

            if (swipeLeftView != null)
                swipeLeftView.setVisibility(View.GONE);

            if (mLogo != null)
                mLogo.setVisibility(TextView.GONE);
        }
        else {
            final int visits = prefs.getInt("visits", 0);

            String emptyText = "";
            final String lastUpdateDate = prefs.getString("last_podcast_sync_date", "");
            mEmptyView = mActivity.findViewById(R.id.empty_podcast_list);

            if (visits < 10) {
                emptyText = emptyText.concat(mActivity.getString(R.string.empty_podcast_list)).concat("\n\n").concat(mActivity.getString(R.string.empty_podcast_list2));
                swipeLeftView.setVisibility(View.VISIBLE);
            }
            else if (lastUpdateDate.length() > 0) {
                emptyText = emptyText.concat(getString(R.string.last_updated)
                        .concat(":\n")
                        .concat(DateUtils.GetDisplayDate(mActivity, lastUpdateDate, "EEE MMM dd H:mm:ss Z yyyy"))
                        .concat(" @ ")
                        .concat(DateUtils.GetTime(DateUtils.ConvertDate(lastUpdateDate, "EEE MMM dd H:mm:ss Z yyyy"))));
            }
            else
                emptyText = mActivity.getString(R.string.empty_podcast_list);

            if (mEmptyView != null) {
                mEmptyView.setText(emptyText);
                mEmptyView.setVisibility(TextView.VISIBLE);
            }

            if (swipeLeftView != null)
                swipeLeftView.setVisibility(visits < 10 ? View.VISIBLE : View.GONE);

            if (mLogo != null) {
                mLogo.setImageDrawable(GetRoundedLogo(mActivity, null));
                mLogo.setVisibility(TextView.VISIBLE);
                mLogo.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        new AsyncTasks.DisplayPodcasts(mActivity, false,
                                new Interfaces.PodcastsResponse() {
                                    @Override
                                    public void processFinish(final List<PodcastItem> podcasts) {
                                        mAdapter = new PodcastsAdapter(mActivity, podcasts);
                                        mPodcastsList.setAdapter(mAdapter);
                                        showCopy(podcasts.size());
                                    }
                                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                        return false;
                    }
                });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null)
            showCopy(mAdapter.refreshContent());
    }
}
