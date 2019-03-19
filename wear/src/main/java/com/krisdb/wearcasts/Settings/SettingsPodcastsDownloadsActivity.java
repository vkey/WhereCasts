package com.krisdb.wearcasts.Settings;


import android.os.Bundle;

import com.krisdb.wearcasts.Activities.BasePreferenceActivity;

public class SettingsPodcastsDownloadsActivity extends BasePreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

<<<<<<< HEAD
<<<<<<< HEAD
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsPodcastsDownloadsFragment()).commit();
=======
        final FragmentManager fm = getFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();

        final SettingsPodcastsDownloadsFragment pf = new SettingsPodcastsDownloadsFragment();
        ft.replace(android.R.id.content, pf);
        ft.commit();
>>>>>>> parent of 638f5a8... preferences update
=======
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsPodcastsDownloadsFragment()).commit();

>>>>>>> parent of 16d73e0... revet
    }
}
