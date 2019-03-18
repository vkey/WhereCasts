package com.krisdb.wearcasts;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.RemoteViews;

import com.krisdb.wearcasts.Activities.PhoneMainActivity;

import androidx.media.session.MediaButtonReceiver;

public class WidgetProvider extends AppWidgetProvider {

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String media_url = prefs.getString("episode_url", "");

            Intent intent = new Intent(context, PhoneMainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
            //rv.setTextViewText(R.id.widget_episode_title, prefs.getString("episode_title", ""));
            rv.setTextViewText(R.id.widget_episode_title, "test");
            rv.setOnClickPendingIntent(R.id.widget_episode_title, pendingIntent);

            rv.setOnClickPendingIntent(R.id.iv_widget_playpause, MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY));

            //rv.setOnClickPendingIntent(R.id.widget_player_layout, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }
    }



}
