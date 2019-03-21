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
import android.preference.PreferenceManager;
import android.util.Log;

import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.CacheUtils;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcasts;

public class BackgroundService extends JobService {
    boolean isWorking = false;
    boolean jobCancelled = false;
    private static List<PodcastItem> mDownloadEpisodes;
    private static WeakReference<Context> mContext;
    private LocalBroadcastManager mBroadcastManger;
    private ConnectivityManager mManager;

    public BackgroundService() {}

    @Override
    public void onCreate() {}

    @Override
    public boolean onStartJob(JobParameters params) {
        isWorking = true;
        mContext = new WeakReference<>(getApplicationContext());
        mBroadcastManger = LocalBroadcastManager.getInstance(mContext.get());

        try {mBroadcastManger.registerReceiver(mDownloadsComplete, new IntentFilter("downloads_complete")); }
        catch(Exception ignored){}
        Log.d(getPackageName(), "[downloads] job start");

        doWork(params);

        return isWorking;
    }

    // Called if the job was cancelled before being finished
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        jobCancelled = true;
        boolean needsReschedule = isWorking;
        Log.d(getPackageName(), "[downloads] job stop (reschedule): " + needsReschedule);
        jobFinished(jobParameters, needsReschedule);
        return needsReschedule;
    }

    private void doWork(JobParameters jobParameters) {

        //Log.d(getPackageName(), "Updated Started");
        final List<PodcastItem> podcasts = GetPodcasts(mContext.get());

        if (podcasts.size() > 0) {
            new AsyncTasks.SyncPodcasts(this, 0, true,
                    new Interfaces.BackgroundSyncResponse() {
                        @Override
                        public void processFinish(final int newEpisodeCount, final int downloadCount, final List<PodcastItem> downloadEpisodes) {
                            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext.get());
                            final SharedPreferences.Editor editor = prefs.edit();
                            Log.d(getPackageName(), "[downloads] SERVICE new episode count: " + newEpisodeCount);
                            Log.d(getPackageName(), "[downloads] SERVICE new download count: " + downloadCount);
                            Log.d(getPackageName(), "[downloads] SERVICE new downloads size: " + downloadEpisodes.size());

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
                                    final MediaPlayer mPlayer = MediaPlayer.create(mContext.get(), R.raw.new_episodes);
                                    mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                    mPlayer.start();
                                }
                                Log.d(getPackageName(), "[downloads] SERVICE download count: " + downloadEpisodes.size());

                                if (downloadEpisodes.size() > 0)
                                {
                                    if (prefs.getBoolean("pref_downloads_disable_bluetooth", true) && Utilities.BluetoothEnabled())
                                    {
                                        BluetoothAdapter.getDefaultAdapter().disable();
                                        Log.d(mContext.get().getPackageName(), "[downloads] SERVICE bluetooth disabled");
                                    }

                                    mDownloadEpisodes = downloadEpisodes;
                                    if (prefs.getBoolean("pref_high_bandwidth", true))
                                    {
                                        mManager = (ConnectivityManager) mContext.get().getSystemService(Context.CONNECTIVITY_SERVICE);
                                        Log.d(mContext.get().getPackageName(), "[downloads] SERVICE network requested");

                                        editor.putBoolean("from_job", true);
                                        editor.apply();

                                        final NetworkRequest request = new NetworkRequest.Builder()
                                                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                                                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                                                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                                                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                                .build();

                                        mManager.requestNetwork(request, mNetworkCallback);
                                    }
                                    else
                                    {
                                        for(final PodcastItem episode : downloadEpisodes)
                                            Utilities.startDownload(mContext.get(), episode);
                                    }
                                }
                            }

                            CacheUtils.deletePodcastsCache(mContext.get());

                            editor.putString("last_podcast_sync_date", new Date().toString());
                            editor.apply();
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        //Log.d(this.getPackageName(), "Updated Finished");

        isWorking = false;
        jobFinished(jobParameters, false);
    }

    public static ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            Log.d(mContext.get().getPackageName(), "[downloads] CALLBACK network available");
            Log.d(mContext.get().getPackageName(), "[downloads] CALLBACK network available downloads size: " + mDownloadEpisodes.size());

            for(final PodcastItem episode : mDownloadEpisodes)
               Utilities.startDownload(mContext.get(), episode);
        }
    };

    private BroadcastReceiver mDownloadsComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mManager != null && mNetworkCallback != null) {
                mManager.bindProcessToNetwork(null);
                mManager.unregisterNetworkCallback(mNetworkCallback);
            }
            Log.d(context.getPackageName(), "[downloads] CALLBACK released broadcast received");

            try {mBroadcastManger.unregisterReceiver(mDownloadsComplete); }
            catch(Exception ignored){}
        }
    };
}