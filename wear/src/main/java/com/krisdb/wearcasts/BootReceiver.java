package com.krisdb.wearcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(context.getPackageName(), "Boot received");
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("updatesEnabled", true)) {
                Log.d(context.getPackageName(), "Job started");
                Utilities.StartJob(context);
            }
        }
    }
}
