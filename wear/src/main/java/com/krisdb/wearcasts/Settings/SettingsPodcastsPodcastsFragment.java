package com.krisdb.wearcasts.Settings;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;

import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.CacheUtils;
import com.krisdb.wearcasts.Utilities.DBUtilities;
import com.krisdb.wearcasts.Utilities.Utilities;

import java.util.List;
import java.util.Objects;

public class SettingsPodcastsPodcastsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
