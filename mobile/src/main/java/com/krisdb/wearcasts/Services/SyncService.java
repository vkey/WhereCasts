package com.krisdb.wearcasts.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import com.krisdb.wearcasts.Activities.DirectoryActivity;
import com.krisdb.wearcasts.Models.FileUploadProgress;
import com.krisdb.wearcasts.Models.MediaPlaybackStatus;
import com.krisdb.wearcasts.Models.OPMLImport;
import com.krisdb.wearcasts.Models.WatchStatus;
import com.krisdb.wearcasts.R;

import org.greenrobot.eventbus.EventBus;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class SyncService extends WearableListenerService {

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/syncdevice")) {
                final DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                final String podcast_title = dataMapItem.getDataMap().getString("podcast_title");
                final String episode_url = dataMapItem.getDataMap().getString("episode_url");
                final String episode_title = dataMapItem.getDataMap().getString("episode_title");
                final int position = dataMapItem.getDataMap().getInt("position");
                final int duration = dataMapItem.getDataMap().getInt("duration");
                final int id = dataMapItem.getDataMap().getInt("id");

                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putString("podcast_title", podcast_title);
                editor.putString("episode_url", episode_url);
                editor.putString("episode_title", episode_title);
                editor.putInt("position", position);
                editor.putInt("duration", duration);
                editor.putInt("id", id);
                editor.apply();


                MediaPlaybackStatus mpsMediaSync = new MediaPlaybackStatus();
                mpsMediaSync.setMediaSync(true);

                EventBus.getDefault().post(mpsMediaSync);

            } else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/rateapp")) {
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.google_play_url)));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/fileuploadprogress")) {

                final DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                final boolean started = dataMapItem.getDataMap().getBoolean("started");
                final boolean processing = dataMapItem.getDataMap().getBoolean("processing");
                //final int length = dataMapItem.getDataMap().getInt("length");
                //final int progress = dataMapItem.getDataMap().getInt("progress");
                final boolean complete = dataMapItem.getDataMap().getBoolean("finished");

                final FileUploadProgress fup = new FileUploadProgress();
                fup.setStarted(started);
                fup.setProcessing(processing);
                fup.setComplete(complete);
                EventBus.getDefault().post(fup);

            } else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/premiumconfirm")) {
                final WatchStatus wsPremiumConfirm = new WatchStatus();
                wsPremiumConfirm.setPremiumConfirm(true);
                EventBus.getDefault().post(wsPremiumConfirm);
            }
            else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/syncplaybackspeed")) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final SharedPreferences.Editor editor = prefs.edit();
                final DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                editor.putFloat("pref_playback_speed", dataMapItem.getDataMap().getFloat("playback_speed"));
                editor.apply();
            }
            else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/thirdparty")) {

                 final WatchStatus wsThirdPartyConfirm = new WatchStatus();
                wsThirdPartyConfirm.setThirdParty(true);

                EventBus.getDefault().post(wsThirdPartyConfirm);
            }
            else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/opmlimport_complete")) {
                final OPMLImport opmlComplete = new OPMLImport();
                opmlComplete.setComplete(true);

                EventBus.getDefault().post(opmlComplete);

                final Intent notificationIntent = new Intent(this, DirectoryActivity.class);
                notificationIntent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                final PendingIntent intentDirectory = PendingIntent.getActivity(this, 5, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    final String channelID = getPackageName().concat(".opmlcomplete");

                    final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    final NotificationChannel channel = new NotificationChannel(channelID, getString(R.string.notification_channel_opml_import_complete), NotificationManager.IMPORTANCE_DEFAULT);
                    notificationManager.createNotificationChannel(channel);

                    final Notification notification = new NotificationCompat.Builder(this, channelID)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(getString(R.string.notification_opml_import_complete_title))
                            .setContentIntent(intentDirectory)
                            .build();

                    notificationManager.notify(122, notification);
                } else {
                    final NotificationCompat.Builder notificationBuilder =
                            new NotificationCompat.Builder(this)
                                    .setSmallIcon(R.drawable.ic_notification)
                                    .setContentIntent(intentDirectory)
                                    .setContentTitle(getString(R.string.notification_opml_import_complete_title));

                    NotificationManagerCompat.from(this).notify(122, notificationBuilder.build());
                }
            }
            else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/opmlimport_podcasts")) {
                final DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                final OPMLImport opmlPodcasts = new OPMLImport();
                opmlPodcasts.setPodcasts(true);
                opmlPodcasts.setTitle(dataMapItem.getDataMap().getString("podcast_title"));

                EventBus.getDefault().post(opmlPodcasts);
            }
            else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/opmlimport_episodes")) {
                final DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                final OPMLImport opmlEpisodes = new OPMLImport();
                opmlEpisodes.setEpisodes(true);
                opmlEpisodes.setTitle(dataMapItem.getDataMap().getString("podcast_title_episodes"));

                EventBus.getDefault().post(opmlEpisodes);

            }
            else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/opmlimport_art")) {
                final DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                final OPMLImport opmlArt = new OPMLImport();
                opmlArt.setArt(true);
                opmlArt.setTitle(dataMapItem.getDataMap().getString("podcast_title_art"));

                EventBus.getDefault().post(opmlArt);
            }
        }
    }
}
