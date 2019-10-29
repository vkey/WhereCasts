package com.krisdb.wearcasts.Settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.krisdb.wearcasts.Activities.BaseFragmentActivity;
import com.krisdb.wearcasts.R;

public class SettingsContextActivity extends BaseFragmentActivity {
    private String mKey;
    private SharedPreferences.Editor mEditor;
    private EditText mStartTimeOther;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_context_activity);

        mStartTimeOther = findViewById(R.id.settings_context_skip_start_time);

        ((TextView)findViewById(R.id.settings_context_skip_start_time_label)).setText(getString(R.string.other).concat(": "));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = prefs.edit();

        mKey = getIntent().getExtras().getString("key");
        final String defaultValue = getIntent().getExtras().getString("default") != null ? getIntent().getExtras().getString("default") : "0";

        final String value = prefs.getString(mKey, defaultValue);

        if (value != null) {
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
        }

        mStartTimeOther.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

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
