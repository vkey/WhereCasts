package com.krisdb.wearcasts.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getInt("new_episode_count", 0) > 0) {
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("new_episode_count", 0);
            editor.apply();
        }
    }
}
