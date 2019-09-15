package com.krisdb.wearcasts.Services;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.session.MediaButtonReceiver;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Processor;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodesWithDownloads;
import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcasts;

public class SleepTimer extends Worker {

    private static WeakReference<Context> mContext;

    public SleepTimer(@NonNull Context context, @NonNull WorkerParameters params) {
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
        Log.d(ctx.getPackageName(), "Audio stopped 1");

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                MediaSessionCompat mediaSessionCompat = new MediaSessionCompat(ctx, MediaPlayerService.class.getSimpleName());
                mediaSessionCompat.getController().getTransportControls().pause();
            }
        });


        Log.d(ctx.getPackageName(), "Audio stopped 2");


        //ctx.stopService(new Intent(ctx, MediaPlayerService.class));

        return Result.success();
    }

}
