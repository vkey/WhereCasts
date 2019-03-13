package com.krisdb.wearcasts.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.krisdb.wearcasts.Activities.MainActivity;
import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.CacheUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.Date;
import java.util.List;

import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcasts;

public class BackgroundService extends JobService {
    boolean isWorking = false;
    boolean jobCancelled = false;

    public BackgroundService() {

    }

    @Override
    public void onCreate() {
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        isWorking = true;
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
        final Context ctx = getApplicationContext();
        //Log.d(getPackageName(), "Updated Started");
        final List<PodcastItem> podcasts = GetPodcasts(ctx);

        if (podcasts.size() > 0) {
            new AsyncTasks.SyncPodcasts(this, 0, true,
                    new Interfaces.BackgroundSyncResponse() {
                        @Override
                        public void processFinish(int newEpisodeCount, int downloads) {
                            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
                            final SharedPreferences.Editor editor = prefs.edit();
                            if (newEpisodeCount > 0) {
                                final int episodeCount = prefs.getInt("new_episode_count", 0) + newEpisodeCount;
                                final int downloadCount = prefs.getInt("new_downloads_count", 0) + downloads;

                                String displayMessage = episodeCount > 1 ? getString(R.string.plurals_multiple_new_episodes, episodeCount) : getString(R.string.plurals_single_new_episode);

                                if (downloadCount > 0)
                                    displayMessage = displayMessage.concat("\n").concat(downloadCount > 1 ? getString(R.string.notification_downloads_count, downloadCount) : getString(R.string.notification_download_count));

                                final Intent notificationIntent = new Intent(ctx, MainActivity.class);
                                final Bundle bundle = new Bundle();
                                bundle.putBoolean("new_episodes", true);
                                notificationIntent.putExtras(bundle);
                                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                                final PendingIntent intent = PendingIntent.getActivity(ctx, 5, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                                    final String channelID = ctx.getPackageName().concat(".newepisodes");

                                    final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                                    final NotificationChannel mChannel = new NotificationChannel(channelID, getString(R.string.notification_channel_newepisodes), NotificationManager.IMPORTANCE_DEFAULT);
                                    mChannel.setDescription(displayMessage);
                                    mNotificationManager.createNotificationChannel(mChannel);

                                    final Notification notification = new NotificationCompat.Builder(ctx, channelID)
                                            .setContentIntent(intent)
                                            .setSmallIcon(R.drawable.ic_notification)
                                            .setContentTitle(getString(R.string.app_name_wc))
                                            .setContentText(displayMessage).build();

                                    mNotificationManager.notify(102, notification);
                                } else {
                                    final NotificationCompat.Builder notificationBuilder =
                                            new NotificationCompat.Builder(ctx)
                                                    .setSmallIcon(R.drawable.ic_notification)
                                                    .setContentTitle(getString(R.string.app_name_wc))
                                                    .setContentText(displayMessage)
                                                    .setContentIntent(intent);

                                    NotificationManagerCompat.from(ctx).notify(102, notificationBuilder.build());
                                }

                                editor.putInt("new_episode_count", episodeCount);
                                editor.putInt("new_downloads_count", downloadCount);

                                final String disableStart = prefs.getString("pref_updates_new_episodes_disable_start", "0");
                                final String disableEnd = prefs.getString("pref_updates_new_episodes_disable_end", "0");

                                Boolean playSound = prefs.getBoolean("pref_updates_new_episodes_sound", true);

                                if (playSound && DateUtils.isTimeBetweenTwoTime(disableStart, disableEnd, DateUtils.FormatDate(new Date(), "HH:mm:ss")))
                                    playSound = false;

                                if (playSound) {
                                    final MediaPlayer mPlayer = MediaPlayer.create(ctx, R.raw.new_episodes);
                                    mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                    mPlayer.start();
                                }
                                CacheUtils.deletePodcastsCache(ctx);
                            }

                            editor.putString("last_podcast_sync_date", new Date().toString());
                            editor.apply();
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        //Log.d(this.getPackageName(), "Updated Finished");

        isWorking = false;
        jobFinished(jobParameters, false);
    }
}