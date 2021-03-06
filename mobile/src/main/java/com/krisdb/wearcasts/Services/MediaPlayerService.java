package com.krisdb.wearcasts.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcasts.Activities.DirectoryActivity;
import com.krisdb.wearcasts.Models.MediaPlaybackStatus;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import static com.krisdb.wearcastslibrary.CommonUtils.getCurrentPosition;

public class MediaPlayerService extends MediaBrowserServiceCompat implements AudioManager.OnAudioFocusChangeListener {
    private static String mPackage = null;
    private MediaPlayer mMediaPlayer;
    private PodcastItem mEpisode;
    private Context mContext;
    private Handler mMediaTimeHandler = new Handler();
    private MediaSessionCompat mMediaSessionCompat;
    private static int mNotificationID = 101;
    private String mNotificationChannelID;
    private TelephonyManager mTelephonyManager;

    public MediaPlayerService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPackage = getPackageName();
        mNotificationChannelID = mPackage.concat(".playback");
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        mMediaSessionCompat = new MediaSessionCompat(this, MediaPlayerService.class.getSimpleName());
        mContext = this;

        initMediaPlayer();
        initMediaSession();
    }

    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        mMediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            final NotificationChannel channel = new NotificationChannel(mNotificationChannelID, "Media Player Service", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            final Notification notification = new NotificationCompat.Builder(this, mNotificationChannelID)
                    .setContentTitle(getString(R.string.app_name_wc))
                    .setContentText("Media Player Service")
                    .setSmallIcon(R.drawable.ic_notification).build();

            startForeground(1, notification);
        }
        */

        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);

        return START_STICKY;
    }

    final PhoneStateListener mPhoneState = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                PauseAudio();
            } else if(state == TelephonyManager.CALL_STATE_IDLE) {
                //if (mMediaPlayer != null && mMediaPlayer.isPlaying() == false)
                //PlayAudio();
            } else if(state == TelephonyManager.CALL_STATE_OFFHOOK) {
                PauseAudio();
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneState, PhoneStateListener.LISTEN_NONE);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isplaying", false);

        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (audioManager != null)
            audioManager.abandonAudioFocus(this);

        mMediaSessionCompat.release();
        NotificationManagerCompat.from(this).cancel(mNotificationID);

        if (mMediaPlayer != null) {
            editor.putInt("position", getCurrentPosition(mMediaPlayer));
            //mMediaPlayer.stop();
            //mMediaPlayer.reset();
            //mMediaPlayer.release();
        }

        editor.apply();

        mMediaTimeHandler.removeCallbacks(UpdateMediaTime);
        disableNoisyReceiver();
        stopForeground(true);

        Log.d(mPackage, "Media player service stopped");
    }

    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public void onPlay() {
            super.onPlay();

            if (successfullyRetrievedAudioFocus() == false) return;

            mMediaSessionCompat.setActive(true);
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

            final MediaPlaybackStatus mps = new MediaPlaybackStatus();
            mps.setMediaPlay(true);

            EventBus.getDefault().post(mps);

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isplaying", true);
            editor.apply();

            showNotification(false);

            if (mTelephonyManager != null)
                mTelephonyManager.listen(mPhoneState, PhoneStateListener.LISTEN_CALL_STATE);

            mMediaPlayer.start();

            SyncWithWearDevice();
        }

        @Override
        public void onPause() {
            super.onPause();
            PauseAudio();
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            super.onPlayFromUri(uri, extras);

            mMediaPlayer.reset();

            mEpisode = new PodcastItem();
            mEpisode.setTitle(PreferenceManager.getDefaultSharedPreferences(mContext).getString("episode_title", ""));
            mEpisode.setDescription("");

            mMediaSessionCompat.setActive(true);
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            initMediaSessionMetadata();

            showNotification(false);

            StartStream(uri);

            startService(new Intent(getApplicationContext(), MediaPlayerService.class));
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);

        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);

            mMediaPlayer.seekTo((int) pos);
        }
    };

    private void PauseAudio()
    {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("position", getCurrentPosition(mMediaPlayer));
            editor.putBoolean("isplaying", false);
            editor.apply();

            mMediaPlayer.pause();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            stopForeground(false);
            showNotification(true);
            disableNoisyReceiver();

            final MediaPlaybackStatus mpsPaused = new MediaPlaybackStatus();
            mpsPaused.setMediaPaused(true);

            EventBus.getDefault().post(mpsPaused);

            if (mTelephonyManager != null)
                mTelephonyManager.listen(mPhoneState, PhoneStateListener.LISTEN_NONE);

            SyncWithWearDevice();
        }
    }

    private void SyncWithWearDevice() {
        SyncWithWearDevice(false);
    }

    private void SyncWithWearDevice(final boolean finished) {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        final PutDataMapRequest dataMap = PutDataMapRequest.create("/syncwear");
        dataMap.getDataMap().putInt("position", finished ? 0 : getCurrentPosition(mMediaPlayer));
        dataMap.getDataMap().putBoolean("finished", finished);
        dataMap.getDataMap().putInt("id", prefs.getInt("id", 0));

        CommonUtils.DeviceSync(mContext, dataMap);
    }

    private void StartStream(Uri uri) {

        final MediaPlaybackStatus mpsStart = new MediaPlaybackStatus();
        mpsStart.setMediaStart(true);

        EventBus.getDefault().post(mpsStart);

        initNoisyReceiver();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isplaying", true);
        editor.apply();

        if (mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneState, PhoneStateListener.LISTEN_CALL_STATE);

        try {

            Log.d(mPackage, "STREAMING FROM: " + uri);

            mMediaPlayer.setDataSource(uri.toString());

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);


                    MediaPlaybackStatus mpsCompleted = new MediaPlaybackStatus();
                    mpsCompleted.setMediaCompleted(true);

                    EventBus.getDefault().post(mpsCompleted);

                    SyncWithWearDevice(true);

                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("id", 0);
                    editor.putInt("position", 0);
                    editor.putBoolean("isplaying", false);
                    editor.apply();
                }
            });

            mMediaPlayer.setOnPreparedListener(player -> {
                mMediaTimeHandler.postDelayed(UpdateMediaTime, 100);
                mMediaPlayer.seekTo(prefs.getInt("position", 0));
            });

            mMediaPlayer.setOnSeekCompleteListener(mp -> {
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                mp.start();
                final SharedPreferences prefs1 = PreferenceManager.getDefaultSharedPreferences(mContext);
                SharedPreferences.Editor editor1 = prefs1.edit();
                editor1.putBoolean("isplaying", true);
                editor1.apply();

                MediaPlaybackStatus mpsMediaPlaying = new MediaPlaybackStatus();
                mpsMediaPlaying.setMediaPlaying(true);

                EventBus.getDefault().post(mpsMediaPlaying);

                SyncWithWearDevice();
            });

            mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                final MediaPlaybackStatus mpsError = new MediaPlaybackStatus();
                mpsError.setErrorCode(what);
                mpsError.setMediaError(true);

                Log.e(mPackage, "Service error: " + what + " " + extra);

                if (mMediaPlayer != null) {
                    if (mMediaPlayer.isPlaying())
                        mMediaPlayer.stop();

                    try {
                        mMediaPlayer.reset();
                    } catch (Exception ignored)
                    {}
                }

                return true;
            });

            mMediaPlayer.prepareAsync();
            mMediaPlayer.setPlaybackParams(mMediaPlayer.getPlaybackParams().setSpeed(PreferenceManager.getDefaultSharedPreferences(mContext).getFloat("pref_playback_speed", 1.0f)));
        } catch (Exception ex) {
            Log.e(mPackage, "Service error: " + ex.toString());
        }
    }

    private void disableNoisyReceiver() {
        try { unregisterReceiver(mNoisyReceiver); }
        catch(Exception ex) {}
    }

    private void initNoisyReceiver() {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        final IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mNoisyReceiver, filter);
    }

    private BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(mPackage, "MediaPlayerService Noisy receiver hit");
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                PauseAudio();
            }
        }
    };

    private void showNotification(Boolean pause) {

        final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final Bundle bundle = new Bundle();
        bundle.putInt("eid", mEpisode.getEpisodeId());

        final Intent notificationIntent = new Intent(mContext, DirectoryActivity.class);
        notificationIntent.putExtras(bundle);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(mContext, mNotificationChannelID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setColor(0x008000)
                .setContentText(mEpisode.getTitle())
                .setContentIntent(PendingIntent.getActivity(mContext, mEpisode.getEpisodeId(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(mContext, PlaybackStateCompat.ACTION_STOP));

        if (pause) {
            notification.setOngoing(false);
            notification.addAction(android.R.drawable.ic_media_play, mContext.getString(R.string.play), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE));
        } else {
            notification.setOngoing(true);
            notification.addAction(android.R.drawable.ic_media_pause, mContext.getString(R.string.pause), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE));
        }

        notification.setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mMediaSessionCompat.getSessionToken()).setShowActionsInCompactView(0));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = manager.getNotificationChannel(mNotificationChannelID);

            if (channel == null)
                manager.createNotificationChannel(new NotificationChannel(mNotificationChannelID, "Playback", NotificationManager.IMPORTANCE_LOW));

            notification.setChannelId(mNotificationChannelID);

            final NotificationChannel notificationChannel = new NotificationChannel(String.valueOf(mNotificationID), mContext.getPackageName(), NotificationManager.IMPORTANCE_LOW);
            notificationChannel.setSound(null, null);
            manager.createNotificationChannel(notificationChannel);

            notification.setChannelId(String.valueOf(mNotificationID));
        }

        if (pause)
            manager.notify(mNotificationID, notification.build());
        else
            startForeground(mNotificationID, notification.build());
    }

    private void setMediaPlaybackState(final int state)
    {
        setMediaPlaybackState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN);
    }

    private void setMediaPlaybackState(final int state, final long position) {
        final PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();

        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playbackstateBuilder.setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE |
                            PlaybackStateCompat.ACTION_PAUSE |
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                            PlaybackStateCompat.ACTION_FAST_FORWARD |
                            PlaybackStateCompat.ACTION_REWIND
            );
        } else
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);

        playbackstateBuilder.setState(state, position, 0);
        mMediaSessionCompat.setPlaybackState(playbackstateBuilder.build());
    }

    private void initMediaSessionMetadata() {
        final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
        //Notification icon in card
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

        //lock screen icon for pre lollipop
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, mContext.getString(R.string.app_name_wc));
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, mEpisode.getTitle());
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 1);

        mMediaSessionCompat.setMetadata(metadataBuilder.build());
    }

    private void initMediaSession() {
        final ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), mContext.getString(R.string.app_name_wc), mediaButtonReceiver, null);

        mMediaSessionCompat.setCallback(mMediaSessionCallback);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O)
            mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);

        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);

        setSessionToken(mMediaSessionCompat.getSessionToken());
    }

    private boolean successfullyRetrievedAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        return result == AudioManager.AUDIOFOCUS_GAIN;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        if (clientPackageName.equalsIgnoreCase(getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name_wc), null);
        }

        return null;
    }

    //Not important for general audio service, required for class
    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    @Override
    public void onAudioFocusChange(final int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS: {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: {
                mMediaPlayer.pause();
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setVolume(0.3f, 0.3f);
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_GAIN: {
                //if (mMediaPlayer != null) {
                    //if (!mMediaPlayer.isPlaying()) {
                    //mMediaPlayer.start();
                    //}
                    //mMediaPlayer.setVolume(1.0f, 1.0f);
                //}
                break;
            }
        }
    }

    private Runnable UpdateMediaTime = new Runnable() {
        public void run() {

            MediaPlaybackStatus mpsPosition = new MediaPlaybackStatus();
            mpsPosition.setPosition(getCurrentPosition(mMediaPlayer));

            EventBus.getDefault().post(mpsPosition);

            mMediaTimeHandler.postDelayed(this, 100);
        }
    };
}
