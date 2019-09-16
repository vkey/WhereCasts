package com.krisdb.wearcasts.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.krisdb.wearcasts.Activities.MainActivity;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.CommonUtils;

import java.lang.ref.WeakReference;

public class SleepTimerService extends IntentService {

    public SleepTimerService() {
        super("SleepTimerService");
    }

    public SleepTimerService(String name) {
        super(name);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final Context ctx = new WeakReference<>(this).get();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final int timerMinutes = Integer.valueOf(prefs.getString("pref_sleep_timer", "0"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            final String channelID = getPackageName().concat(".sleep.timer");

            final NotificationManager notificationManager = getSystemService(NotificationManager.class);
            final NotificationChannel channel = new NotificationChannel(channelID, getString(R.string.notification_channel_sleep_timer), NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);

            final Intent notificationIntent = new Intent(ctx, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, 0);

            final Notification notification = new NotificationCompat.Builder(ctx, channelID)
                    .setContentTitle(getString(R.string.notification_channel_sleep_timer))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(pendingIntent)
                    .build();

            startForeground(111, notification);
        }

        new CountDownTimer(timerMinutes * 60000, 1000) {

            public void onTick(long millisUntilFinished) {
                //CommonUtils.showToast(ctx, "" + millisUntilFinished/1000);
            }

            public void onFinish() {
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("sleep_timer_running", false);
                editor.apply();

                final Intent intentSleepTimer = new Intent();
                intentSleepTimer.setAction("sleep_timer");
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(intentSleepTimer);

                stopSelf();
            }

        }.start();

        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {}
}
