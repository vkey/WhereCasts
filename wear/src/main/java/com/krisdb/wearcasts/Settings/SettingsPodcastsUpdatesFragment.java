package com.krisdb.wearcasts.Settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.WindowManager;

import com.krisdb.wearcasts.Async.SyncArt;
import com.krisdb.wearcasts.Async.SyncPodcasts;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.app.Activity.RESULT_OK;
import static com.krisdb.wearcastslibrary.CommonUtils.GetPodcastsThumbnailDirectory;

public class SettingsPodcastsUpdatesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Activity mActivity;
    private static WeakReference<Activity> mActivityRef;
    private static int SYNC_ART_RESULTS_CODE = 101;
    private static int SYNC_PODCASTS_RESULTS_CODE = 102;
    private static int LOW_BANDWIDTH_RESULTS_CODE = 103;
    private static TimeOutHandler mTimeOutHandler;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);
    private List<PodcastItem> mDownloadEpisodes;
    private ConnectivityManager mManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_podcasts_updates);
        mActivityRef = new WeakReference<>(getActivity());

        mActivity = getActivity();
        mTimeOutHandler = new TimeOutHandler(this);
        mManager = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        findPreference("pref_delete_thumbs").setOnPreferenceClickListener(preference -> {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setMessage(getString(R.string.confirm_delete_all_thumbs));
                alert.setPositiveButton(getString(R.string.confirm_yes), (dialog, which) -> {
                    int count = Utilities.deleteAllThumbnails(mActivity);

                    String message;

                    if (count == 0)
                        message = getString(R.string.alert_file_none_deleted);
                    else if (count == 1)
                        message = getString(R.string.alert_file_deleted);
                    else
                        message = getString(R.string.alert_files_deleted, count);

                    Utilities.ShowConfirmationActivity(getActivity(), message);
                    //CommonUtils.showToast(getActivity(), message);

                    setDeleteThumbnailsTitle();
                });
                alert.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss());
                alert.show();
            }
            return false;
        });

        final SwitchPreference cbUpdates = (SwitchPreference) findPreference("updatesEnabled");
        findPreference("updateInterval").setEnabled(cbUpdates.isChecked());
        findPreference("updateCharging").setEnabled(cbUpdates.isChecked());
        final SwitchPreference cbSound = (SwitchPreference) findPreference("pref_updates_new_episodes_sound");
        cbSound.setEnabled(cbUpdates.isChecked() && cbSound.isChecked());
        findPreference("pref_updates_new_episodes_disable_start").setEnabled(cbUpdates.isChecked() && cbSound.isChecked());
        findPreference("pref_updates_new_episodes_disable_end").setEnabled(cbUpdates.isChecked() && cbSound.isChecked());

        if (cbSound.isChecked()) {
            findPreference("pref_updates_new_episodes_disable_start").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_start")).getEntry());
            findPreference("pref_updates_new_episodes_disable_end").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_end")).getEntry());
        } else {
            findPreference("pref_updates_new_episodes_disable_start").setSummary("");
            findPreference("pref_updates_new_episodes_disable_end").setSummary("");
        }

        findPreference("updateInterval").setSummary(((ListPreference) findPreference("updateInterval")).getEntry());

        findPreference("pref_sync_art").setOnPreferenceClickListener(preference -> {
            handleNetwork(false);
            return false;
        });

        findPreference("pref_sync_podcasts").setOnPreferenceClickListener(preference -> {
            handleNetwork(true);
            return false;
        });

        SetContent();
        setDeleteThumbnailsTitle();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private void setDeleteThumbnailsTitle() {
        final File thumbsDirectory = new File(GetPodcastsThumbnailDirectory(mActivity));
        final String[] thumbs = thumbsDirectory.list();

        if (thumbs != null)
            findPreference("pref_delete_thumbs").setTitle(getString(R.string.settings_podcasts_label_downloads_thumbs_all, thumbs.length));

        long size = Utilities.getFilesSize(GetPodcastsThumbnailDirectory(mActivity));

        if (size > 0)
            findPreference("pref_delete_thumbs").setSummary(android.text.format.Formatter.formatShortFileSize(getActivity(), size));
        else
            findPreference("pref_delete_thumbs").setSummary("");
    }

    private void handleNetwork(final boolean podcasts) {

        if (!CommonUtils.isNetworkAvailable(mActivity)) {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                alert.setMessage(getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(getString(R.string.confirm_yes), (dialog, which) -> {
                    startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), podcasts ? SYNC_PODCASTS_RESULTS_CODE : SYNC_ART_RESULTS_CODE);
                    dialog.dismiss();
                });

                alert.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss()).show();
            }
        } else {
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            if (podcasts) {
                findPreference("pref_sync_podcasts").setSummary(getString(R.string.syncing));

                CommonUtils.executeSingleThreadAsync(new SyncPodcasts(mActivity, 0), (SyncPodcastsResponse) -> {
                    if (isAdded()) {

                        if (SyncPodcastsResponse.getNewEpisodeCount() > 0)
                            Utilities.SetPodcstRefresh(mActivity);

                        if (SyncPodcastsResponse.getDownloadEpisodes().size() > 0)
                            downloadEpisodes(SyncPodcastsResponse.getDownloadEpisodes());

                        SetContent(SyncPodcastsResponse.getNewEpisodeCount());
                        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                });
            } else {
                findPreference("pref_sync_art").setSummary(getString(R.string.syncing));

                CommonUtils.executeSingleThreadAsync(new SyncArt(mActivity), (response) -> {
                    if (isAdded()) {
                        SetContent();
                        setDeleteThumbnailsTitle();
                        Utilities.SetPodcstRefresh(mActivity);
                        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                });
            }
        }
    }

    private void downloadEpisodes(final List<PodcastItem> episodes) {
        mDownloadEpisodes = episodes;

        if (Utilities.disableBluetooth(mActivity, true)) {

            unregisterNetworkCallback();

            if (isAdded() && !CommonUtils.isNetworkAvailable(mActivity, true))
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
        private final WeakReference<SettingsPodcastsUpdatesFragment> mMainActivityWeakReference;

        TimeOutHandler(final SettingsPodcastsUpdatesFragment fragment) {
            super(Looper.getMainLooper());
            mMainActivityWeakReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(final Message msg) {
            final SettingsPodcastsUpdatesFragment fragment = mMainActivityWeakReference.get();

            if (fragment != null) {
                switch (msg.what) {
                    case MESSAGE_CONNECTIVITY_TIMEOUT:
                        final Activity activity = mActivityRef.get();
                        if (activity != null && !activity.isFinishing()) {
                            final AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                            alert.setMessage(activity.getString(R.string.alert_episode_network_notfound));
                            alert.setPositiveButton(activity.getString(R.string.confirm_yes), (dialog, which) -> {
                                fragment.startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), LOW_BANDWIDTH_RESULTS_CODE);
                                dialog.dismiss();
                            });

                            alert.setNegativeButton(activity.getString(R.string.confirm_no), (dialog, which) -> {
                                Utilities.enableBluetooth(activity);
                                dialog.dismiss();
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
            if (requestCode == SYNC_ART_RESULTS_CODE) {
                findPreference("pref_sync_art").setSummary(getString(R.string.syncing));
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                CommonUtils.executeSingleThreadAsync(new SyncArt(mActivity), (response) -> {
                    if (isAdded()) {
                        SetContent();
                        setDeleteThumbnailsTitle();
                        Utilities.SetPodcstRefresh(mActivity);
                        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                });
            } else if (requestCode == SYNC_PODCASTS_RESULTS_CODE) {
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                findPreference("pref_sync_podcasts").setSummary(getString(R.string.syncing));

                CommonUtils.executeSingleThreadAsync(new SyncPodcasts(mActivity, 0), (SyncPodcastsResponse) -> {
                    if (isAdded()) {

                        if (SyncPodcastsResponse.getNewEpisodeCount() > 0)
                            Utilities.SetPodcstRefresh(mActivity);

                        if (SyncPodcastsResponse.getDownloadEpisodes().size() > 0)
                            downloadEpisodes(SyncPodcastsResponse.getDownloadEpisodes());

                        SetContent(SyncPodcastsResponse.getNewEpisodeCount());
                        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                });
            } else if (requestCode == LOW_BANDWIDTH_RESULTS_CODE) {
                CommonUtils.showToast(mActivity, mActivity.getString(R.string.alert_episode_download_start));

                for (final PodcastItem episode : mDownloadEpisodes)
                    Utilities.startDownload(mActivity, episode, false);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private void SetContent() {
        SetContent(-1);
    }

    private void SetContent(final int newEpisodes) {
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

        final String thumbnailDate = prefs.getString("last_thumbnail_sync_date", "");

        if (thumbnailDate.length() > 0) {
            findPreference("pref_sync_art").setSummary(getString(R.string.last_updated)
                    .concat(":\n")
                    .concat(DateUtils.GetDisplayDate(mActivity, thumbnailDate, "EEE MMM dd H:mm:ss Z yyyy"))
                    .concat(" @ ")
                    .concat(DateUtils.GetTime(DateUtils.ConvertDate(thumbnailDate, "EEE MMM dd H:mm:ss Z yyyy"))));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        final SwitchPreference cbSound = (SwitchPreference) findPreference("pref_updates_new_episodes_sound");
        final SwitchPreference cbUpdates = (SwitchPreference) findPreference("updatesEnabled");

        if (key.equals("updatesEnabled")) {
            findPreference("updateInterval").setEnabled(cbUpdates.isChecked());
            findPreference("updateCharging").setEnabled(cbUpdates.isChecked());
            cbSound.setEnabled(cbUpdates.isChecked());
            findPreference("pref_updates_new_episodes_disable_start").setEnabled(cbUpdates.isChecked() && cbSound.isChecked());
            findPreference("pref_updates_new_episodes_disable_end").setEnabled(cbUpdates.isChecked() && cbSound.isChecked());
        }

        if (key.equals("pref_updates_new_episodes_sound")) {
            findPreference("pref_updates_new_episodes_disable_start").setEnabled(cbUpdates.isChecked() && cbSound.isChecked());
            findPreference("pref_updates_new_episodes_disable_end").setEnabled(cbUpdates.isChecked() && cbSound.isChecked());

            if (!cbSound.isChecked()) {
                findPreference("pref_updates_new_episodes_disable_start").setSummary("");
                findPreference("pref_updates_new_episodes_disable_end").setSummary("");
            } else {
                findPreference("pref_updates_new_episodes_disable_start").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_start")).getEntry());
                findPreference("pref_updates_new_episodes_disable_end").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_end")).getEntry());
            }
        }

        if (key.equals("pref_updates_new_episodes_disable_start"))
            findPreference("pref_updates_new_episodes_disable_start").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_start")).getEntry());

        if (key.equals("pref_updates_new_episodes_disable_end"))
            findPreference("pref_updates_new_episodes_disable_end").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_end")).getEntry());

        if (key.equals("updatesEnabled") || key.equals("updateInterval") || key.equals("updateCharging")) {
            Utilities.CancelJob(mActivity.getApplicationContext());
            if (((SwitchPreference) findPreference("updatesEnabled")).isChecked())
                Utilities.StartJob(mActivity.getApplicationContext());
        }

        if (key.equals("pref_download_sound_disable_start"))
            findPreference("pref_download_sound_disable_start").setSummary(((ListPreference) findPreference("pref_download_sound_disable_start")).getEntry());

        if (key.equals("pref_download_sound_disable_end"))
            findPreference("pref_download_sound_disable_end").setSummary(((ListPreference) findPreference("pref_download_sound_disable_end")).getEntry());

        if (key.equals("updateInterval"))
            findPreference("updateInterval").setSummary(((ListPreference) findPreference("updateInterval")).getEntry());
    }
}