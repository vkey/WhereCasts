package com.krisdb.wearcasts.Settings;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.krisdb.wearcasts.Activities.BasePreferenceActivity;

public class SettingsPodcastsUpdatesActivity extends BasePreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();

        final SettingsPodcastsUpdatesFragment pf = new SettingsPodcastsUpdatesFragment();
        ft.replace(android.R.id.content, pf);
        ft.commit();
    }
}
