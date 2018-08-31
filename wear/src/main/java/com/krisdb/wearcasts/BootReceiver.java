package com.krisdb.wearcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("updatesEnabled", true)) {
               Utilities.StartJob(context);
            }
        }
    }
}
