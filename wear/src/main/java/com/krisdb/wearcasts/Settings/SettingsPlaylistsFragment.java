package com.krisdb.wearcasts.Settings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.CommonUtils;

import java.util.List;
import java.util.Objects;

import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;

public class SettingsPlaylistsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.settings_playlists);

        final PreferenceCategory category = (PreferenceCategory)findPreference("playlist_settings");
        mActivity = getActivity();

        final List<PlaylistItem> playlists = getPlaylists(mActivity);

        if (playlists.size() == 0) {
            findPreference("pref_playlists_empty").setTitle(R.string.text_playlists_pref_empty_title);
            findPreference("pref_playlists_empty").setSummary(R.string.text_playlists_pref_empty_summary);
        }
        else {
            category.removePreference(findPreference("pref_playlists_empty"));
            for (final PlaylistItem playlist : playlists) {
                final EditTextPreference et = new EditTextPreference(mActivity);
                et.setText(playlist.getName());
                et.setTitle(playlist.getName());
                et.setSummary(R.string.rename);
                category.addPreference(et);
            }
        }

        findPreference("pref_display_playlist_sort_order").setSummary(((ListPreference)findPreference("pref_display_playlist_sort_order")).getEntry());

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

         if (Objects.equals(key, "pref_hide_empty_playlists")) {
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("refresh_vp", true);
            editor.apply();
        }

        if (key.equals("pref_display_playlist_sort_order"))
            findPreference("pref_display_playlist_sort_order").setSummary(((ListPreference) findPreference("pref_display_playlist_sort_order")).getEntry());
    }
}
