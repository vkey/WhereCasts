package com.krisdb.wearcasts;


import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.krisdb.wearcastslibrary.PodcastItem;


public class MediaStyleHelper {

    public static NotificationCompat.Builder from(final Context context, final PodcastItem episode, final MediaSessionCompat mediaSession) {

        final MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        final Intent notificationIntent = new Intent(context, PodcastEpisodeActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.setFlags(Notification.FLAG_ONGOING_EVENT);
        notificationIntent.setFlags(Notification.FLAG_NO_CLEAR);
        notificationIntent.setFlags(Notification.FLAG_FOREGROUND_SERVICE);

        final Bundle bundle = new Bundle();
        bundle.putInt("eid", episode.getEpisodeId());
        notificationIntent.putExtras(bundle);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setLargeIcon(description.getIconBitmap())
                //.setContentIntent(controller.getSessionActivity())
                .setContentIntent(PendingIntent.getActivity(context, episode.getEpisodeId(), notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT))
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        MediaSession.Token token = (MediaSession.Token) mediaSession.getSessionToken().getToken();

        Notification.MediaStyle style = new Notification.MediaStyle()
                .setMediaSession(token)
                .setShowActionsInCompactView(0);

        return builder;
    }
}