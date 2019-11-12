package com.krisdb.wearcasts.Settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.WindowManager;

import com.krisdb.wearcasts.Async.SyncArt;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;

import java.io.File;
import java.lang.ref.WeakReference;

import static android.app.Activity.RESULT_OK;
import static com.krisdb.wearcastslibrary.CommonUtils.GetThumbnailDirectory;

public class SettingsPodcastsUpdatesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Activity mActivity;
    private Boolean mNoResume = false;
    private static WeakReference<Activity> mActivityRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_podcasts_updates);
        mActivityRef = new WeakReference<>(getActivity());

        mActivity = getActivity();

        setDeleteThumbnailsTitle();

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

        final SwitchPreference cbUpdates = (SwitchPreference)findPreference("updatesEnabled");
        findPreference("updateInterval").setEnabled(cbUpdates.isChecked());
        findPreference("updateCharging").setEnabled(cbUpdates.isChecked());
        final SwitchPreference cbSound = (SwitchPreference)findPreference("pref_updates_new_episodes_sound");
        cbSound.setEnabled(cbUpdates.isChecked() && cbSound.isChecked());
        findPreference("pref_updates_new_episodes_disable_start").setEnabled(cbUpdates.isChecked() && cbSound.isChecked());
        findPreference("pref_updates_new_episodes_disable_end").setEnabled(cbUpdates.isChecked() && cbSound.isChecked());

        if (cbSound.isChecked()) {
            findPreference("pref_updates_new_episodes_disable_start").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_start")).getEntry());
            findPreference("pref_updates_new_episodes_disable_end").setSummary(((ListPreference) findPreference("pref_updates_new_episodes_disable_end")).getEntry());
        }
        else
        {
            findPreference("pref_updates_new_episodes_disable_start").setSummary("");
            findPreference("pref_updates_new_episodes_disable_end").setSummary("");
        }

        findPreference("pref_sync_art").setOnPreferenceClickListener(preference -> {
            handleNetwork();
            return false;
        });
        SetContent();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private void setDeleteThumbnailsTitle()
    {
        if (!isAdded()) return;

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

        if (!CommonUtils.isNetworkAvailable(mActivity))
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                alert.setMessage(getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(getString(R.string.confirm_yes), (dialog, which) -> {
                    startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), 1);
                    dialog.dismiss();
                });

                alert.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss()).show();
            }
        }
        else {
            findPreference("pref_sync_art").setSummary(getString(R.string.syncing));
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            CommonUtils.executeSingleThreadAsync(new SyncArt(mActivity), (response) -> {
                SetContent();
                setDeleteThumbnailsTitle();
                Utilities.SetPodcstRefresh(mActivity);
                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                findPreference("pref_sync_art").setSummary(getString(R.string.syncing));
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                CommonUtils.executeSingleThreadAsync(new SyncArt(mActivity), (response) -> {
                    SetContent();
                    setDeleteThumbnailsTitle();
                    Utilities.SetPodcstRefresh(mActivity);
                    mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                });
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


        SetContent();
    }
}