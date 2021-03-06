package com.krisdb.wearcasts.Settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;

import java.util.List;
import java.util.Objects;

import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;

public class SettingsPlaylistsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                et.getEditText().setImeOptions(EditorInfo.IME_ACTION_DONE);

                et.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            final String text = v.getText().toString();

                            if (text.length() == 0) {
                                Utilities.ShowFailureActivity(mActivity, mActivity.getString(R.string.validation_podcast_rename_title));
                                //CommonUtils.showToast(mActivity, mActivity.getString(R.string.validation_podcast_rename_title));
                                return true;
                            }

                            if (text.length() > 20) {
                                Utilities.ShowFailureActivity(mActivity, getString(R.string.validation_podcast_rename_length));
                                //CommonUtils.showToast(mActivity, getString(R.string.validation_podcast_rename_length));
                                return true;
                            }

                            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);

                            db.updatePlaylist(text, playlist.getID());
                            db.close();

                            et.setText(text);
                            et.setTitle(text);
                            et.onClick(et.getDialog(), Dialog.BUTTON_POSITIVE);
                            et.getDialog().dismiss();
                            return true;
                        }
                        return false;
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
        if (Objects.equals(key, "pref_hide_empty_playlists")) {
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("refresh_vp", true);
            editor.apply();
        }

        if (key.equals("pref_display_playlist_sort_order"))
            findPreference("pref_display_playlist_sort_order").setSummary(((ListPreference) findPreference("pref_display_playlist_sort_order")).getEntry());
    }
}
