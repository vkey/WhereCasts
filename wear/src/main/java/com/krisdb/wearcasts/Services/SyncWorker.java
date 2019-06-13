package com.krisdb.wearcasts.Services;

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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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

public class SyncWorker extends Worker {

    private static WeakReference<Context> mContext;

    private static List<PodcastItem> mDownloadEpisodes;
    private LocalBroadcastManager mBroadcastManger;
    private ConnectivityManager mManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private static TimeOutHandler mTimeOutHandler;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        mContext = new WeakReference<>(context);
        mBroadcastManger = LocalBroadcastManager.getInstance(mContext.get());

        mTimeOutHandler = new TimeOutHandler(this);
        mManager = (ConnectivityManager)mContext.get().getSystemService(Context.CONNECTIVITY_SERVICE);

        CommonUtils.writeToFile(mContext.get(),"job started");

        try { mBroadcastManger.registerReceiver(mDownloadsComplete, new IntentFilter("downloads_complete")); }
        catch (Exception ignored) {}
    }

    @NonNull
    @Override
    public Result doWork() {

        final Context ctx = mContext.get();

        final List<PodcastItem> podcasts = GetPodcasts(ctx);

        final Processor processor = new Processor(ctx);
        processor.downloadEpisodes = new ArrayList<>();

        int newEpisodes = 0, downloadCount = 0;

        for (final PodcastItem podcast : podcasts) {

            processor.processEpisodes(podcast);
            newEpisodes = processor.newEpisodesCount;
            downloadCount = processor.downloadCount;

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

            final int downloadsToDeleteNumber = Integer.valueOf(prefs.getString("pref_" + podcast.getPodcastId() + "_downloads_saved", "0"));

            if (downloadsToDeleteNumber > 0) {
                List<PodcastItem> downloads1 = GetEpisodesWithDownloads(ctx, podcast.getPodcastId(), downloadsToDeleteNumber);

                if (downloads1.size() > 0) {
                    for (final PodcastItem download : downloads1) {
                        Utilities.DeleteMediaFile(ctx, download);
                        SystemClock.sleep(500);
                    }
                }
            }

            final int autoDeleteID = Integer.valueOf(prefs.getString("pref_downloads_auto_delete", "1"));

            if (autoDeleteID > Enums.AutoDelete.PLAYED.getAutoDeleteID()) {
                List<PodcastItem> downloads2 = GetEpisodesWithDownloads(ctx, podcast.getPodcastId());

                for (final PodcastItem download : downloads2) {
                    final Date downloadDate = DateUtils.ConvertDate(download.getDownloadDate(), "yyyy-MM-dd HH:mm:ss");
                    final Date compareDate = DateUtils.addHoursToDate(new Date(), autoDeleteID);

                    if (downloadDate.after(compareDate)) {
                        Utilities.DeleteMediaFile(ctx, download);
                        SystemClock.sleep(500);
                    }
                }
            }
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final SharedPreferences.Editor editor = prefs.edit();

        CommonUtils.writeToFile(mContext.get(),"new episodes: " + newEpisodes);
        CommonUtils.writeToFile(mContext.get(),"new downloads: " + downloadCount);

        if (newEpisodes > 0) {
            //used to track failed download attempts and total notification over night
            final int episodeCount = prefs.getInt("new_episode_count", 0) + newEpisodes;
            final int downloadCount2 = prefs.getInt("new_downloads_count", 0) + downloadCount;

            editor.putInt("new_episode_count", episodeCount);
            editor.putInt("new_downloads_count", downloadCount2);

            Utilities.showNewEpisodesNotification(mContext.get(), episodeCount, downloadCount2);

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

            if (processor.downloadEpisodes.size() > 0) {

                mDownloadEpisodes =  processor.downloadEpisodes;

                editor.putBoolean("from_job", true);
                editor.apply();

                if (prefs.getBoolean("pref_disable_bluetooth", false) && Utilities.BluetoothEnabled() && Utilities.disableBluetooth(mContext.get(), false)) {

                    unregisterNetworkCallback();

                    mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(final Network network) {
                            mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);
                            CommonUtils.writeToFile(mContext.get(),"network found");

                            for (final PodcastItem episode : mDownloadEpisodes)
                                Utilities.startDownload(mContext.get(), episode);
                        }
                    };

                    final NetworkRequest request = new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .build();

                    mManager.requestNetwork(request, mNetworkCallback);

                    CommonUtils.writeToFile(mContext.get(),"requesting network");

                    mTimeOutHandler.sendMessageDelayed(
                            mTimeOutHandler.obtainMessage(MESSAGE_CONNECTIVITY_TIMEOUT),
                            NETWORK_CONNECTIVITY_TIMEOUT_MS);
                } else {
                    for (final PodcastItem episode : mDownloadEpisodes)
                        Utilities.startDownload(ctx, episode);
                }
            }
        }

        //CacheUtils.deletePodcastsCache(ctx);

        editor.putString("last_podcast_sync_date", new Date().toString());
        editor.apply();

        CommonUtils.writeToFile(mContext.get(),"job ended");

        return Result.success();
    }

    private BroadcastReceiver mDownloadsComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            unregisterNetworkCallback();
            CommonUtils.writeToFile(mContext.get(),"network release received");

            try {
                mBroadcastManger.unregisterReceiver(mDownloadsComplete);
            } catch (Exception ignored) {}
        }
    };

    private static class TimeOutHandler extends Handler {
        private final WeakReference<SyncWorker> mMainActivityWeakReference;

        TimeOutHandler(final SyncWorker service) {
            super(Looper.getMainLooper());
            mMainActivityWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(final Message msg) {
            final SyncWorker service = mMainActivityWeakReference.get();

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
