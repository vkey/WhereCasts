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
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Interfaces;

import java.io.File;

import static android.app.Activity.RESULT_OK;
import static com.krisdb.wearcastslibrary.CommonUtils.GetThumbnailDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class SettingsPodcastsUpdatesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Activity mActivity;
    private ConnectivityManager mConnectivityManager;
    private Handler mNetworkHandler = new Handler();
    private Boolean mNoResume = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_podcasts_updates);

        mActivity = getActivity();
        setDeleteThumbnailsTitle();

        findPreference("pref_delete_thumbs").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setMessage(getString(R.string.confirm_delete_all_thumbs));
                alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int count = Utilities.deleteAllThumbnails();

                        String message;

                        if (count == 0)
                            message = getString(R.string.alert_file_none_deleted);
                        else if (count == 1)
                            message = getString(R.string.alert_file_deleted);
                        else
                            message = getString(R.string.alert_files_deleted, count);

                        CommonUtils.showToast(getActivity(), message);

                        setDeleteThumbnailsTitle();
                    }
                });
                alert.setNegativeButton(getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alert.show();
                return false;
            }
        });

        final SwitchPreference cbSound = (SwitchPreference)findPreference("pref_updates_new_episodes_sound");
        findPreference("pref_updates_new_episodes_disable_start").setEnabled(cbSound.isChecked());
        findPreference("pref_updates_new_episodes_disable_end").setEnabled(cbSound.isChecked());

        if (cbSound.isChecked()) {
            findPreference("pref_updates_new_episodes_disable_start").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_start")).getEntry());
            findPreference("pref_updates_new_episodes_disable_end").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_end")).getEntry());
        }
        else
        {
            findPreference("pref_updates_new_episodes_disable_start").setSummary("");
            findPreference("pref_updates_new_episodes_disable_end").setSummary("");
        }

        findPreference("pref_sync_podcasts").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                handleNetwork(mNetworkCallbackPodcasts);
                return false;
            }
        });

        findPreference("pref_sync_art").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                handleNetwork(mNetworkCallbackArt);
                return false;
            }
        });
        SetContent();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private void setDeleteThumbnailsTitle()
    {
        final File thumbsDirectory = new File(GetThumbnailDirectory());
        final String[] thumbs = thumbsDirectory.list();

        if (thumbs != null)
            findPreference("pref_delete_thumbs").setTitle(getString(R.string.settings_podcasts_label_downloads_thumbs_all,  thumbs.length));

        long size = Utilities.getFilesSize(GetThumbnailDirectory());

        if (size > 0)
            findPreference("pref_delete_thumbs").setSummary(android.text.format.Formatter.formatShortFileSize(getActivity(), size));
        else
            findPreference("pref_delete_thumbs").setSummary("");
    }

    private void handleNetwork(final ConnectivityManager.NetworkCallback callback)
    {
        if (PreferenceManager.getDefaultSharedPreferences(mActivity).getBoolean("pref_high_bandwidth", true) == false)
        {
            if (callback == mNetworkCallbackPodcasts) {
                findPreference("pref_sync_podcasts").setSummary(getString(R.string.syncing));
                new AsyncTasks.SyncPodcasts(mActivity, 0, true,
                        new Interfaces.BackgroundSyncResponse() {
                            @Override
                            public void processFinish(final int count, final int downloads) {
                                SetContent();
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            else
            {
                findPreference("pref_sync_art").setSummary(getString(R.string.syncing));
                new AsyncTasks.SyncArt(mActivity,
                        new Interfaces.AsyncResponse(){
                            @Override
                            public void processFinish(
                            ) {
                                SetContent();
                                setDeleteThumbnailsTitle();
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            return;
        }

        mNetworkHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        mConnectivityManager.bindProcessToNetwork(null);
                        mConnectivityManager.unregisterNetworkCallback(callback);
                        mNetworkHandler.removeMessages(1);

                        final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                        alert.setMessage(getString(R.string.alert_episode_network_notfound));
                        alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivityForResult(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"), (callback == mNetworkCallbackPodcasts) ? 1 : 2);
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

                mConnectivityManager.requestNetwork(request, callback);
                showToast(mActivity.getApplicationContext(), getString(R.string.alert_episode_network_search));
                mNetworkHandler.sendMessageDelayed( mNetworkHandler.obtainMessage(1), 10000);
            }
            else {

                if (callback == mNetworkCallbackPodcasts) {
                    findPreference("pref_sync_podcasts").setSummary(getString(R.string.syncing));
                    new AsyncTasks.SyncPodcasts(mActivity, 0, true,
                            new Interfaces.BackgroundSyncResponse() {
                                @Override
                                public void processFinish(final int count, final int downloads) {
                                    SetContent();
                                }
                            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                else
                {
                    findPreference("pref_sync_art").setSummary(getString(R.string.syncing));
                    new AsyncTasks.SyncArt(mActivity,
                            new Interfaces.AsyncResponse(){
                                @Override
                                public void processFinish(
                                ) {
                                    SetContent();
                                    setDeleteThumbnailsTitle();
                                }
                            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        }
        else
        {
            final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
            alert.setMessage(getString(R.string.alert_episode_network_notfound));
            alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivityForResult(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"), (callback == mNetworkCallbackPodcasts) ? 1 : 2);
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
    public void onResume() {
        super.onResume();
        if (mNoResume == false)
            SetContent();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mConnectivityManager != null) {
            mConnectivityManager.bindProcessToNetwork(null);

            try {
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallbackArt);
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallbackPodcasts);
            }
            catch(Exception ignored){}
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            mNoResume = true;
            if (requestCode == 1) {
                findPreference("pref_sync_podcasts").setSummary(getString(R.string.syncing));
                new AsyncTasks.SyncPodcasts(mActivity, 0, true,
                        new Interfaces.BackgroundSyncResponse() {
                            @Override
                            public void processFinish(final int count, final int downloads) {
                                SetContent();
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                findPreference("pref_sync_art").setSummary(getString(R.string.syncing));
                new AsyncTasks.SyncArt(mActivity,
                        new Interfaces.AsyncResponse() {
                            @Override
                            public void processFinish(
                            ) {
                                SetContent();
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    final ConnectivityManager.NetworkCallback mNetworkCallbackPodcasts = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            mActivity.runOnUiThread(new Runnable() {
                public void run(){
                    findPreference("pref_sync_podcasts").setSummary(getString(R.string.syncing));
                }
            });
            new AsyncTasks.SyncPodcasts(mActivity, 0, true,
                    new Interfaces.BackgroundSyncResponse(){
                        @Override
                        public void processFinish(final int count, final int downloads) {SetContent();}
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            mNetworkHandler.removeMessages(1);
        }
    };

    final ConnectivityManager.NetworkCallback mNetworkCallbackArt = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            mActivity.runOnUiThread(new Runnable() {
                public void run(){
                    findPreference("pref_sync_art").setSummary(getString(R.string.syncing));
                }
            });
            new AsyncTasks.SyncArt(mActivity,
                    new Interfaces.AsyncResponse(){
                        @Override
                        public void processFinish(
                        ) {SetContent();}
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            mNetworkHandler.removeMessages(1);
        }
    };

    private void SetContent()
    {
        if (mActivity == null || isAdded() == false) return;

        final SwitchPreference cbUpdatesEnabled = (SwitchPreference)findPreference("updatesEnabled");
        final CheckBoxPreference cbUpdatesCharging = (CheckBoxPreference)getPreferenceScreen().findPreference("updateCharging");
        final ListPreference lpUpdateInterval = (ListPreference)findPreference("updateInterval");

        final Boolean updatesEnabled = cbUpdatesEnabled.isChecked();
        lpUpdateInterval.setEnabled(updatesEnabled);
        cbUpdatesCharging.setEnabled(updatesEnabled);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        final String podcastDate = prefs.getString("last_podcast_sync_date", "");
        final String thumbnailDate = prefs.getString("last_thumbnail_sync_date", "");

        if (podcastDate.length() > 0) {
            findPreference("pref_sync_podcasts").setSummary(getString(R.string.last_updated)
                    .concat(":\n")
                    .concat(DateUtils.GetDisplayDate(mActivity, podcastDate, "EEE MMM dd H:mm:ss Z yyyy"))
                    .concat(" @ ")
                    .concat(DateUtils.GetTime(DateUtils.ConvertDate(podcastDate, "EEE MMM dd H:mm:ss Z yyyy"))));
        }

        if (thumbnailDate.length() > 0) {
            findPreference("pref_sync_art").setSummary(getString(R.string.last_updated)
                    .concat(":\n")
                    .concat(DateUtils.GetDisplayDate(mActivity, thumbnailDate, "EEE MMM dd H:mm:ss Z yyyy"))
                    .concat(" @ ")
                    .concat(DateUtils.GetTime(DateUtils.ConvertDate(thumbnailDate, "EEE MMM dd H:mm:ss Z yyyy"))));
        }

        findPreference("updateInterval").setSummary(((ListPreference)findPreference("updateInterval")).getEntry());
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onPause() {
        super.onPause();
        mNoResume = false;

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {

        if (key.equals("pref_updates_new_episodes_sound")) {
            final SwitchPreference cbSound = (SwitchPreference)findPreference("pref_updates_new_episodes_sound");

            findPreference("pref_updates_new_episodes_disable_start").setEnabled(cbSound.isChecked());
            findPreference("pref_updates_new_episodes_disable_end").setEnabled(cbSound.isChecked());

            if (!cbSound.isChecked()) {
                findPreference("pref_updates_new_episodes_disable_start").setSummary("");
                findPreference("pref_updates_new_episodes_disable_end").setSummary("");
            }
            else
            {
                findPreference("pref_updates_new_episodes_disable_start").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_start")).getEntry());
                findPreference("pref_updates_new_episodes_disable_end").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_end")).getEntry());
            }
        }
        if (key.equals("pref_updates_new_episodes_disable_start"))
            findPreference("pref_updates_new_episodes_disable_start").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_start")).getEntry());

        if (key.equals("pref_updates_new_episodes_disable_end"))
            findPreference("pref_updates_new_episodes_disable_end").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_end")).getEntry());

        if (key.equals("updatesEnabled") || key.equals("updateInterval") || key.equals("updateCharging")) {
            if (((SwitchPreference)findPreference("updatesEnabled")).isChecked())
                Utilities.StartJob(mActivity.getApplicationContext());
                //Utilities.StartAlarm(mActivity.getApplicationContext());
            else
                Utilities.CancelJob(mActivity.getApplicationContext());
            //Utilities.CancelAlarm(mActivity.getApplicationContext());
        }

        if (key.equals("pref_download_sound_disable_start"))
            findPreference("pref_download_sound_disable_start").setSummary(((ListPreference) findPreference("pref_download_sound_disable_start")).getEntry());

        if (key.equals("pref_download_sound_disable_end"))
            findPreference("pref_download_sound_disable_end").setSummary(((ListPreference) findPreference("pref_download_sound_disable_end")).getEntry());


        SetContent();
    }
}