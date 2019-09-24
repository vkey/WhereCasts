package com.krisdb.wearcasts.Settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.wearable.input.WearableButtons;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

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

            final EditTextPreference etSkipBack = (EditTextPreference)findPreference("pref_playback_skip_back");
            etSkipBack.getEditText().setImeOptions(EditorInfo.IME_ACTION_DONE);

            etSkipBack.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE){
                        etSkipBack.setText(v.getText().toString());
                        etSkipBack.setSummary(!etSkipBack.getText().equals("0") ? v.getText().toString().concat(" ").concat(getString(R.string.seconds).toLowerCase()) : "");
                        etSkipBack.onClick(etSkipBack.getDialog(), Dialog.BUTTON_POSITIVE);
                        etSkipBack.getDialog().dismiss();
                        return true;
                    }
                    return false;
                }
            });

            final EditTextPreference etSkipForward = (EditTextPreference)findPreference("pref_playback_skip_forward");
            etSkipForward.getEditText().setImeOptions(EditorInfo.IME_ACTION_DONE);

            etSkipForward.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE){
                        etSkipForward.setText(v.getText().toString());
                        etSkipForward.setSummary(!etSkipForward.getText().equals("0") ? v.getText().toString().concat(" ").concat(getString(R.string.seconds).toLowerCase()) : "");
                        etSkipForward.onClick(etSkipForward.getDialog(), Dialog.BUTTON_POSITIVE);
                        etSkipForward.getDialog().dismiss();
                        return true;
                    }
                    return false;
                }
            });

            etSkipBack.setText(etSkipBack.getText());

            if (!etSkipBack.getText().equals("0"))
                etSkipBack.setSummary(etSkipBack.getText().concat(" ").concat(getString(R.string.seconds).toLowerCase()));

            etSkipForward.setText(etSkipForward.getText());

            if (!etSkipForward.getText().equals("0"))
                etSkipForward.setSummary(etSkipForward.getText().concat(" ").concat(getString(R.string.seconds).toLowerCase()));

            //findPreference("pref_playback_skip_forward").setSummary(((ListPreference)findPreference("pref_playback_skip_forward")).getEntry());
            //findPreference("pref_playback_skip_back").setSummary(((ListPreference)findPreference("pref_playback_skip_back")).getEntry());
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
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        SystemClock.sleep(500);

        if (key.equals("pref_playback_speed") && Utilities.hasPremium(mActivity)) {
            findPreference("pref_playback_speed").setSummary(((ListPreference) findPreference("pref_playback_speed")).getEntry());

            final PutDataMapRequest dataMap = PutDataMapRequest.create("/syncplaybackspeed");

            dataMap.getDataMap().putFloat("playback_speed", Float.parseFloat(((ListPreference)findPreference("pref_playback_speed")).getValue()));

            CommonUtils.DeviceSync(mActivity, dataMap);
        }

        //if (key.equals("pref_playback_skip_forward"))
        //findPreference("pref_playback_skip_forward").setSummary(((ListPreference)findPreference("pref_playback_skip_forward")).getEntry());

        //if (key.equals("pref_playback_skip_back"))
            //findPreference("pref_playback_skip_back").setSummary(((ListPreference)findPreference("pref_playback_skip_back")).getEntry());

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
