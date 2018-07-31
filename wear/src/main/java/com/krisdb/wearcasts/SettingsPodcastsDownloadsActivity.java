package com.krisdb.wearcasts;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

public class SettingsPodcastsDownloadsActivity extends BasePreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();

        final SettingsPodcastsDownloadsFragment pf = new SettingsPodcastsDownloadsFragment();
        ft.replace(android.R.id.content, pf);
        ft.commit();
    }
}
