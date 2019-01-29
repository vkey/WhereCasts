package com.krisdb.wearcasts;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import com.krisdb.wearcastslibrary.CommonUtils;

import java.io.File;
import java.lang.ref.WeakReference;

import static com.krisdb.wearcasts.Utilities.GetMediaDirectory;

public class SettingsPodcastsDownloadsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener  {
    private static WeakReference<Activity> mActivityRef;

   @Override
    public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);

       addPreferencesFromResource(R.xml.settings_podcasts_downloads);
       mActivityRef = new WeakReference<>(getActivity());

       final SwitchPreference cbSound = (SwitchPreference)findPreference("pref_download_complete_sound");
       findPreference("pref_download_sound_disable_start").setEnabled(cbSound.isChecked());
       findPreference("pref_download_sound_disable_end").setEnabled(cbSound.isChecked());

       if (cbSound.isChecked()) {
           findPreference("pref_download_sound_disable_start").setSummary(((ListPreference) findPreference("pref_download_sound_disable_start")).getEntry());
           findPreference("pref_download_sound_disable_end").setSummary(((ListPreference) findPreference("pref_download_sound_disable_end")).getEntry());
       }
       else
       {
           findPreference("pref_download_sound_disable_start").setSummary("");
           findPreference("pref_download_sound_disable_end").setSummary("");
       }

       /*
       if (BluetoothAdapter.getDefaultAdapter() == null)
       {
           PreferenceCategory category = (PreferenceCategory)findPreference("pref_general");
           category.removePreference(findPreference("pref_disable_bluetooth"));
       }
       */

       setDeleteDownloadsTitle();

       findPreference("pref_delete_downloads").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
           public boolean onPreferenceClick(Preference preference) {
               if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                   final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                   alert.setMessage(getString(R.string.confirm_delete_all_downloads));
                   alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                           int count = Utilities.deleteAllDownloadedFiles();

                           final DBPodcastsEpisodes db = new DBPodcastsEpisodes(getActivity());
                           final ContentValues cv = new ContentValues();
                           cv.put("downloadid", 0);
                           cv.put("download", 0);
                           cv.put("downloadurl", "");

                           db.updateAll(cv);
                           db.close();

                           String message;

                           if (count == 0)
                               message = getString(R.string.alert_file_none_deleted);
                           else if (count == 1)
                               message = getString(R.string.alert_file_deleted);
                           else
                               message = getString(R.string.alert_files_deleted, count);

                           CommonUtils.showToast(getActivity(), message);

                           final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

                           if (prefs.getBoolean("pref_hide_empty_playlists", false)) {
                               final SharedPreferences.Editor editor = prefs.edit();
                               editor.putBoolean("refresh_vp", true);
                               editor.apply();
                           }
                           setDeleteDownloadsTitle();
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

       findPreference("pref_cancel_downloads").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
           public boolean onPreferenceClick(Preference preference) {
                Utilities.cancelAllDownloads(getActivity());
                CommonUtils.showToast(getActivity(), getString(R.string.alert_downloads_all_cancelled));
               return false;
           }
       });

       getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
   }

   private void setDeleteDownloadsTitle()
   {
       final File episodesDirectory = new File(GetMediaDirectory());
       final String[] episodes = episodesDirectory.list();

       if (episodes != null)
           findPreference("pref_delete_downloads").setTitle(getString(R.string.settings_podcasts_label_downloads_delete_all,  episodes.length));

       long size = Utilities.getFilesSize(GetMediaDirectory());

       if (size > 0)
           findPreference("pref_delete_downloads").setSummary(android.text.format.Formatter.formatShortFileSize(getActivity(), size));
       else
           findPreference("pref_delete_downloads").setSummary("");
   }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        SystemClock.sleep(500);

        if (key.equals("pref_download_complete_sound")) {
            final SwitchPreference cbSound = (SwitchPreference)findPreference("pref_download_complete_sound");

            findPreference("pref_download_sound_disable_start").setEnabled(cbSound.isChecked());
            findPreference("pref_download_sound_disable_end").setEnabled(cbSound.isChecked());

            if (!cbSound.isChecked()) {
                findPreference("pref_download_sound_disable_start").setSummary("");
                findPreference("pref_download_sound_disable_end").setSummary("");
            }
            else
            {
                findPreference("pref_download_sound_disable_start").setSummary(((ListPreference) findPreference("pref_download_sound_disable_start")).getEntry());
                findPreference("pref_download_sound_disable_end").setSummary(((ListPreference) findPreference("pref_download_sound_disable_end")).getEntry());
            }
        }

        if (key.equals("pref_download_sound_disable_start"))
            findPreference("pref_download_sound_disable_start").setSummary(((ListPreference) findPreference("pref_download_sound_disable_start")).getEntry());

        if (key.equals("pref_download_sound_disable_end"))
            findPreference("pref_download_sound_disable_end").setSummary(((ListPreference) findPreference("pref_download_sound_disable_end")).getEntry());
    }
}
