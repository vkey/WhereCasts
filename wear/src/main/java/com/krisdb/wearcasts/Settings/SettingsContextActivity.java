package com.krisdb.wearcasts.Settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.krisdb.wearcasts.Activities.BaseFragmentActivity;
import com.krisdb.wearcasts.Adapters.PlaylistsAssignAdapter;
import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;

import java.util.List;

import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.playlistIsEmpty;

public class SettingsContextActivity extends BaseFragmentActivity {
    private String mKey;
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;
    private EditText mStartTimeOther;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_context_activity);

        mStartTimeOther = findViewById(R.id.settings_context_skip_start_time);

        ((TextView)findViewById(R.id.settings_context_skip_start_time_label)).setText(getString(R.string.other).concat(": "));

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mPrefs.edit();

        mKey = getIntent().getExtras().getString("key");
        final String defaultValue = getIntent().getExtras().getString("default") != null ? getIntent().getExtras().getString("default") : "0";

        String value = mPrefs.getString(mKey, defaultValue);

        switch (value) {
            case "0":
                ((RadioButton) findViewById(R.id.settings_context_skip_start_0)).setChecked(true);
                break;
            case "10":
                ((RadioButton) findViewById(R.id.settings_context_skip_start_10)).setChecked(true);
                break;
            case "15":
                ((RadioButton) findViewById(R.id.settings_context_skip_start_15)).setChecked(true);
                break;
            case "30":
                ((RadioButton) findViewById(R.id.settings_context_skip_start_30)).setChecked(true);
                break;
            case "60":
                ((RadioButton) findViewById(R.id.settings_context_skip_start_60)).setChecked(true);
                break;
            default:
                mStartTimeOther.setText(value);
                break;
        }

        mStartTimeOther.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0 && !s.equals("")) {
                    ((RadioButton) findViewById(R.id.settings_context_skip_start_0)).setChecked(false);
                    ((RadioButton) findViewById(R.id.settings_context_skip_start_10)).setChecked(false);
                    ((RadioButton) findViewById(R.id.settings_context_skip_start_15)).setChecked(false);
                    ((RadioButton) findViewById(R.id.settings_context_skip_start_30)).setChecked(false);
                    ((RadioButton) findViewById(R.id.settings_context_skip_start_60)).setChecked(false);

                    mEditor.putString(mKey, s.toString());
                    mEditor.apply();
                }
            }
        });


        findViewById(R.id.settings_context_button_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    public void onRadioButtonClicked(View view) {

        boolean checked = ((RadioButton) view).isChecked();

        mStartTimeOther.getText().clear();

        switch(view.getId()) {
            case R.id.settings_context_skip_start_0:
                if (checked)
                mEditor.putString(mKey, "0");
                break;
            case R.id.settings_context_skip_start_10:
                if (checked)
                mEditor.putString(mKey, "10");
                break;
            case R.id.settings_context_skip_start_15:
                if (checked)
                mEditor.putString(mKey, "15");
                break;
            case R.id.settings_context_skip_start_30:
                if (checked)
                mEditor.putString(mKey, "30");
                break;
            case R.id.settings_context_skip_start_60:
                if (checked)
                mEditor.putString(mKey, "60");
                break;

        }

        mEditor.apply();
        finish();
    }
}
