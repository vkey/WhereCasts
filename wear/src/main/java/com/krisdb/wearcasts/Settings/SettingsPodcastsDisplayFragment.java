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

import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;

import java.util.List;
import java.util.Objects;

import androidx.core.content.ContextCompat;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.HasEpisodes;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;

public class SettingsPodcastsDisplayFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_podcasts_display);

        mActivity = getActivity();
        final Resources resources = getResources();

        final PreferenceCategory category = (PreferenceCategory)findPreference("display_settings");

        final List<PlaylistItem> playlists = getPlaylists(mActivity, false);
        int size = playlists.size();

        //third party: check if playlist has episodes
        final Boolean thirdPartyPlayerFM = HasEpisodes(mActivity, 0, resources.getInteger(R.integer.playlist_playerfm));

        int limit;

        if (thirdPartyPlayerFM)
            limit = size + 5;
        else
            limit = size + 4;

        final CharSequence entryValues[] = new String[limit];
        final CharSequence entryText[] = new String[limit];

        final List<PlaylistItem> playlistsArray = getPlaylists(mActivity);

        entryValues[0] = "0";
        entryText[0] = getString(R.string.settings_podcasts_home_screen_default);

        for(int p = 1; p <= size; p++) {
            entryValues[p] = String.valueOf(playlistsArray.get(p-1).getID());
            entryText[p] = playlists.get(p-1).getName();
        }

        entryValues[size + 1] = String.valueOf(resources.getInteger(R.integer.playlist_local));
        entryValues[size + 2] = String.valueOf(resources.getInteger(R.integer.playlist_inprogress));
        entryValues[size + 3] = String.valueOf(resources.getInteger(R.integer.playlist_downloads));
        if (thirdPartyPlayerFM) //third party: if third party playlist had episodes add it to drop down
            entryValues[size + 4] = String.valueOf(resources.getInteger(R.integer.playlist_playerfm));

        entryText[size + 1] = getString(R.string.settings_podcasts_home_screen_local);
        entryText[size + 2] = getString(R.string.settings_podcasts_home_screen_inprogress);
        entryText[size + 3] = getString(R.string.settings_podcasts_home_screen_downloads);
        if (thirdPartyPlayerFM) //third party: if third party playlist had episodes add it to drop down
            entryText[size + 4] = getString(R.string.third_party_title_playerfm);

        final ListPreference lpHomeScreen = new ListPreference(mActivity);
        lpHomeScreen.setTitle(R.string.settings_podcasts_label_home_screen);
        lpHomeScreen.setKey("pref_display_home_screen");
        lpHomeScreen.setEntryValues(entryValues);
        lpHomeScreen.setEntries(entryText);
        lpHomeScreen.setIcon(ContextCompat.getDrawable(mActivity, R.drawable.ic_setting_dropdown_indicator));
        lpHomeScreen.setDefaultValue("0");

        category.addPreference(lpHomeScreen);

        if (!Utilities.hasPremium(mActivity))
        {
            findPreference("pref_display_home_screen").setSummary(getString(R.string.premium_locked_playback_speed));
            findPreference("pref_display_home_screen").setEnabled(false);
            findPreference("pref_theme").setSummary(getString(R.string.premium_locked_playback_speed));
            findPreference("pref_theme").setEnabled(false);
            findPreference("pref_header_color").setSummary(getString(R.string.premium_locked_playback_speed));
            findPreference("pref_header_color").setEnabled(false);
        }
        else
        {
            findPreference("pref_display_home_screen").setSummary(((ListPreference) findPreference("pref_display_home_screen")).getEntry());
            findPreference("pref_theme").setSummary(((ListPreference) findPreference("pref_theme")).getEntry());
            findPreference("pref_header_color").setSummary(((ListPreference) findPreference("pref_header_color")).getEntry());
        }

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
        if (Objects.equals(key, "pref_theme")) {
            final Intent i = mActivity.getBaseContext().getPackageManager().getLaunchIntentForPackage(mActivity.getBaseContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
        else if (Objects.equals(key, "pref_hide_empty_playlists") || Objects.equals(key, "pref_paging_indicator")) {
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("refresh_vp", true);
            editor.apply();
        }

        if (Objects.equals(key, "pref_theme") == false)
        {
            final Boolean hasPremium = Utilities.hasPremium(mActivity);

            if (hasPremium)
            {
                if (key.equals("pref_display_home_screen"))
                    findPreference("pref_display_home_screen").setSummary(((ListPreference) findPreference("pref_display_home_screen")).getEntry());

                if (key.equals("pref_theme"))
                    findPreference("pref_theme").setSummary(((ListPreference) findPreference("pref_theme")).getEntry());

                if (key.equals("pref_header_color"))
                    findPreference("pref_header_color").setSummary(((ListPreference) findPreference("pref_header_color")).getEntry());
            }
        }
    }
}
