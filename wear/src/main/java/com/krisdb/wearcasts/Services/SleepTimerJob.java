package com.krisdb.wearcasts.Services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.lang.ref.WeakReference;

public class SleepTimerJob extends Worker {

    private static WeakReference<Context> mContext;

    public SleepTimerJob(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        mContext = new WeakReference<>(context);
    }

    @NonNull
    @Override
    public Result doWork() {

        final Context ctx = mContext.get();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("sleep_timer_running", false);
        editor.apply();

        final Intent intentSleepTimer = new Intent();
        intentSleepTimer.setAction("sleep_timer");
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(intentSleepTimer);

        return Result.success();
    }
}
