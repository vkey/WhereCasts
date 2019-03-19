package com.krisdb.wearcasts.Settings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
<<<<<<< HEAD
<<<<<<< HEAD
=======
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
>>>>>>> parent of 638f5a8... preferences update
=======
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
>>>>>>> parent of 16d73e0... revet

import com.krisdb.wearcasts.Fragments.BasePreferenceFragmentCompat;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;

import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;

<<<<<<< HEAD
<<<<<<< HEAD
public class SettingsPodcastsEpisodesFragment extends BasePreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
=======
public class SettingsPodcastsEpisodesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
>>>>>>> parent of 638f5a8... preferences update
=======
public class SettingsPodcastsEpisodesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
>>>>>>> parent of 16d73e0... revet

    //private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

<<<<<<< HEAD
<<<<<<< HEAD
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

=======
>>>>>>> parent of 638f5a8... preferences update
=======
      }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
>>>>>>> parent of 16d73e0... revet
        addPreferencesFromResource(R.xml.settings_podcasts_episodes);

        final Activity mActivity = getActivity();

        findPreference("pref_episode_limit").setSummary(((ListPreference) findPreference("pref_episode_limit")).getEntry());
        findPreference("pref_display_episodes_sort_order").setSummary(((ListPreference)findPreference("pref_display_episodes_sort_order")).getEntry());

        final PreferenceCategory category = (PreferenceCategory)findPreference("display_episodes");

        final List<PlaylistItem> playlists = getPlaylists(mActivity, false);
        final int size = playlists.size();

        final CharSequence entryValues[] = new String[size + 2];
        final CharSequence entryText[] = new String[size + 2];

        entryValues[0] = "0";
        entryText[0] = getString(R.string.settings_podcasts_episodes_swipe_action_option_toggle);

        entryValues[1] = "-1";
        entryText[1] = getString(R.string.settings_podcasts_episodes_swipe_action_option_download);

        for(int p = 2; p <= size+1; p++) {
            entryValues[p] = String.valueOf(playlists.get(p-2).getID());
            entryText[p] = getString(R.string.settings_podcasts_episodes_swipe_action_option_playlist, playlists.get(p-2).getName());
        }

        final ListPreference lpSwipeAction = new ListPreference(mActivity);
        lpSwipeAction.setTitle(R.string.settings_podcasts_label_episodes_swipe_action);
        lpSwipeAction.setKey("pref_episodes_swipe_action");
        lpSwipeAction.setEntryValues(entryValues);
        lpSwipeAction.setEntries(entryText);
        lpSwipeAction.setIcon(ContextCompat.getDrawable(mActivity, R.drawable.ic_setting_dropdown_indicator));
        lpSwipeAction.setDefaultValue("0");

        category.addPreference(lpSwipeAction);

        if (!Utilities.hasPremium(mActivity))
        {
            findPreference("pref_episodes_swipe_action").setSummary(getString(R.string.premium_locked_playback_speed));
            findPreference("pref_episodes_swipe_action").setEnabled(false);
            findPreference("pref_episodes_downloads_first").setEnabled(false);
            findPreference("pref_episodes_downloads_first").setSummary(getString(R.string.premium_locked_playback_speed));
        }
        else
        {
            findPreference("pref_episodes_swipe_action").setSummary(((ListPreference) findPreference("pref_episodes_swipe_action")).getEntry());
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

        if (key.equals("pref_episode_limit"))
            findPreference("pref_episode_limit").setSummary(((ListPreference) findPreference("pref_episode_limit")).getEntry());

        if (key.equals("pref_episodes_swipe_action"))
            findPreference("pref_episodes_swipe_action").setSummary(((ListPreference) findPreference("pref_episodes_swipe_action")).getEntry());

        if (key.equals("pref_display_episodes_sort_order"))
            findPreference("pref_display_episodes_sort_order").setSummary(((ListPreference) findPreference("pref_display_episodes_sort_order")).getEntry());
    }
}
