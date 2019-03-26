package com.krisdb.wearcasts.Settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Constants;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class SettingsPodcastsFragment extends PreferenceFragment {

    private Activity mActivity;
    private static WeakReference<Activity> mActivityRef;
    private Boolean mNoResume = false;
    /*
    private ConnectivityManager mManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private TimeOutHandler mTimeOutHandler;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);
    */

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_podcasts);
        mActivityRef = new WeakReference<>(getActivity());

        mActivity = getActivity();
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
        else if (prefs.getBoolean("pref_high_bandwidth", true) && !CommonUtils.HighBandwidthNetwork(mActivity)) {
            unregisterNetworkCallback();
            //CommonUtils.showToast(mActivity, mActivity.getString(R.string.alert_episode_network_search));

            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(final Network network) {
                    mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findPreference("pref_sync_podcasts").setSummary(getString(R.string.syncing));
                            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    });

                    new AsyncTasks.SyncPodcasts(mActivity, 0, true, findPreference("pref_sync_podcasts"),
                            new Interfaces.BackgroundSyncResponse() {
                                @Override
                                public void processFinish(final int newEpisodeCount, final int downloads, final List<PodcastItem> downloadEpisodes) {
                                    SetContent(newEpisodeCount);
                                    mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                }
                            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        else {
            findPreference("pref_sync_podcasts").setSummary(getString(R.string.syncing));
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            new AsyncTasks.SyncPodcasts(mActivity, 0, true, findPreference("pref_sync_podcasts"),
                    new Interfaces.BackgroundSyncResponse() {
                        @Override
                        public void processFinish(final int newEpisodeCount, final int downloads, final List<PodcastItem> downloadEpisodes) {
                            SetContent(newEpisodeCount);
                            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
    /*
    private static class TimeOutHandler extends Handler {
        private final WeakReference<SettingsPodcastsFragment> mActivityWeakReference;

        TimeOutHandler(final SettingsPodcastsFragment fragment) {
            mActivityWeakReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(final Message msg) {
            final SettingsPodcastsFragment fragment = mActivityWeakReference.get();

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
                                    fragment.startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), 1);
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
    */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                findPreference("pref_sync_podcasts").setSummary(getString(R.string.syncing));
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                new AsyncTasks.SyncPodcasts(mActivity, 0, true, findPreference("pref_sync_podcasts"),
                        new Interfaces.BackgroundSyncResponse() {
                            @Override
                            public void processFinish(final int newEpisodeCount, final int downloads, final List<PodcastItem> downloadEpisodes) {
                                SetContent(newEpisodeCount);
                                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //releaseHighBandwidthNetwork();
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
