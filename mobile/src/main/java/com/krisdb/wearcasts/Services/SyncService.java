package com.krisdb.wearcasts.Services;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import com.krisdb.wearcasts.R;

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

                final Intent intentMediaSynced = new Intent();
                intentMediaSynced.setAction("media_action");
                intentMediaSynced.putExtra("media_synced", true);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intentMediaSynced);
            } else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/rateapp")) {
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.google_play_url)));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/fileuploadprogress")) {

                final DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                final Boolean started = dataMapItem.getDataMap().getBoolean("started");
                final Boolean processing = dataMapItem.getDataMap().getBoolean("processing");
                final int length = dataMapItem.getDataMap().getInt("length");
                final int progress = dataMapItem.getDataMap().getInt("progress");
                final Boolean finished = dataMapItem.getDataMap().getBoolean("finished");

                final Intent intentFileUploaded = new Intent();
                intentFileUploaded.setAction("file_uploaded");
                intentFileUploaded.putExtra("started", started);
                intentFileUploaded.putExtra("processing", processing);
                intentFileUploaded.putExtra("length", length);
                intentFileUploaded.putExtra("finished", finished);
                intentFileUploaded.putExtra("progress", progress);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intentFileUploaded);
            } else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/premiumconfirm")) {
                final Intent intentPremiumConfirm = new Intent();
                intentPremiumConfirm.setAction("watchresponse");
                intentPremiumConfirm.putExtra("premium", true);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intentPremiumConfirm);
            }
            else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/syncplaybackspeed")) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final SharedPreferences.Editor editor = prefs.edit();
                final DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                editor.putFloat("pref_playback_speed", dataMapItem.getDataMap().getFloat("playback_speed"));
                editor.apply();
            }
            else if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/thirdparty")) {
                final Intent intent = new Intent();
                intent.setAction("watchresponse");
                intent.putExtra("thirdparty", true);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }
        }
    }
}
