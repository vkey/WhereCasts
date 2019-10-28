package com.krisdb.wearcasts.Settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.android.material.textfield.TextInputLayout;
import com.krisdb.wearcasts.Activities.BaseFragmentActivity;
import com.krisdb.wearcasts.Adapters.PlaylistsAssignAdapter;
import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.Interfaces;

import java.util.List;

import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.playlistIsEmpty;

public class SettingsContextActivity extends BaseFragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_context_activity);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final String key = getIntent().getExtras().getString("key");
        String value = prefs.getString(key, null);

        if (key.contains("_skip_start_time")) {

            EditText editText = findViewById(R.id.settings_context_skip_start_time);
            editText.setVisibility(View.VISIBLE);
            findViewById(R.id.settings_context_group_skip_start).setVisibility(View.VISIBLE);

            if (value != null)
                editText.setHint(value);

            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(key, s.toString());
                    editor.apply();
                }
            });

        }
    }

    public void onRadioButtonClicked(View view) {

        boolean checked = ((RadioButton) view).isChecked();

        switch(view.getId()) {
            case R.id.settings_context_skip_start_10:
                if (checked)
                    // Pirates are the best
                    break;
            case R.id.settings_context_skip_start_30:
                if (checked)
                    // Ninjas rule
                    break;
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {

            }
        }
    }

}
