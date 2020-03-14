package com.krisdb.wearcasts.Settings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.krisdb.wearcasts.R;

public class SettingsPodcastsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_podcasts);

        findPreference("pref_updates").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), SettingsPodcastsUpdatesActivity.class));
            return false;
        });

        findPreference("pref_podcasts").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), SettingsPodcastsPodcastsActivity.class));
            return false;
        });

        findPreference("pref_episodes").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), SettingsPodcastsEpisodesActivity.class));
            return false;
        });

        findPreference("pref_display").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), SettingsPodcastsDisplayActivity.class));
            return false;
        });

        findPreference("pref_downloads").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), SettingsPodcastsDownloadsActivity.class));
            return false;
        });

        findPreference("pref_playback").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), SettingsPodcastsPlaybackActivity.class));
            return false;
        });

        findPreference("pref_playlists").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), SettingsPlaylistsActivity.class));
            return false;
        });
    }
}
