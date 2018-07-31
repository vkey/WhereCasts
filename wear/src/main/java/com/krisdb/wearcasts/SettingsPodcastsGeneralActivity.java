package com.krisdb.wearcasts;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

public class SettingsPodcastsGeneralActivity extends BasePreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();

        final SettingsPodcastsGeneralFragment pf = new SettingsPodcastsGeneralFragment();
        ft.replace(android.R.id.content, pf);
        ft.commit();
    }
}
