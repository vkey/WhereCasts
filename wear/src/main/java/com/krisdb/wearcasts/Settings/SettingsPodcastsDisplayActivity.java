package com.krisdb.wearcasts.Settings;

import android.os.Bundle;

import com.krisdb.wearcasts.Activities.BasePreferenceActivity;

public class SettingsPodcastsDisplayActivity extends BasePreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsPodcastsDisplayFragment()).commit();

    }
}
