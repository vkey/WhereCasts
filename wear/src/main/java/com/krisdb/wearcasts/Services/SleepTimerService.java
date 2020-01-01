package com.krisdb.wearcasts.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.krisdb.wearcasts.Activities.MainActivity;
import com.krisdb.wearcasts.R;

import java.lang.ref.WeakReference;

public class SleepTimerService extends IntentService {

    private CountDownTimer timer;
    private MediaBrowserCompat mMediaBrowser;
    private Context mContext;


    public SleepTimerService() {
        super("SleepTimerService");
    }

    public SleepTimerService(String name) {
        super(name);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mContext = new WeakReference<>(this).get();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final int timerMinutes = Integer.valueOf(prefs.getString("pref_sleep_timer", "0"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            final String channelID = getPackageName().concat(".sleep.timer");

            final NotificationManager notificationManager = getSystemService(NotificationManager.class);
            final NotificationChannel channel = new NotificationChannel(channelID, getString(R.string.notification_channel_sleep_timer), NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);

            final Intent notificationIntent = new Intent(mContext, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

            final Notification notification = new NotificationCompat.Builder(mContext, channelID)
                    .setContentTitle(getString(R.string.notification_channel_sleep_timer))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(pendingIntent)
                    .build();

            startForeground(111, notification);
        }

        timer = new CountDownTimer(timerMinutes * 60000, 1000) {

            public void onTick(long millisUntilFinished) { }

            public void onFinish() {
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("sleep_timer_running", false);
                editor.apply();

                mMediaBrowser = new MediaBrowserCompat(mContext,
                        new ComponentName(mContext, MediaPlayerService.class),
                        connectionCallbacks,
                        null);

                mMediaBrowser.connect();

                //final Intent intentSleepTimer = new Intent();
                //intentSleepTimer.setAction("sleep_timer");
                //LocalBroadcastManager.getInstance(mContext).sendBroadcast(intentSleepTimer);

                stopSelf();
            }

        }.start();

        return START_STICKY;
    }

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    try {
                        final MediaControllerCompat mediaController = new MediaControllerCompat(mContext, mMediaBrowser.getSessionToken());
                        mediaController.getTransportControls().pause();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConnectionSuspended() { }

                @Override
                public void onConnectionFailed() { }
            };


    @Override
    public void onDestroy() {
        if (timer != null)
            timer.cancel();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {}
}
