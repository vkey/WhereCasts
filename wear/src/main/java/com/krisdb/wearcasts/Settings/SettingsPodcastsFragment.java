package com.krisdb.wearcasts.Settings;

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
import android.os.Looper;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.app.Activity.RESULT_OK;

public class SettingsPodcastsFragment extends PreferenceFragment {

    private Activity mActivity;
    private static WeakReference<Activity> mActivityRef;
    private Boolean mNoResume = false;
    private ConnectivityManager mManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private static TimeOutHandler mTimeOutHandler;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);
    private static int NO_NETWORK_RESULTS_CODE = 101;
    private static int LOW_BANDWIDTH_RESULTS_CODE = 102;
    private List<PodcastItem> mDownloadEpisodes;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_podcasts);
        mActivityRef = new WeakReference<>(getActivity());

        mActivity = getActivity();

        mTimeOutHandler = new TimeOutHandler(this);
        mManager = (ConnectivityManager)mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        //mTimeOutHandler = new TimeOutHandler(this);
        //mManager = (ConnectivityManager)mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        findPreference("pref_updates").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPodcastsUpdatesActivity.class));
                return false;
            }
        });

        findPreference("pref_podcasts").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPodcastsPodcastsActivity.class));
                return false;
            }
        });

        findPreference("pref_episodes").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPodcastsEpisodesActivity.class));
                return false;
            }
        });

        findPreference("pref_display").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPodcastsDisplayActivity.class));
                return false;
            }
        });

        findPreference("pref_downloads").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPodcastsDownloadsActivity.class));
                return false;
            }
        });

        findPreference("pref_playback").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPodcastsPlaybackActivity.class));
                return false;
            }
        });

        findPreference("pref_playlists").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPlaylistsActivity.class));
                return false;
            }
        });

        findPreference("pref_sync_podcasts").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                handleNetwork();
                return false;
            }
        });
    }

    private void handleNetwork() {
        //final SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(mActivity);

        if (!CommonUtils.isNetworkAvailable(mActivity))
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                alert.setMessage(getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), NO_NETWORK_RESULTS_CODE);
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
        else {
            findPreference("pref_sync_podcasts").setSummary(getString(R.string.syncing));
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            new AsyncTasks.SyncPodcasts(mActivity, 0, true, findPreference("pref_sync_podcasts"),
                    new Interfaces.BackgroundSyncResponse() {
                        @Override
                        public void processFinish(final int newEpisodeCount, final int downloads, final List<PodcastItem> downloadEpisodes) {

                            if (downloadEpisodes.size() > 0)
                                downloadEpisodes(downloadEpisodes);

                            SetContent(newEpisodeCount);
                            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    private void downloadEpisodes(final List<PodcastItem> episodes)
    {
        mDownloadEpisodes = episodes;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        if (prefs.getBoolean("pref_disable_bluetooth", false) && Utilities.BluetoothEnabled() && Utilities.disableBluetooth(mActivity, true)) {

            unregisterNetworkCallback();

            if (isAdded())
                CommonUtils.showToast(mActivity, getString(R.string.alert_episode_network_waiting));

            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(final Network network) {
                    mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);

                    if (isAdded())
                        CommonUtils.showToast(mActivity, mActivity.getString(R.string.alert_episode_download_start));

                    for (final PodcastItem episode : episodes)
                        Utilities.startDownload(mActivity, episode, false);
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
        } else {
            if (isAdded())
                CommonUtils.showToast(mActivity, mActivity.getString(R.string.alert_episode_download_start));

            for (final PodcastItem episode : episodes)
                Utilities.startDownload(mActivity, episode, false);
        }
    }

    private static class TimeOutHandler extends Handler {
        private final WeakReference<SettingsPodcastsFragment> mMainActivityWeakReference;

        TimeOutHandler(final SettingsPodcastsFragment fragment) {
            super(Looper.getMainLooper());
            mMainActivityWeakReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(final Message msg) {
            final SettingsPodcastsFragment fragment = mMainActivityWeakReference.get();

            if (fragment != null) {
                switch (msg.what) {
                    case MESSAGE_CONNECTIVITY_TIMEOUT:
                        final Activity activity = mActivityRef.get();
                        if (activity != null && !activity.isFinishing()) {
                            final AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                            alert.setMessage(activity.getString(R.string.alert_episode_network_notfound));
                            alert.setPositiveButton(activity.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    fragment.startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), LOW_BANDWIDTH_RESULTS_CODE);
                                    dialog.dismiss();
                                }
                            });

                            alert.setNegativeButton(activity.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Utilities.enableBluetooth(activity);
                                    dialog.dismiss();
                                }
                            }).show();
                        }
                        fragment.unregisterNetworkCallback();
                        mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);
                        break;
                }
            }
        }
    }

    private void unregisterNetworkCallback() {
        if (mManager != null && mNetworkCallback != null) {
            mManager.unregisterNetworkCallback(mNetworkCallback);
            mManager.bindProcessToNetwork(null);
            mNetworkCallback = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == NO_NETWORK_RESULTS_CODE) {
                findPreference("pref_sync_podcasts").setSummary(getString(R.string.syncing));
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                new AsyncTasks.SyncPodcasts(mActivity, 0, true, findPreference("pref_sync_podcasts"),
                        new Interfaces.BackgroundSyncResponse() {
                            @Override
                            public void processFinish(final int newEpisodeCount, final int downloads, final List<PodcastItem> downloadEpisodes) {
                                if (downloadEpisodes.size() > 0)
                                    downloadEpisodes(downloadEpisodes);

                                SetContent(newEpisodeCount);
                                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            else if (requestCode == LOW_BANDWIDTH_RESULTS_CODE) {
                CommonUtils.showToast(mActivity, mActivity.getString(R.string.alert_episode_download_start));

                for (final PodcastItem episode : mDownloadEpisodes)
                    Utilities.startDownload(mActivity, episode, false);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void SetContent(final int newEpisodes)
    {
        if (mActivity == null || isAdded() == false) return;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        final String podcastDate = prefs.getString("last_podcast_sync_date", "");

        if (newEpisodes > -1)
            findPreference("pref_sync_podcasts").setSummary(newEpisodes == 1 ? getString(R.string.plurals_single_new_episode) : getString(R.string.plurals_multiple_new_episodes, newEpisodes));
        else if (podcastDate.length() > 0) {
            findPreference("pref_sync_podcasts").setSummary(getString(R.string.last_updated)
                    .concat(":\n")
                    .concat(DateUtils.GetDisplayDate(mActivity, podcastDate, "EEE MMM dd H:mm:ss Z yyyy"))
                    .concat(" @ ")
                    .concat(DateUtils.GetTime(DateUtils.ConvertDate(podcastDate, "EEE MMM dd H:mm:ss Z yyyy"))));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNoResume == false)
            SetContent(-1);
    }
}
