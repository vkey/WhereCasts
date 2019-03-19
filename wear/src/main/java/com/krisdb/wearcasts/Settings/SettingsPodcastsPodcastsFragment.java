package com.krisdb.wearcasts.Settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
<<<<<<< HEAD
=======
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
>>>>>>> parent of 638f5a8... preferences update

import com.krisdb.wearcasts.Fragments.BasePreferenceFragmentCompat;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.CacheUtils;

<<<<<<< HEAD
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsPodcastsPodcastsFragment extends BasePreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
=======
public class SettingsPodcastsPodcastsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
>>>>>>> parent of 638f5a8... preferences update

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

<<<<<<< HEAD

    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
=======
>>>>>>> parent of 638f5a8... preferences update
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
