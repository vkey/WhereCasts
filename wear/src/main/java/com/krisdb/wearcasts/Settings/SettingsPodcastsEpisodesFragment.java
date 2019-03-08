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

import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;

public class SettingsPodcastsEpisodesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_podcasts_episodes);

        mActivity = getActivity();

        findPreference("pref_episode_limit").setSummary(((ListPreference) findPreference("pref_episode_limit")).getEntry());
        findPreference("pref_display_episodes_sort_order").setSummary(((ListPreference)findPreference("pref_display_episodes_sort_order")).getEntry());

        final PreferenceCategory category = (PreferenceCategory)findPreference("display_episodes");

        final List<PlaylistItem> playlists = getPlaylists(mActivity, false);
        int size = playlists.size();

        final CharSequence entryValues[] = new String[size + 2];
        final CharSequence entryText[] = new String[size + 2];

        entryValues[0] = "0";
        entryText[0] = "Toggle played/unplayed";

        entryValues[1] = "-1";
        entryText[1] = "Download";

        for(int p = 2; p <= size+1; p++) {
            entryValues[p] = String.valueOf(playlists.get(p-2).getID());
            entryText[p] = "Add to ".concat(playlists.get(p-2).getName()); //TODO: Localize
        }

        final ListPreference lpSwipeAction = new ListPreference(mActivity);
        lpSwipeAction.setTitle(R.string.settings_podcasts_label_episodes_swipe_action);
        lpSwipeAction.setSummary(R.string.settings_podcasts_label_episodes_swipe_action_summary);
        lpSwipeAction.setKey("pref_episodes_swipe_action");
        lpSwipeAction.setEntryValues(entryValues);
        lpSwipeAction.setEntries(entryText);
        lpSwipeAction.setIcon(ContextCompat.getDrawable(mActivity, R.drawable.ic_setting_dropdown_indicator));
        lpSwipeAction.setDefaultValue("0");

        category.addPreference(lpSwipeAction);

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

        if (key.equals("pref_episode_limit"))
            findPreference("pref_episode_limit").setSummary(((ListPreference) findPreference("pref_episode_limit")).getEntry());


        if (key.equals("pref_display_episodes_sort_order"))
            findPreference("pref_display_episodes_sort_order").setSummary(((ListPreference) findPreference("pref_display_episodes_sort_order")).getEntry());
    }
}
