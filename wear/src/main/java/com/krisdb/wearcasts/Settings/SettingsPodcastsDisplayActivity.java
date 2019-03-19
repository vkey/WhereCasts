package com.krisdb.wearcasts.Settings;

<<<<<<< HEAD

<<<<<<< HEAD
=======
import android.app.FragmentManager;
import android.app.FragmentTransaction;
>>>>>>> parent of 638f5a8... preferences update
=======
>>>>>>> parent of 16d73e0... revet
import android.os.Bundle;

import com.krisdb.wearcasts.Activities.BasePreferenceActivity;

public class SettingsPodcastsDisplayActivity extends BasePreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsPodcastsDisplayFragment()).commit();

    }
}
