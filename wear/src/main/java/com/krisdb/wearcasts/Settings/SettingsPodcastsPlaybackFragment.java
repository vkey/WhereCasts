package com.krisdb.wearcasts.Settings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.wearable.input.WearableButtons;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;

public class SettingsPodcastsPlaybackFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_podcasts_playback);

        mActivity = getActivity();

        final int buttonCount = WearableButtons.getButtonCount(mActivity);

        if (!Utilities.hasPremium(mActivity))
        {
            findPreference("pref_playback_speed").setSummary(getString(R.string.premium_locked_playback_speed));
            findPreference("pref_playback_speed").setEnabled(false);
            findPreference("pref_playback_skip_forward").setSummary(getString(R.string.premium_locked_playback_speed));
            findPreference("pref_playback_skip_forward").setEnabled(false);
            findPreference("pref_playback_skip_back").setSummary(getString(R.string.premium_locked_playback_speed));
            findPreference("pref_playback_skip_back").setEnabled(false);
            if (buttonCount == 1 || buttonCount == 3) {
                findPreference("pref_hardware_override_episode").setTitle(getString(R.string.settings_podcasts_label_hardware_buttons));
                findPreference("pref_hardware_override_episode").setSummary(getString(R.string.premium_locked_playback_speed));
                findPreference("pref_hardware_override_episode").setEnabled(false);
            }
            else
            {
                final PreferenceCategory category = (PreferenceCategory) findPreference("pref_general");
                category.removePreference(findPreference("pref_hardware_override_episode"));
            }
        }
        else
        {
            if (buttonCount == 1) {
                findPreference("pref_hardware_override_episode").setSummary(getString(R.string.settings_podcasts_label_hardware_buttons_1_summary));
                findPreference("pref_hardware_override_episode").setTitle(getString(R.string.settings_podcasts_label_hardware_button));
            }
            else if (buttonCount == 3) {
                findPreference("pref_hardware_override_episode").setSummary(getString(R.string.settings_podcasts_label_hardware_buttons_3_summary));
                findPreference("pref_hardware_override_episode").setTitle(getString(R.string.settings_podcasts_label_hardware_buttons));
            }
            else {
                final PreferenceCategory category = (PreferenceCategory) findPreference("pref_general");
                category.removePreference(findPreference("pref_hardware_override_episode"));
            }
            findPreference("pref_playback_skip_forward").setSummary(((ListPreference)findPreference("pref_playback_skip_forward")).getEntry());
            findPreference("pref_playback_skip_back").setSummary(((ListPreference)findPreference("pref_playback_skip_back")).getEntry());
            findPreference("pref_playback_speed").setSummary(((ListPreference)findPreference("pref_playback_speed")).getEntry());
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

        if (key.equals("pref_playback_speed") && Utilities.hasPremium(mActivity)) {
            findPreference("pref_playback_speed").setSummary(((ListPreference) findPreference("pref_playback_speed")).getEntry());

            final PutDataMapRequest dataMap = PutDataMapRequest.create("/syncplaybackspeed");

            dataMap.getDataMap().putFloat("playback_speed", Float.parseFloat(((ListPreference)findPreference("pref_playback_speed")).getEntry().toString()));

            CommonUtils.DeviceSync(mActivity, dataMap);
        }

        if (key.equals("pref_playback_skip_forward"))
            findPreference("pref_playback_skip_forward").setSummary(((ListPreference)findPreference("pref_playback_skip_forward")).getEntry());

        if (key.equals("pref_playback_skip_back"))
            findPreference("pref_playback_skip_back").setSummary(((ListPreference)findPreference("pref_playback_skip_back")).getEntry());
    }
}
