package com.krisdb.wearcasts.Settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.WindowManager;

import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Constants;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Interfaces;

import java.io.File;
import java.lang.ref.WeakReference;

import static android.app.Activity.RESULT_OK;
import static com.krisdb.wearcastslibrary.CommonUtils.GetThumbnailDirectory;

public class SettingsPodcastsUpdatesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Activity mActivity;
    private Boolean mNoResume = false;
    private static WeakReference<Activity> mActivityRef;
    /*
    private ConnectivityManager mManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private TimeOutHandler mTimeOutHandler;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(7);
    */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_podcasts_updates);
        mActivityRef = new WeakReference<>(getActivity());

        mActivity = getActivity();
        //mTimeOutHandler = new TimeOutHandler(this);
        //mManager = (ConnectivityManager)mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        setDeleteThumbnailsTitle();

        findPreference("pref_delete_thumbs").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                    alert.setMessage(getString(R.string.confirm_delete_all_thumbs));
                    alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int count = Utilities.deleteAllThumbnails(mActivity);

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
                }
                return false;
            }
        });

        final SwitchPreference cbSound = (SwitchPreference)findPreference("pref_updates_new_episodes_sound");
        //findPreference("pref_updates_new_episodes_disable_start").setEnabled(cbSound.isChecked());
        //findPreference("pref_updates_new_episodes_disable_end").setEnabled(cbSound.isChecked());

        if (cbSound.isChecked()) {
            findPreference("pref_updates_new_episodes_disable_start").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_start")).getEntry());
            findPreference("pref_updates_new_episodes_disable_end").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_end")).getEntry());
        }
        else
        {
            findPreference("pref_updates_new_episodes_disable_start").setSummary("");
            findPreference("pref_updates_new_episodes_disable_end").setSummary("");
        }

        findPreference("pref_sync_art").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                handleNetwork();
                return false;
            }
        });
        SetContent();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private void setDeleteThumbnailsTitle()
    {
        final File thumbsDirectory = new File(GetThumbnailDirectory(mActivity));
        final String[] thumbs = thumbsDirectory.list();

        if (thumbs != null)
            findPreference("pref_delete_thumbs").setTitle(getString(R.string.settings_podcasts_label_downloads_thumbs_all,  thumbs.length));

        long size = Utilities.getFilesSize(GetThumbnailDirectory(mActivity));

        if (size > 0)
            findPreference("pref_delete_thumbs").setSummary(android.text.format.Formatter.formatShortFileSize(getActivity(), size));
        else
            findPreference("pref_delete_thumbs").setSummary("");
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
        else if (prefs.getBoolean("pref_high_bandwidth", true) && !CommonUtils.HighBandwidthNetwork(mActivity))
        {
            unregisterNetworkCallback();
            //CommonUtils.showToast(mActivity, mActivity.getString(R.string.alert_episode_network_search));

            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(final Network network) {
                    mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findPreference("pref_sync_art").setSummary(getString(R.string.syncing));
                            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    });

                    new AsyncTasks.SyncArt(mActivity, findPreference("pref_sync_art"),
                            new Interfaces.AsyncResponse() {
                                @Override
                                public void processFinish() {
                                    SetContent();
                                    setDeleteThumbnailsTitle();
                                    mActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                        }
                                    });
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
            findPreference("pref_sync_art").setSummary(getString(R.string.syncing));
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            new AsyncTasks.SyncArt(mActivity, findPreference("pref_sync_art"),
                    new Interfaces.AsyncResponse() {
                        @Override
                        public void processFinish() {
                            SetContent();
                            setDeleteThumbnailsTitle();
                            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
    /*
    private static class TimeOutHandler extends Handler {
        private final WeakReference<SettingsPodcastsUpdatesFragment> mActivityWeakReference;

        TimeOutHandler(final SettingsPodcastsUpdatesFragment fragment) {
            mActivityWeakReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(final Message msg) {
            final SettingsPodcastsUpdatesFragment fragment = mActivityWeakReference.get();

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
                findPreference("pref_sync_art").setSummary(getString(R.string.syncing));
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                new AsyncTasks.SyncArt(mActivity, findPreference("pref_sync_art"),
                        new Interfaces.AsyncResponse() {
                            @Override
                            public void processFinish() {
                                SetContent();
                                setDeleteThumbnailsTitle();
                                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mNoResume == false)
            SetContent();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private void SetContent()
    {
        if (mActivity == null || isAdded() == false) return;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        final String thumbnailDate = prefs.getString("last_thumbnail_sync_date", "");

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

        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        //releaseHighBandwidthNetwork();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {

        if (key.equals("pref_updates_new_episodes_sound")) {
            final SwitchPreference cbSound = (SwitchPreference)findPreference("pref_updates_new_episodes_sound");

            //findPreference("pref_updates_new_episodes_disable_start").setEnabled(cbSound.isChecked());
            //findPreference("pref_updates_new_episodes_disable_end").setEnabled(cbSound.isChecked());

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
            else
                Utilities.CancelJob(mActivity.getApplicationContext());
        }

        if (key.equals("pref_download_sound_disable_start"))
            findPreference("pref_download_sound_disable_start").setSummary(((ListPreference) findPreference("pref_download_sound_disable_start")).getEntry());

        if (key.equals("pref_download_sound_disable_end"))
            findPreference("pref_download_sound_disable_end").setSummary(((ListPreference) findPreference("pref_download_sound_disable_end")).getEntry());


        SetContent();
    }
}
