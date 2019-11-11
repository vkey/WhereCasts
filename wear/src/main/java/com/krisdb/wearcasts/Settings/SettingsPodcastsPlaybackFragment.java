package com.krisdb.wearcasts.Settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.wearable.input.WearableButtons;
import android.widget.Toast;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Services.MediaPlayerService;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

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
            findPreference("pref_sleep_timer").setSummary(getString(R.string.premium_locked_playback_speed));
            findPreference("pref_sleep_timer").setEnabled(false);
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

            final Preference pfSkipBack = findPreference("pref_playback_skip_back");
            pfSkipBack.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final Intent intent = new Intent(mActivity, SettingsContextActivity.class);
                    final Bundle bundle = new Bundle();
                    bundle.putString("key", "pref_playback_skip_back");
                    bundle.putString("default", "30");
                    intent.putExtras(bundle);

                    startActivityForResult(intent, 1);
                    return false;
                }
            });

            final Preference pfSkipForward = findPreference("pref_playback_skip_forward");
            pfSkipForward.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final Intent intent = new Intent(mActivity, SettingsContextActivity.class);
                    final Bundle bundle = new Bundle();
                    bundle.putString("key", "pref_playback_skip_forward");
                    bundle.putString("default", "30");
                    intent.putExtras(bundle);

                    startActivityForResult(intent, 1);
                    return false;
                }
            });

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

            final int skipBack = Integer.valueOf(prefs.getString("pref_playback_skip_back", "30"));

            if (skipBack > 0)
                pfSkipBack.setSummary(String.valueOf(skipBack).concat(" ").concat(getString(R.string.seconds).toLowerCase()));

            final int skipForward = Integer.valueOf(prefs.getString("pref_playback_skip_forward", "30"));

            if (skipForward > 0)
                pfSkipForward.setSummary(String.valueOf(skipForward).concat(" ").concat(getString(R.string.seconds).toLowerCase()));

            findPreference("pref_sleep_timer").setSummary(((ListPreference)findPreference("pref_sleep_timer")).getEntry());
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK || resultCode == RESULT_CANCELED) {
            if (requestCode == 1)
            {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

                final int skipBack = Integer.valueOf(prefs.getString("pref_playback_skip_back", "30"));
                findPreference("pref_playback_skip_back").setSummary(skipBack > 0 ? String.valueOf(skipBack).concat(" ").concat(getString(R.string.seconds).toLowerCase()) : "");

                final int skipForward = Integer.valueOf(prefs.getString("pref_playback_skip_forward", "30"));
                findPreference("pref_playback_skip_forward").setSummary(skipForward > 0 ? String.valueOf(skipForward).concat(" ").concat(getString(R.string.seconds).toLowerCase()) : "");
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        SystemClock.sleep(500);

        if (key.equals("pref_playback_speed") && Utilities.hasPremium(mActivity)) {
            findPreference("pref_playback_speed").setSummary(((ListPreference) findPreference("pref_playback_speed")).getEntry());

            final PutDataMapRequest dataMap = PutDataMapRequest.create("/syncplaybackspeed");

            dataMap.getDataMap().putFloat("playback_speed", Float.parseFloat(((ListPreference)findPreference("pref_playback_speed")).getValue()));

            CommonUtils.DeviceSync(mActivity, dataMap);
        }

        if (key.equals("pref_sleep_timer")) {

            if (sharedPreferences.getBoolean("sleep_timer_first_set", true))
            {
                CommonUtils.showToast(mActivity, getString(R.string.sleep_timer_first), Toast.LENGTH_LONG);
                final SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("sleep_timer_first_set", false);
                editor.apply();
            }

            findPreference("pref_sleep_timer").setSummary(((ListPreference) findPreference("pref_sleep_timer")).getEntry());
        }
    }
}
