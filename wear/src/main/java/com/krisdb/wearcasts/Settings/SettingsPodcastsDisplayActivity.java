package com.krisdb.wearcasts.Settings;


<<<<<<< HEAD
=======
import android.app.FragmentManager;
import android.app.FragmentTransaction;
>>>>>>> parent of 638f5a8... preferences update
import android.os.Bundle;

import com.krisdb.wearcasts.Activities.BasePreferenceActivity;

public class SettingsPodcastsDisplayActivity extends BasePreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();

        final SettingsPodcastsDisplayFragment pf = new SettingsPodcastsDisplayFragment();
        ft.replace(android.R.id.content, pf);
        ft.commit();
    }
}
