package com.krisdb.wearcasts.Services;

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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.krisdb.wearcasts.Activities.EpisodeActivity;
import com.krisdb.wearcasts.Async.FinishMedia;
import com.krisdb.wearcasts.Async.SyncWithMobileDevice;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Models.MediaPlaybackStatus;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.Async.WatchConnected;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.PodcastItem;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

import static androidx.core.app.NotificationCompat.PRIORITY_LOW;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisode;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodeValue;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.SaveEpisodeValue;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLogo;
import static com.krisdb.wearcastslibrary.CommonUtils.getCurrentPosition;

public class MediaPlayerService extends MediaBrowserServiceCompat implements AudioManager.OnAudioFocusChangeListener {
    private static String mPackage = null;
    private static MediaPlayer mMediaPlayer;
    private PodcastItem mEpisode;
    private Context mContext;
    private MediaSessionCompat mMediaSessionCompat;
    private final MediaHandler mMediaHandler = new MediaHandler(this);
    private int mPlaylistID, mPlaybackPosition = 0, mPlaybackCount = 0;
    private static int mNotificationID = 101;
    private String mLocalFile;
    private TelephonyManager mTelephonyManager;
    private Boolean mError = false;
    private boolean mHasPremium, mCallStarted = false;
    private AudioManager mAudioManager;

    public MediaPlayerService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPackage = getPackageName();

        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mMediaSessionCompat = new MediaSessionCompat(this, MediaPlayerService.class.getSimpleName());

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mContext = this;

        initMediaPlayer();
        initMediaSession();

        mHasPremium = Utilities.hasPremium(this);
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

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        final ContentValues cv = new ContentValues();
        cv.put("playing", 0);
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mContext);
        db.updateAll(cv);
        db.close();

        if (mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneState, PhoneStateListener.LISTEN_NONE);

        if (mAudioManager != null)
            mAudioManager.abandonAudioFocus(this);

        mMediaSessionCompat.release();
        NotificationManagerCompat.from(this).cancel(mNotificationID);

        if (mLocalFile == null && mMediaPlayer != null) {
            SaveEpisodeValue(mContext, mEpisode, "position", getCurrentPosition(mMediaPlayer));
            SyncWithMobileDevice();
        }

        //if (Utilities.sleepTimerEnabled(mContext)) {
            //try { unregisterReceiver(mSleepTimer); }
        //catch (Exception ignored) {}
        //}

        mMediaHandler.removeCallbacksAndMessages(null);

/*        if (CommonUtils.inDebugMode(mContext)) {
            if (prefs.getBoolean("pref_detect_bluetooth_changes", true))
            {
                try { unregisterReceiver(mBluetoothConnected);}
                catch (Exception ignored) {}
            }
        }*/

        disableNoisyReceiver();
        stopForeground(true);
    }

    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            final int position = getCurrentPosition(mMediaPlayer) - (Integer.valueOf(prefs.getString("pref_playback_skip_back", String.valueOf(getResources().getInteger(R.integer.default_playback_skip)))) * 1000);

            if (position > 0)
                mMediaPlayer.seekTo(position);
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            final int position = getCurrentPosition(mMediaPlayer) + (Integer.valueOf(prefs.getString("pref_playback_skip_forward", String.valueOf(getResources().getInteger(R.integer.default_playback_skip)))) * 1000);

            if (position < mMediaPlayer.getDuration())
                mMediaPlayer.seekTo(position);
        }

        @Override
        public void onRewind() {
            super.onRewind();
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            final int position = getCurrentPosition(mMediaPlayer) - (Integer.valueOf(prefs.getString("pref_playback_skip_back", String.valueOf(getResources().getInteger(R.integer.default_playback_skip)))) * 1000);

            mMediaPlayer.seekTo(position);
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            final int position = getCurrentPosition(mMediaPlayer) + (Integer.valueOf(prefs.getString("pref_playback_skip_forward", String.valueOf(getResources().getInteger(R.integer.default_playback_skip)))) * 1000);

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
            PauseAudio();
        }

        @Override
        public void onPlayFromUri(final Uri uri, final Bundle extras) {
            super.onPlayFromUri(uri, extras);

            mPlaylistID = extras.getInt("playlistid");
            mLocalFile = extras.getString("local_file");
            mEpisode = GetEpisode(mContext, extras.getInt("episodeid"));

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
        CommonUtils.executeAsync(new WatchConnected(this), (connected) -> {
            if (connected)
                CommonUtils.executeAsync(new SyncWithMobileDevice(mContext, mEpisode, mMediaPlayer, finished), (response) -> { });
        });
    }

    private void PlayAudio()
    {
        if (successfullyRetrievedAudioFocus() == false) return;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        if (mLocalFile == null) {
            final ContentValues cv = new ContentValues();
            cv.put("playing", 1);
            cv.put("finished", 0);

            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mContext);
            db.update(cv, mEpisode.getEpisodeId());
            db.close();
        }

        if (mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneState, PhoneStateListener.LISTEN_CALL_STATE);

        if (mAudioManager != null)
            mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        mMediaSessionCompat.setActive(true);
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

        final MediaPlaybackStatus mps = new MediaPlaybackStatus();
        mps.setMediaPlay(true);

        EventBus.getDefault().post(mps);

        showNotification(false);
        mPlaybackPosition = 0;
        mPlaybackCount = 0;
        mMediaPlayer.start();

        if (mHasPremium) {

            float playbackSpeed = Float.parseFloat(prefs.getString("pref_playback_speed", "1.0f"));
            float playbackSpeed2 = Float.valueOf(prefs.getString("pref_" + mEpisode.getPodcastId() + "_playback_speed", "0"));

            if (playbackSpeed2 != 0)
                playbackSpeed = playbackSpeed2;

            if (playbackSpeed != 1.0f)
                mMediaPlayer.setPlaybackParams(mMediaPlayer.getPlaybackParams().setSpeed(playbackSpeed));
        }

        initNoisyReceiver();

        mMediaHandler.postDelayed(mUpdateMediaPosition, 100);

        if (mLocalFile == null)
            SyncWithMobileDevice();
    }

    private void PauseAudio() {
        PauseAudio(true, true);
    }

    private void PauseAudio(final Boolean disableTelephony, final Boolean disableAudioFocus)
    {
        if (disableTelephony && mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneState, PhoneStateListener.LISTEN_NONE);

        if (disableAudioFocus && mAudioManager != null)
            mAudioManager.abandonAudioFocus(this);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            if (mLocalFile == null)
            {
                SaveEpisodeValue(mContext, mEpisode, "position", getCurrentPosition(mMediaPlayer));

                final ContentValues cvPlaying = new ContentValues();
                cvPlaying.put("playing", 0);
                final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mContext);
                db.updateAll(cvPlaying);
                db.close();
            }
            else
            {
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(Utilities.GetLocalPositionKey(mLocalFile), getCurrentPosition(mMediaPlayer));
                editor.apply();
            }

            mPlaybackPosition = 0;
            mPlaybackCount = 0;

            mMediaPlayer.pause();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);

            final MediaPlaybackStatus mpsPaused = new MediaPlaybackStatus();
            mpsPaused.setMediaPaused(true);

            EventBus.getDefault().post(mpsPaused);

            disableNoisyReceiver();

            showNotification(true);
            stopForeground(prefs.getBoolean("pref_remove_notification", false));

            if (mLocalFile == null)
                SyncWithMobileDevice();

            if (mLocalFile == null && !mEpisode.getIsDownloaded())
                Utilities.enableBluetooth(mContext);

            mMediaHandler.removeCallbacksAndMessages(null);
        }
    }

    private void StartStream(final Uri uri) {

        if (mEpisode.getIsDownloaded() && !Utilities.getEpisodeFile(mContext, mEpisode).exists())
        {
            CommonUtils.showToast(mContext, getString(R.string.alert_download_error), Toast.LENGTH_LONG);
            return;
        }

        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        mMediaSessionCompat.setActive(true);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        if (mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneState, PhoneStateListener.LISTEN_CALL_STATE);

        if (mAudioManager != null)
            mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        mCallStarted = false;

        final MediaPlaybackStatus mpsStart = new MediaPlaybackStatus();
        mpsStart.setMediaStart(true);

        EventBus.getDefault().post(mpsStart);

        if (mLocalFile == null) {
            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mContext);

            final ContentValues cvPlayingReset = new ContentValues();
            cvPlayingReset.put("playing", 0);
            db.updateAll(cvPlayingReset);

            final ContentValues cvPlaying = new ContentValues();
            cvPlaying.put("playing", 1);
            cvPlaying.put("finished", 0);
            db.update(cvPlaying, mEpisode.getEpisodeId());
            db.close();
        }

        initNoisyReceiver();
        initMediaSessionMetadata();
        mPlaybackPosition = 0;
        mPlaybackCount = 0;
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            try {
                mMediaPlayer.setDataSource(uri.toString());
            } catch (IllegalStateException ex) {
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(uri.toString());
            }

            if (mLocalFile == null && !mEpisode.getIsDownloaded()) {
                new Handler(Looper.getMainLooper()).post(() -> CommonUtils.showToast(mContext, getString(R.string.alert_streaming)));
                mMediaPlayer.prepareAsync();
            } else
                mMediaPlayer.prepare();

            mError = false;

            if (mHasPremium) {
                float playbackSpeed = Float.parseFloat(prefs.getString("pref_playback_speed", "1.0f"));
                float playbackSpeed2 = Float.valueOf(prefs.getString("pref_" + mEpisode.getPodcastId() + "_playback_speed", "0"));

                if (playbackSpeed2 != 0)
                    playbackSpeed = playbackSpeed2;

                if (playbackSpeed != 1.0f)
                    mMediaPlayer.setPlaybackParams(mMediaPlayer.getPlaybackParams().setSpeed(playbackSpeed));
            }

            if (mLocalFile != null || mEpisode.getIsDownloaded()) {
                int position;

                if (mLocalFile == null)
                    position = GetEpisodeValue(getApplicationContext(), mEpisode, "position");
                else
                    position = prefs.getInt(Utilities.GetLocalPositionKey(mLocalFile), 0);

                if (mHasPremium && position == 0)
                    position = Integer.valueOf(prefs.getString("pref_" + mEpisode.getPodcastId() + "_skip_start_time", String.valueOf(mContext.getResources().getInteger(R.integer.default_skip_start_time)))) * 1000;

                mMediaPlayer.seekTo(position);
            }

            showNotification(false);

            mMediaPlayer.setOnCompletionListener(mp -> {
            });

            mMediaPlayer.setOnPreparedListener(player -> {
                if (mLocalFile == null && !mEpisode.getIsDownloaded()) {
                    int position;

                    if (mLocalFile == null)
                        position = GetEpisodeValue(getApplicationContext(), mEpisode, "position");
                    else
                        position = prefs.getInt(Utilities.GetLocalPositionKey(mLocalFile), 0);

                    if (position == 0)
                        position = Integer.valueOf(prefs.getString("pref_" + mEpisode.getPodcastId() + "_skip_start_time", String.valueOf(mContext.getResources().getInteger(R.integer.default_skip_start_time)))) * 1000;

                    mMediaPlayer.seekTo(position);
                }
            });

            mMediaPlayer.setOnSeekCompleteListener(mp -> {
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

                final SharedPreferences prefs1 = PreferenceManager.getDefaultSharedPreferences(mContext);

                if (mLocalFile == null)
                    SaveEpisodeValue(mContext, mEpisode, "duration", mMediaPlayer.getDuration());
                else {
                    final SharedPreferences.Editor editor = prefs1.edit();
                    editor.putLong(Utilities.GetLocalDurationKey(mLocalFile), mMediaPlayer.getDuration());
                    editor.apply();
                }

                mPlaybackPosition = 0;
                mPlaybackCount = 0;

                mMediaHandler.removeCallbacksAndMessages(null);
                mMediaHandler.postDelayed(mUpdateMediaPosition, 100);

                MediaPlaybackStatus mpsMediaPlaying = new MediaPlaybackStatus();
                mpsMediaPlaying.setMediaPlaying(true);

                EventBus.getDefault().post(mpsMediaPlaying);

                if (mLocalFile == null)
                    SyncWithMobileDevice();

                mp.start();
            });

            mMediaPlayer.setOnInfoListener((mp, what, extra) -> {
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    //Log.d(mPackage, "MediaPlayerService Buffering started");
                } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    //Log.d(mPackage, "MediaPlayerService Buffering ended");
                }
                return false;
            });

            mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                if (what != -38) {

                    final MediaPlaybackStatus mpsError = new MediaPlaybackStatus();
                    mpsError.setErrorCode(what);
                    mpsError.setMediaError(true);

                    EventBus.getDefault().post(mpsError);

                    PauseAudio();
                    mError = true;
                    mPlaybackPosition = 0;
                    mPlaybackCount = 0;

                    if (mMediaPlayer != null) {
                        if (mMediaPlayer.isPlaying())
                            mMediaPlayer.stop();

                        try {
                            mMediaPlayer.reset();
                        } catch (Exception ignored) {
                        }
                    }
                }

                return true;
            });

            final SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("last_episode_played", mEpisode.getEpisodeId());
            editor.apply();

        } catch (Exception ex) {
            Log.e(mPackage, "MediaPlayerService Service error: " + ex.toString());
        }
    }

    private void playlistSkip(final Enums.SkipDirection direction, final List<PodcastItem> episodes) {

        if (mEpisode.getPodcastId() > -1 && PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pref_episodes_continuous_play", true) == false) {
            setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            disableNoisyReceiver();
            SyncWithMobileDevice(true);
        } else if (episodes != null && episodes.size() > 2) {
            {
                int currentPosition = 0;

                if (mLocalFile != null) {
                    for (final PodcastItem playlistItem : episodes) {
                        if (playlistItem.getTitle() != null && Objects.equals(playlistItem.getTitle(), mLocalFile))
                            break;
                        currentPosition++;
                    }
                } else {
                    for (final PodcastItem playlistItem : episodes) {
                        if (playlistItem.getEpisodeId() == mEpisode.getEpisodeId()) break;
                        currentPosition++;
                    }
                }

                if (direction == Enums.SkipDirection.NEXT && currentPosition < episodes.size() - 1) //in middle
                    mEpisode = episodes.get(currentPosition + 1);
                else if (direction == Enums.SkipDirection.NEXT && currentPosition == episodes.size() - 1) //at end
                    mEpisode = episodes.get(1);
                else if (direction == Enums.SkipDirection.PREVIOUS && currentPosition == 1) //at beginning, go to end
                    mEpisode = episodes.get(episodes.size() - 1);
                else if (direction == Enums.SkipDirection.PREVIOUS && currentPosition <= episodes.size() - 1) //in middle
                    mEpisode = episodes.get(currentPosition - 1);

                String uri;

                if (GetEpisodeValue(mContext, mEpisode, "download") == 1)
                    uri = Utilities.GetMediaFile(mContext, mEpisode);
                else if (mLocalFile != null) {
                    mLocalFile = mEpisode.getTitle();
                    uri = CommonUtils.GetLocalDirectory(mContext).concat(mEpisode.getTitle());
                } else
                    uri = mEpisode.getMediaUrl().toString();

                if (mMediaPlayer != null)
                    mMediaPlayer.reset();

                //CommonUtils.writeToFile(mContext, "Next Episode: " + mEpisode.getTitle());

                StartStream(Uri.parse(uri));

                final Bundle extras = new Bundle();
                extras.putInt("id", mEpisode.getEpisodeId());
                mMediaSessionCompat.setExtras(extras);

                final MediaPlaybackStatus mpsPlaylistSkip = new MediaPlaybackStatus();
                mpsPlaylistSkip.setMediaPlaylistSkip(true);
                mpsPlaylistSkip.setEpisodeId(mEpisode.getEpisodeId());
                mpsPlaylistSkip.setPlaylistId(mPlaylistID);
                if (mLocalFile != null)
                    mpsPlaylistSkip.setLocalFile(mLocalFile);

                EventBus.getDefault().post(mpsPlaylistSkip);
            }
        } else {
            disableNoisyReceiver();
            SyncWithMobileDevice(true);
            stopForeground(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pref_remove_notification", false));

            if (mAudioManager != null)
                mAudioManager.abandonAudioFocus(this);

            if (mTelephonyManager != null)
                mTelephonyManager.listen(mPhoneState, PhoneStateListener.LISTEN_NONE);

            if (mLocalFile == null && !mEpisode.getIsDownloaded())
                Utilities.enableBluetooth(mContext);
        }
    }

    private void disableNoisyReceiver() {
        try { unregisterReceiver(mNoisyReceiver); }
        catch(Exception ignored) {}
    }

    private void initNoisyReceiver() {
        final IntentFilter filterNoisy = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mNoisyReceiver, filterNoisy);
    }

    private BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(mPackage, "MediaPlayerService Noisy receiver hit");
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                PauseAudio();
            }
        }
    };

    private void showNotification(final Boolean pause) {
        showNotification(pause, false);
    }
    private void showNotification(final Boolean pause, final boolean cancel) {

        final Bundle bundle = new Bundle();
        bundle.putInt("episodeid", mEpisode.getEpisodeId());
        bundle.putInt("playlistid", mPlaylistID);

        final Intent notificationIntent = new Intent(mContext, EpisodeActivity.class);
        notificationIntent.putExtras(bundle);

        String channelID = mPackage.concat(".playing");

        final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationManager notificationManager = getSystemService(NotificationManager.class);
            final NotificationChannel channel = new NotificationChannel(channelID, getString(R.string.notification_channel_media_playing), NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, channelID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(GetLogo(mContext, mEpisode))
                .setContentText(mLocalFile != null ? mLocalFile : mEpisode.getTitle())
                .setLocalOnly(true)
                .setWhen(0)
                .setOnlyAlertOnce(true)
                .setPriority(PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(PendingIntent.getActivity(mContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        //.setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(mContext, PlaybackStateCompat.ACTION_STOP));

        if (pause) {
            builder.setOngoing(false);
            builder.addAction(android.R.drawable.ic_media_play, mContext.getString(R.string.play), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE));
        } else {
            builder.setOngoing(true);
            builder.addAction(android.R.drawable.ic_media_pause, mContext.getString(R.string.pause), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE));
        }

        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mMediaSessionCompat.getSessionToken()).setShowActionsInCompactView(0));

        if (pause)
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
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, mLocalFile != null ? mLocalFile : mEpisode.getTitle());
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, mContext.getString(R.string.app_name_wc));
        //metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, DateUtils.FormatPositionTime(mMediaPlayer.getCurrentPosition()).concat("/").concat(String.valueOf(DateUtils.FormatPositionTime(mMediaPlayer.getDuration()))));

        if (mLocalFile != null)
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, mEpisode.getTitle());

        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, mContext.getString(R.string.app_name_wc));
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DATE, mEpisode.getPubDate());
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, mEpisode.getDescription());

        //metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1);
        //metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 1);

        if (mMediaPlayer.isPlaying()) {
            //try {metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, DateUtils.FormatPositionTime(mMediaPlayer.getDuration() - mMediaPlayer.getCurrentPosition()));}
            //catch(Exception ignored){}
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
        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), mContext.getString(R.string.app_name_wc), mediaButtonReceiver, null);
        mMediaSessionCompat.setCallback(mMediaSessionCallback);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O)
            mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

       //final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        //mediaButtonIntent.setClass(this, MediaButtonReceiver.class);

        //final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        //mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);
        mMediaSessionCompat.setMediaButtonReceiver(null);

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
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mMediaPlayer.isPlaying())
                    PauseAudio(true, false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mMediaPlayer != null)
                    mMediaPlayer.setVolume(0.3f, 0.3f);
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mMediaPlayer != null && !mMediaPlayer.isPlaying())
                    PlayAudio();
                break;
        }
    }

    final PhoneStateListener mPhoneState = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                mCallStarted = true;
                if (mMediaPlayer.isPlaying())
                    PauseAudio(false, true);
            } else if(state == TelephonyManager.CALL_STATE_IDLE) {
                if (mCallStarted && mMediaPlayer != null && !mMediaPlayer.isPlaying())
                    PlayAudio();
            } else if(state == TelephonyManager.CALL_STATE_OFFHOOK) {
                mCallStarted = true;
                PauseAudio(false, true);
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

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

            final int specifiedTime = mHasPremium ? Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(mContext).getString("pref_" + mEpisode.getPodcastId() + "_finish_end_time", String.valueOf(mContext.getResources().getInteger(R.integer.default_finish_end_time)))) * 1000 : 0;

            final int finishTime = mMediaPlayer.getDuration() - specifiedTime;

            if (position >= finishTime || (mPlaybackPosition > 0 && (mPlaybackPosition == position))) {
                mMediaHandler.removeCallbacksAndMessages(null);
                mPlaybackPosition = 0;
                mPlaybackCount = 0;
                completeMedia(false);
            } else {
                final MediaPlaybackStatus mpPosition = new MediaPlaybackStatus();
                mpPosition.setPosition(position);

                EventBus.getDefault().post(mpPosition);
                //initMediaSessionMetadata();
                if (mPlaybackCount > 10) {
                    mPlaybackPosition = position;
                    mPlaybackCount = 0;
                } else
                    mPlaybackCount++;
                mMediaHandler.postDelayed(mUpdateMediaPosition, 100);
            }
        }
    };

    private void completeMedia(final boolean playbackError)
    {
        //CommonUtils.writeToFile(mContext, "\n\n");
        setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
        mMediaPlayer.stop();

        if (!mError) {
            CommonUtils.executeSingleThreadAsync(new FinishMedia(mContext, mEpisode, mPlaylistID, mLocalFile, playbackError), (episodes) -> {
                if (episodes.size() < 3) {

                    final MediaPlaybackStatus mpsCompleted = new MediaPlaybackStatus();
                    mpsCompleted.setMediaCompleted(true);

                    EventBus.getDefault().post(mpsCompleted);
                }

                playlistSkip(Enums.SkipDirection.NEXT, episodes);
            });
        }
    }

    @Override
    public void onTaskRemoved(final Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }
}
