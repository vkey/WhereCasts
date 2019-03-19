package com.krisdb.wearcasts.Settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;

import com.krisdb.wearcasts.Fragments.BasePreferenceFragmentCompat;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.CacheUtils;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsPodcastsPodcastsFragment extends BasePreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_podcasts_podcasts);

        findPreference("pref_display_podcasts_sort_order").setSummary(((ListPreference)findPreference("pref_display_podcasts_sort_order")).getEntry());

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        SystemClock.sleep(500);


        if (key.equals("pref_display_podcasts_sort_order")) {
            findPreference("pref_display_podcasts_sort_order").setSummary(((ListPreference) findPreference("pref_display_podcasts_sort_order")).getEntry());
            CacheUtils.deletePodcastsCache(getActivity());
        }

    }
}
