package com.krisdb.wearcasts.Settings;


import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.krisdb.wearcasts.R;

public class SettingsPodcastsGeneralFragment extends PreferenceFragment {

   @Override
    public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);

       addPreferencesFromResource(R.xml.settings_podcasts_general);

   }
}
