package com.krisdb.wearcasts;


import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsPodcastsGeneralFragment extends PreferenceFragment {

   @Override
    public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);

       addPreferencesFromResource(R.xml.settings_podcasts_general);

   }
}
