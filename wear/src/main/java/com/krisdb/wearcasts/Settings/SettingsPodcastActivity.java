package com.krisdb.wearcasts.Settings;


import android.os.Bundle;

import com.krisdb.wearcasts.Activities.BasePreferenceActivity;

public class SettingsPodcastActivity extends BasePreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int podcastId = getIntent().getExtras().getInt("podcastId");

        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, SettingsPodcastFragment.newInstance(podcastId)).commit();
    }
}
