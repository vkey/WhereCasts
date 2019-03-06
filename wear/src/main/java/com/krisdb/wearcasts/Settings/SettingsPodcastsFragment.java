package com.krisdb.wearcasts.Settings;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.krisdb.wearcasts.R;

public class SettingsPodcastsFragment extends PreferenceFragment {

    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_podcasts);

        mActivity = getActivity();

        findPreference("pref_updates").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPodcastsUpdatesActivity.class));
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
    }
}
