package com.krisdb.wearcasts.Services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
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
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.krisdb.wearcasts.Activities.MainActivity;
import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.CacheUtils;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcasts;

public class BackgroundService extends JobService {
    boolean isWorking = false;
    boolean jobCancelled = false;
    private static List<PodcastItem> mDownloadEpisodes;
    private static WeakReference<Context> mContext;
    private LocalBroadcastManager mBroadcastManger;
    private ConnectivityManager mManager;
    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private static TimeOutHandler mTimeOutHandler;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);

    public BackgroundService() {
    }

    @Override
    public void onCreate() {
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        isWorking = true;
        mContext = new WeakReference<>(getApplicationContext());
        mBroadcastManger = LocalBroadcastManager.getInstance(mContext.get());
        mTimeOutHandler = new TimeOutHandler(this);

        try {
            mBroadcastManger.registerReceiver(mDownloadsComplete, new IntentFilter("downloads_complete"));
            }
        catch (Exception ignored) {}
        doWork(params);

        return isWorking;
    }

    // Called if the job was cancelled before being finished
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        jobCancelled = true;
        boolean needsReschedule = isWorking;
        jobFinished(jobParameters, needsReschedule);
        return needsReschedule;
    }

    private void doWork(JobParameters jobParameters) {
        final Context ctx = mContext.get();

        final List<PodcastItem> podcasts = GetPodcasts(ctx);

        if (podcasts.size() > 0) {
            new AsyncTasks.SyncPodcasts(this, 0, true,
                    new Interfaces.BackgroundSyncResponse() {
                        @Override
                        public void processFinish(final int newEpisodeCount, final int downloadCount, final List<PodcastItem> downloadEpisodes) {
                            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
                            final SharedPreferences.Editor editor = prefs.edit();

                            if (newEpisodeCount > 0) {
                                //used to track failed download attempts
                                final int episodeCount = prefs.getInt("new_episode_count", 0) + newEpisodeCount;
                                final int downloadCount2 = prefs.getInt("new_downloads_count", 0) + downloadCount;

                                editor.putInt("new_episode_count", episodeCount);
                                editor.putInt("new_downloads_count", downloadCount2);

                                final String disableStart = prefs.getString("pref_updates_new_episodes_disable_start", "0");
                                final String disableEnd = prefs.getString("pref_updates_new_episodes_disable_end", "0");

                                boolean playSound = prefs.getBoolean("pref_updates_new_episodes_sound", true);

                                if (playSound && DateUtils.isTimeBetweenTwoTime(disableStart, disableEnd, DateUtils.FormatDate(new Date(), "HH:mm:ss")))
                                    playSound = false;

                                if (playSound) {
                                    final MediaPlayer mPlayer = MediaPlayer.create(ctx, R.raw.new_episodes);
                                    mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                    mPlayer.start();
                                }

                                if (downloadEpisodes.size() > 0) {
                                    //only disable bluetooth if wifi is enabled, otherwise there will be no connection
                                    if (prefs.getBoolean("pref_downloads_disable_bluetooth", true) && Utilities.BluetoothEnabled() && Utilities.WifiEnabled(ctx))
                                        BluetoothAdapter.getDefaultAdapter().disable();

                                    mDownloadEpisodes = downloadEpisodes;

                                    editor.putBoolean("from_job", true);
                                    editor.apply();

                                    if (prefs.getBoolean("pref_high_bandwidth", true)) {
                                        mManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

                                        final NetworkRequest request = new NetworkRequest.Builder()
                                                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                                                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                                                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                                                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                                .build();

                                        mManager.requestNetwork(request, mNetworkCallback);

                                        mTimeOutHandler.sendMessageDelayed(
                                                mTimeOutHandler.obtainMessage(MESSAGE_CONNECTIVITY_TIMEOUT),
                                                NETWORK_CONNECTIVITY_TIMEOUT_MS);
                                    } else {
                                        for (final PodcastItem episode : mDownloadEpisodes)
                                            Utilities.startDownload(ctx, episode);
                                    }
                                }
                            }

                            CacheUtils.deletePodcastsCache(ctx);

                            editor.putString("last_podcast_sync_date", new Date().toString());
                            editor.apply();
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


        }
        isWorking = false;
        jobFinished(jobParameters, false);
    }

    public static ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);
            for (final PodcastItem episode : mDownloadEpisodes)
                Utilities.startDownload(mContext.get(), episode);
        }

        @Override
        public void onLost(Network network) {
            final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext.get()).edit();
            editor.putBoolean("show_no_network_message", true);
            editor.apply();
        }
    };

    private BroadcastReceiver mDownloadsComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            unregisterNetworkCallback();
            try {
                mBroadcastManger.unregisterReceiver(mDownloadsComplete);
            } catch (Exception ignored) {}
        }
    };

    private static class TimeOutHandler extends Handler {
        private final WeakReference<BackgroundService> mMainActivityWeakReference;

        TimeOutHandler(BackgroundService service) {
            mMainActivityWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            BackgroundService service = mMainActivityWeakReference.get();

            if (service != null) {
                switch (msg.what) {
                    case MESSAGE_CONNECTIVITY_TIMEOUT:
                        service.unregisterNetworkCallback();
                        mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);
                        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext.get()).edit();
                        editor.putBoolean("show_no_network_message", true);
                        editor.apply();
                        break;
                }
            }
        }
    }

    private void unregisterNetworkCallback() {
        if (mManager != null && mNetworkCallback != null) {
            mManager.unregisterNetworkCallback(mNetworkCallback);
            mManager.bindProcessToNetwork(null);
            mNetworkCallback = null;
        }
    }
}