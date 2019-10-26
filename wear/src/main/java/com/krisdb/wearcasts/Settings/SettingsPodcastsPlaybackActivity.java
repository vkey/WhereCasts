package com.krisdb.wearcasts.Settings;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.krisdb.wearcasts.Activities.BasePreferenceActivity;

public class SettingsPodcastsPlaybackActivity extends BasePreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();

        final SettingsPodcastsPlaybackFragment pf = new SettingsPodcastsPlaybackFragment();
        ft.replace(android.R.id.content, pf);
        ft.commit();

        //final android.database.sqlite.SQLiteDatabase sdb1 = com.krisdb.wearcasts.Databases.DatabaseHelper.select(this);
        //sdb1.execSQL("DELETE FROM [tbl_podcast_episodes] WHERE [id] IN (SELECT [id] FROM [tbl_podcast_episodes] WHERE [pid] = 1 ORDER BY [pubDate] DESC LIMIT 2)");

        //final android.database.sqlite.SQLiteDatabase sdb2 = com.krisdb.wearcasts.Databases.DatabaseHelper.select(this);
        //sdb2.execSQL("DELETE FROM [tbl_podcast_episodes] WHERE [id] IN (SELECT [id] FROM [tbl_podcast_episodes] WHERE [pid] = 2 ORDER BY [pubDate] DESC LIMIT 2)");
    }
}
