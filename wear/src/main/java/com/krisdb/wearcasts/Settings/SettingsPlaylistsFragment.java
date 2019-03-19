package com.krisdb.wearcasts.Settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;

import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Fragments.BasePreferenceFragmentCompat;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.CommonUtils;

import java.util.List;
import java.util.Objects;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;

public class SettingsPlaylistsFragment extends BasePreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_playlists);

        final PreferenceCategory category = findPreference("playlist_settings");
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
                et.setKey("prefs_playlists_" + playlist.getID());
                et.setText(playlist.getName());
                et.setTitle(playlist.getName());
                et.setSummary(R.string.rename);
                et.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String text = newValue.toString();

                        if (text.length() == 0) {
                            CommonUtils.showToast(mActivity, mActivity.getString(R.string.validation_podcast_rename_title));
                            return true;
                        }

                        if (text.length() > 12) {
                            CommonUtils.showToast(mActivity, getString(R.string.validation_playlist_rename_length));
                            return true;
                        }

                        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);

                        db.updatePlaylist(text, playlist.getID());
                        db.close();

                        et.setText(text);
                        et.setTitle(text);
                        return true;
                    }
                });

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
