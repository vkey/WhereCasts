package com.krisdb.wearcasts;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

import static android.support.v4.app.NotificationCompat.PRIORITY_LOW;
import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLogo;

public class MediaPlayerService extends MediaBrowserServiceCompat implements AudioManager.OnAudioFocusChangeListener {
    private static String mPackage = null;
    private static MediaPlayer mMediaPlayer;
    private PodcastItem mEpisode;
    private Context mContext;
    private MediaSessionCompat mMediaSessionCompat;
    private final MediaHandler mMediaHandler = new MediaHandler(this);
    private int mPlaylistID;
    private static int mNotificationID = 101;
    private String mLocalFile;
    private TelephonyManager mTelephonyManager;

    public MediaPlayerService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPackage = getPackageName();
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
            MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final NotificationChannel channel = new NotificationChannel(mPackage.concat(".service"), getString(R.string.notification_channel_media_service), NotificationManager.IMPORTANCE_DEFAULT);
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

                final Notification notification = new NotificationCompat.Builder(this, mPackage.concat(".service"))
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.notification_channel_media_service))
                        .setSmallIcon(R.drawable.ic_notification).build();

              startForeground(1, notification);
            }
            */
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        final ContentValues cv = new ContentValues();
        cv.put("playing", 0);
        new DBPodcastsEpisodes(mContext).updateAll(cv);

        if (mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneState, PhoneStateListener.LISTEN_NONE);

        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (audioManager != null)
            audioManager.abandonAudioFocus(this);

        mMediaSessionCompat.release();
        NotificationManagerCompat.from(this).cancel(mNotificationID);

        if (mLocalFile == null && mMediaPlayer != null) {
            DBUtilities.SaveEpisodeValue(mContext, mEpisode, "position", mMediaPlayer.getCurrentPosition());
            SyncWithMobileDevice();
        }

        clearMediaPlayer();

        mMediaHandler.removeCallbacksAndMessages(null);

        disableNoisyReceiver();
        stopForeground(true);
        //Log.d(mPackage, "MediaPlayerService Media player service stopped");
    }

    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            int position = mMediaPlayer.getCurrentPosition() - (Integer.valueOf(prefs.getString("pref_playback_skip_back", String.valueOf(getResources().getInteger(R.integer.default_playback_skip)))) * 1000);

            if (position > 0)
                mMediaPlayer.seekTo(position);
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            int position = mMediaPlayer.getCurrentPosition() + (Integer.valueOf(prefs.getString("pref_playback_skip_forward", String.valueOf(getResources().getInteger(R.integer.default_playback_skip)))) * 1000);
            if (position < mMediaPlayer.getDuration())
                mMediaPlayer.seekTo(position);
        }

        @Override
        public void onRewind() {
            super.onRewind();
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            int position = mMediaPlayer.getCurrentPosition() - (Integer.valueOf(prefs.getString("pref_playback_skip_back", String.valueOf(getResources().getInteger(R.integer.default_playback_skip)))) * 1000);

            mMediaPlayer.seekTo(position);
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            int position = mMediaPlayer.getCurrentPosition() + (Integer.valueOf(prefs.getString("pref_playback_skip_forward", String.valueOf(getResources().getInteger(R.integer.default_playback_skip)))) * 1000);
            mMediaPlayer.seekTo(position);
        }

        @Override
        public void onPlay() {
            super.onPlay();
            PlayAudio();
        }

        @Override
        public void onPause() {
            super.onPause();
            PauseAudio(true);
        }

        @Override
        public void onPlayFromUri(final Uri uri, final Bundle extras) {
            super.onPlayFromUri(uri, extras);

            mEpisode = DBUtilities.GetEpisode(mContext, extras.getInt("id"));
            mPlaylistID = extras.getInt("playlistid");
            mLocalFile = extras.getString("local_file");

            StartStream(uri);
        }

        @Override
        public void onCommand(final String command, final Bundle extras, final ResultReceiver cb) {
            super.onCommand(command, extras, cb);

        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);

            mMediaPlayer.seekTo((int) pos);
        }
    };

    private void SyncWithMobileDevice()
    {
        SyncWithMobileDevice(false);
    }

    private void SyncWithMobileDevice(boolean finished) {
        new AsyncTasks.SyncWithMobileDevice(mContext, mEpisode, mMediaPlayer, finished).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void PlayAudio()
    {
        if (successfullyRetrievedAudioFocus() == false) return;

        if (mLocalFile == null) {
            final ContentValues cv = new ContentValues();
            cv.put("playing", 1);
            cv.put("finished", 0);

            new DBPodcastsEpisodes(mContext).update(cv, mEpisode.getEpisodeId());
        }

        if (mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneState, PhoneStateListener.LISTEN_CALL_STATE);

        mMediaSessionCompat.setActive(true);
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        final Intent intentMediaPlayed = new Intent();
        intentMediaPlayed.setAction("media_action");
        intentMediaPlayed.putExtra("media_played", true);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intentMediaPlayed);

        initNoisyReceiver();

        showNotification(false, false);

        //mMediaPlayer.reset();
        mMediaPlayer.start();
        mMediaPlayer.setPlaybackParams(mMediaPlayer.getPlaybackParams().setSpeed(Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(mContext).getString("pref_playback_speed", "1.0f"))));

        mMediaHandler.postDelayed(mUpdateMediaPosition, 100);

        if (mLocalFile == null)
            SyncWithMobileDevice();
    }

    private void PauseAudio(final Boolean disableTelephony)
    {
        if (disableTelephony && mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneState, PhoneStateListener.LISTEN_NONE);

        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            if (mLocalFile == null)
            {
                DBUtilities.SaveEpisodeValue(mContext, mEpisode, "position", mMediaPlayer.getCurrentPosition());

                final ContentValues cvPlaying = new ContentValues();
                cvPlaying.put("playing", 0);
                final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mContext);
                db.updateAll(cvPlaying);
                db.close();
            }
            else
            {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(Utilities.GetLocalPositionKey(mLocalFile), mMediaPlayer.getCurrentPosition());
                editor.apply();
            }

            mMediaPlayer.pause();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);

            final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            if (audioManager != null)
                audioManager.abandonAudioFocus(this);

            final Intent intentMediaPaused = new Intent();
            intentMediaPaused.setAction("media_action");
            intentMediaPaused.putExtra("media_paused", true);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intentMediaPaused);
            disableNoisyReceiver();

            stopForeground(false);
            showNotification(true, false);

            if (mLocalFile == null)
                SyncWithMobileDevice();

            mMediaHandler.removeCallbacksAndMessages(null);
        }
    }

    private void StartStream(final Uri uri) {
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        mMediaSessionCompat.setActive(true);

        if (mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneState, PhoneStateListener.LISTEN_CALL_STATE);

        final Intent intentMediaStart = new Intent();
        intentMediaStart.setAction("media_action");
        intentMediaStart.putExtra("media_start", true);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intentMediaStart);

        if (mLocalFile == null) {
            final ContentValues cv = new ContentValues();
            cv.put("playing", 1);
            cv.put("finished", 0);
            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mContext);
            db.update(cv, mEpisode.getEpisodeId());
            db.close();
        }

        initMediaSessionMetadata();
        initNoisyReceiver();


        if (mMediaPlayer != null)
            mMediaPlayer.reset();

        try {
            mMediaPlayer.setDataSource(uri.toString());

            if (uri.toString().startsWith("http"))
                mMediaPlayer.prepareAsync();
            else
                mMediaPlayer.prepare();

            mMediaPlayer.setPlaybackParams(mMediaPlayer.getPlaybackParams().setSpeed(Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(mContext).getString("pref_playback_speed", "1.0f"))));

            showNotification(false, false);

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) { }
            });

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer player) {
                    int position;
                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

                    if (mLocalFile == null)
                        position = DBUtilities.GetEpisodeValue(getApplicationContext(), mEpisode, "position");
                    else
                        position = prefs.getInt(Utilities.GetLocalPositionKey(mLocalFile), 0);

                    if (position == 0)
                        position = Integer.valueOf(prefs.getString("pref_" + mEpisode.getPodcastId() + "_skip_start_time", String.valueOf(mContext.getResources().getInteger(R.integer.default_skip_start_time)))) * 1000;

                    mMediaPlayer.seekTo(position);
                }
            });

            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

                    if (mLocalFile == null)
                        DBUtilities.SaveEpisodeValue(mContext, mEpisode, "duration", mMediaPlayer.getDuration());
                    else
                    {
                        final SharedPreferences.Editor editor = prefs.edit();
                        editor.putLong(Utilities.GetLocalDurationKey(mLocalFile), mMediaPlayer.getDuration());
                        editor.apply();
                    }

                    mMediaHandler.postDelayed(mUpdateMediaPosition, 100);

                    final Intent intentMediaCompleted = new Intent();
                    intentMediaCompleted.setAction("media_action");
                    intentMediaCompleted.putExtra("media_start", false);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(intentMediaCompleted);

                    if (mLocalFile == null)
                        SyncWithMobileDevice();

                    mp.start();
                }
            });

            mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                                                          @Override
                                                          public void onBufferingUpdate(MediaPlayer mp, int percent) {
                      //Intent intentMediaBuffering = new Intent();
                      //intentMediaBuffering.setAction("media_buffering");
                      //intentMediaBuffering.putExtra("percent", percent);
                      //LocalBroadcastManager.getInstance(mContext).sendBroadcast(intentMediaBuffering);
                      //setMediaPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
                      }
                  }
            );

            mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                        //Log.d(mPackage, "MediaPlayerService Buffering started");
                    } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                        //Log.d(mPackage, "MediaPlayerService Buffering ended");
                    }
                    return false;
                }
            });

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {

                    final Intent intentMediaError = new Intent();
                    intentMediaError.setAction("media_action");
                    intentMediaError.putExtra("media_error", true);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(intentMediaError);

                    Log.e(mPackage, "MediaPlayerService Service error: " + String.valueOf(what) + " " + String.valueOf(extra));
                    clearMediaPlayer();
                    return true;
                }
            });

        } catch (Exception ex) {
            Log.e(mPackage, "MediaPlayerService Service error: " + ex.toString());
        }
    }

    private void disableNoisyReceiver() {
        try { unregisterReceiver(mNoisyReceiver); }
        catch(Exception ex) {}
    }

    private void playlistSkip(final Enums.SkipDirection direction, final List<PodcastItem> podcasts) {

        if (mPlaylistID != getResources().getInteger(R.integer.playlist_default)) {

            if (podcasts.size() > 2) {
                int currentPosition = 0;

                if (mLocalFile != null)
                {
                    for (final PodcastItem playlistItem : podcasts) {
                        if (playlistItem.getTitle() != null && Objects.equals(playlistItem.getTitle(), mLocalFile)) break;
                        currentPosition++;
                    }
                }
                else {
                    for (final PodcastItem playlistItem : podcasts) {
                        if (playlistItem.getEpisodeId() == mEpisode.getEpisodeId()) break;
                        currentPosition++;
                    }
                }

                if (direction == Enums.SkipDirection.NEXT && currentPosition < podcasts.size() - 1) //in middle
                    mEpisode = podcasts.get(currentPosition + 1);
                else if (direction == Enums.SkipDirection.NEXT && currentPosition == podcasts.size() - 1) //at end
                    mEpisode = podcasts.get(1);
                else if (direction == Enums.SkipDirection.PREVIOUS && currentPosition == 1) //at beginning, go to end
                    mEpisode = podcasts.get(podcasts.size() - 1);
                else if (direction == Enums.SkipDirection.PREVIOUS && currentPosition <= podcasts.size() - 1) //in middle
                    mEpisode = podcasts.get(currentPosition - 1);

                String uri;

                if (DBUtilities.GetEpisodeValue(mContext, mEpisode, "download") == 1)
                    uri = Utilities.GetMediaFile(mContext, mEpisode);
                else if (mLocalFile != null)
                {
                    mLocalFile = mEpisode.getTitle();
                    uri = CommonUtils.GetLocalDirectory().concat(mEpisode.getTitle());
                }
                else
                    uri = mEpisode.getMediaUrl().toString();

                StartStream(Uri.parse(uri));

                final Bundle extras = new Bundle();
                extras.putInt("id", mEpisode.getEpisodeId());
                mMediaSessionCompat.setExtras(extras);

                final Intent intentMediaPlaylist = new Intent();
                intentMediaPlaylist.setAction("media_action");
                intentMediaPlaylist.putExtra("media_playlist_skip", true);
                intentMediaPlaylist.putExtra("id", mEpisode.getEpisodeId());
                if (mLocalFile != null)
                    intentMediaPlaylist.putExtra("local_file", mLocalFile);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intentMediaPlaylist);
            }
        } else {
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            disableNoisyReceiver();
            SyncWithMobileDevice(true);
        }
    }

    private void initNoisyReceiver() {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        final IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mNoisyReceiver, filter);
    }

    private BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(mPackage, "MediaPlayerService Noisy receiver hit");
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                PauseAudio(false);
            }
        }
    };

    private void showNotification(final Boolean pause, final Boolean update) {

        final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final Bundle bundle = new Bundle();
        bundle.putInt("eid", mEpisode.getEpisodeId());

        final Intent notificationIntent = new Intent(mContext, PodcastEpisodeActivity.class);
        notificationIntent.putExtras(bundle);

        String channelID = mPackage.concat(".playing");

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, channelID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(GetLogo(mContext, mEpisode))
                .setContentText(mLocalFile != null ? mLocalFile : mEpisode.getTitle())
                .setLocalOnly(true)
                .setWhen(0)
                .setOnlyAlertOnce(true)
                .setPriority(PRIORITY_LOW)
                .setVisibility(VISIBILITY_PUBLIC)
                .setContentIntent(PendingIntent.getActivity(mContext, mEpisode.getEpisodeId(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT));
                //.setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(mContext, PlaybackStateCompat.ACTION_STOP));

        if (pause) {
            builder.setOngoing(false);
            builder.addAction(android.R.drawable.ic_media_play, mContext.getString(R.string.play), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE));
        } else {
            builder.setOngoing(true);
            builder.addAction(android.R.drawable.ic_media_pause, mContext.getString(R.string.pause), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE));
        }

        builder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle().setMediaSession(mMediaSessionCompat.getSessionToken()).setShowActionsInCompactView(0));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            final NotificationChannel channel = manager.getNotificationChannel(channelID);

            if (channel == null)
                manager.createNotificationChannel(new NotificationChannel(channelID, getString(R.string.notification_channel_media_playing), NotificationManager.IMPORTANCE_LOW));

            builder.setChannelId(channelID);

            final NotificationChannel notificationChannel = new NotificationChannel(String.valueOf(mNotificationID), mContext.getPackageName(), NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(notificationChannel);

            builder.setChannelId(String.valueOf(mNotificationID));
        }

        if (update) {
            builder.setProgress(mMediaPlayer.getDuration(), mMediaPlayer.getCurrentPosition(), false);
            manager.notify(mNotificationID, builder.build());
        }
        else if (pause)
            manager.notify(mNotificationID, builder.build());
        else
            startForeground(mNotificationID, builder.build());
    }

    private void initMediaSessionMetadata() {
        final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
        //metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, mContext.getString(R.string.app_name));
        final Bitmap roundedLogo = GetLogo(mContext, mEpisode);

        if (roundedLogo != null) {
            final Bitmap logo = CommonUtils.resizedBitmap(roundedLogo, 100, 100);

            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, logo);
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, logo);
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, logo);
        } else {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        }

        //metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, new SimpleDateFormat("h:mm a", Locale.US).format(Calendar.getInstance().getTime()));
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, mLocalFile != null ? mLocalFile : mEpisode.getTitle());
        //metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, mLocalFile != null ? mLocalFile : mEpisode.getTitle());

        //metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, DateUtils.FormatPositionTime(mMediaPlayer.getCurrentPosition()).concat("/").concat(String.valueOf(DateUtils.FormatPositionTime(mMediaPlayer.getDuration()))));

        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, mLocalFile != null ? mLocalFile : mEpisode.getTitle());
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, mContext.getString(R.string.app_name));
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DATE, mEpisode.getPubDate());
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, mEpisode.getDescription());

        //metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1);
        //metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 1);

        if (mMediaPlayer.isPlaying()) {
            try {metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, DateUtils.FormatPositionTime(mMediaPlayer.getDuration() - mMediaPlayer.getCurrentPosition()));}
            catch(Exception ignored){}
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mMediaPlayer.getDuration());
        }

        mMediaSessionCompat.setMetadata(metadataBuilder.build());
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

   private void initMediaSession() {
        final ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), mContext.getString(R.string.app_name), mediaButtonReceiver, null);

        mMediaSessionCompat.setCallback(mMediaSessionCallback);
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);

        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);

        setSessionToken(mMediaSessionCompat.getSessionToken());
    }

    private boolean successfullyRetrievedAudioFocus() {
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        return result == AudioManager.AUDIOFOCUS_GAIN;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, final int clientUid, @Nullable Bundle rootHints) {
        if (clientPackageName.equalsIgnoreCase(getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null);
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
                if (mMediaPlayer != null) {
                    if (!mMediaPlayer.isPlaying()) {
                        mMediaPlayer.start();
                    }
                    mMediaPlayer.setVolume(1.0f, 1.0f);
                }
                break;
            }
        }
    }

    private static class MediaHandler extends Handler {
        private final WeakReference<MediaPlayerService> mWeakReference;

        private MediaHandler(MediaPlayerService service) {
            mWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaPlayerService service = mWeakReference.get();
            if (service != null ) {
            }
        }
    }

    private Runnable mUpdateMediaPosition = new Runnable() {
        public void run() {

            final int position = mMediaPlayer.getCurrentPosition();

            final int specifiedTime = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(mContext).getString("pref_" + mEpisode.getPodcastId() + "_finish_end_time", String.valueOf(mContext.getResources().getInteger(R.integer.default_finish_end_time)))) * 1000;

            final int finishTime = mMediaPlayer.getDuration() - specifiedTime;

            if (position >= finishTime) {
                mMediaHandler.removeCallbacksAndMessages(null);
                completeMedia();
            }
            else {
                final Intent intentMediaPosition = new Intent();
                intentMediaPosition.setAction("media_position");
                intentMediaPosition.putExtra("position", position);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intentMediaPosition);
                initMediaSessionMetadata();
                mMediaHandler.postDelayed(mUpdateMediaPosition, 100);
            }
        }
    };

    final PhoneStateListener mPhoneState = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                PauseAudio(false);
            } else if(state == TelephonyManager.CALL_STATE_IDLE) {
                //if (mMediaPlayer != null && mMediaPlayer.isPlaying() == false)
                //PlayAudio();
            } else if(state == TelephonyManager.CALL_STATE_OFFHOOK) {
                PauseAudio(false);
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

    private void completeMedia()
    {
        setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
        mMediaPlayer.stop();

        if (mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneState, PhoneStateListener.LISTEN_NONE);

        new AsyncTasks.FinishMedia(mContext, mEpisode, mPlaylistID, mLocalFile,
                new Interfaces.PodcastsResponse() {
                    @Override
                    public void processFinish( final List<PodcastItem> playlistItems ) {
                        final Intent intentMediaCompleted = new Intent();
                        intentMediaCompleted.setAction("media_action");
                        intentMediaCompleted.putExtra("media_completed", true);
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intentMediaCompleted);
                        stopForeground(false);
                        SystemClock.sleep(650);
                        playlistSkip(Enums.SkipDirection.NEXT, playlistItems);
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void clearMediaPlayer()
    {
        if (mMediaPlayer != null)
        {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
        }
    }

    @Override
    public void onTaskRemoved(final Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }
}