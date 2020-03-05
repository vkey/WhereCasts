package com.krisdb.wearcasts.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.input.WearableButtons;
import android.text.SpannableString;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.wear.widget.drawer.WearableActionDrawerView;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;

import com.krisdb.wearcasts.Adapters.NavigationAdapter;
import com.krisdb.wearcasts.Adapters.PlaylistsAssignAdapter;
import com.krisdb.wearcasts.Async.SyncPodcasts;
import com.krisdb.wearcasts.Async.ToggleBluetooth;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Models.MediaPlaybackStatus;
import com.krisdb.wearcasts.Models.NavItem;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Services.MediaPlayerService;
import com.krisdb.wearcasts.Settings.SettingsPodcastsActivity;
import com.krisdb.wearcasts.Utilities.EpisodeUtilities;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.PodcastItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisode;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodeValue;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetPlayingEpisodeID;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.markPlayed;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.markUnplayed;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.playlistIsEmpty;
import static com.krisdb.wearcastslibrary.CommonUtils.GetBackgroundLogo;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;
import static com.krisdb.wearcastslibrary.DateUtils.GetDisplayDate;

public class EpisodeActivity extends WearableActivity implements MenuItem.OnMenuItemClickListener, WearableNavigationDrawerView.OnItemSelectedListener {

    private Context mContext;
    private Activity mActivity;

    private ProgressBar mProgressBar, mProgressCircleDownloading, mProgressCircleLoading;
    private SeekBar mSeekBar, mVolumeSeekBar;
    private RelativeLayout mInfoLayout, mControlsLayout;
    private TextView mPositionView, mDurationView, mSkipBack, mSkipForward, mDownloadSpeed, mEpisodeTitle;
    private ImageView mSkipBackImage, mSkipForwardImage, mPlayPauseImage, mVolumeUp, mVolumeDown, mDownloadImage;

    private PodcastItem mEpisode;
    private MediaBrowserCompat mMediaBrowserCompat;
    private WearableActionDrawerView mWearableActionDrawer;
    private NestedScrollView mScrollView;
    private static List<NavItem> mNavItems;
    private WeakReference<EpisodeActivity> mActivityRef;
    private WearableNavigationDrawerView mNavDrawer;
    private Handler mDownloadProgressHandler = new Handler();
    private Handler mVolumeBarHandler;
    private DownloadManager mDownloadManager;

    private ConnectivityManager mManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private TimeOutHandler mTimeOutHandler;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(7);

    private String mLocalFile;
    private int mPlaylistID, mCurrentState, mThemeID, mEpisodeID;
    private long mDownloadId;
    private static final int STATE_PAUSED = 0, STATE_PLAYING = 1;
    private static boolean mDownload;
    private AlertDialog mPlaylistDialog = null;

/*    @Override
    public Resources.Theme getTheme() {
        final Resources.Theme theme = super.getTheme();

        theme.applyStyle(Utilities.getTheme(this), true);

        return theme;
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (!prefs.getBoolean("pref_display_ambient_disable", false))
            setAmbientEnabled();

        setContentView(R.layout.activity_podcast_episode);

        mContext = getApplicationContext();
        mActivity = this;
        boolean hasPremium = Utilities.hasPremium(this);
        mActivityRef = new WeakReference<>(this);

        mNavItems = Utilities.getNavItems(this);

        mThemeID = Utilities.getThemeOptionId(mActivity);

        mMediaBrowserCompat = new MediaBrowserCompat(
                mContext,
                new ComponentName(mContext, MediaPlayerService.class),
                mMediaBrowserCompatConnectionCallback,
                getIntent().getExtras()
        );

        mTimeOutHandler = new TimeOutHandler(this);
        mManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        mScrollView = findViewById(R.id.podcast_episode_scrollview);
        mEpisodeTitle = findViewById(R.id.podcast_episode_title);
        mProgressBar = findViewById(R.id.podcast_episode_progress_bar);
        mProgressCircleDownloading = findViewById(R.id.podcast_episode_progress_circle);
        mProgressCircleLoading = findViewById(R.id.podcast_episode_progress_loading);
        mDownloadImage = findViewById(R.id.ic_podcast_episode_download);
        mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_circle));

        mSeekBar = findViewById(R.id.sb_podcast_episode);
        mVolumeSeekBar = findViewById(R.id.sb_podcast_volume_bar);
        mDownloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
        mPlayPauseImage = findViewById(R.id.ic_podcast_playpause);
        mSkipBackImage = findViewById(R.id.ic_skip_back);
        mSkipForwardImage = findViewById(R.id.ic_skip_forward);
        mDownloadSpeed = findViewById(R.id.podcast_episode_download_speed);
        mVolumeDown = findViewById(R.id.ic_episode_volume_down);
        mVolumeUp = findViewById(R.id.ic_episode_volume_up);
        mDurationView = findViewById(R.id.tv_podcast_duration);
        mPositionView = findViewById(R.id.tv_podcast_position);
        mInfoLayout = findViewById(R.id.podcast_episode_info_layout);
        mControlsLayout = findViewById(R.id.podcast_episode_buttons_layout);
        mWearableActionDrawer = findViewById(R.id.drawer_action_episode);
        mSkipBack = findViewById(R.id.tv_skip_back);
        mSkipForward = findViewById(R.id.tv_skip_forward);

        mWearableActionDrawer.setOnMenuItemClickListener(this);

        if (hasPremium && prefs.getBoolean("pref_display_show_clock_playing_screen", true))
        {
            findViewById(R.id.podcast_episode_clock).setVisibility(View.VISIBLE);
            mEpisodeTitle.setPadding(0, getResources().getConfiguration().isScreenRound() ? 20 : 10, 0 ,0);
        }
        else
        {
            findViewById(R.id.podcast_episode_clock).setVisibility(View.GONE);
            mEpisodeTitle.setPadding(0, getResources().getConfiguration().isScreenRound() ? 40 : 10, 0 ,0);
        }

        final ViewGroup.MarginLayoutParams paramsLoading = (ViewGroup.MarginLayoutParams)mProgressCircleLoading.getLayoutParams();
        final ViewGroup.MarginLayoutParams paramsDownloading = (ViewGroup.MarginLayoutParams)mProgressCircleDownloading.getLayoutParams();
        final ViewGroup.MarginLayoutParams paramsDownloadImage = (ViewGroup.MarginLayoutParams)mDownloadImage.getLayoutParams();

        if (Objects.equals(CommonUtils.getDensityName(mContext), mContext.getString(R.string.hdpi)))
        {
            paramsDownloadImage.setMargins(0, 0, 14, 0);
            paramsLoading.setMargins(0, 0, 3, 0);
            paramsDownloading.setMargins(0, 0, -31, 0);
        }
        else
        {
            paramsDownloadImage.setMargins(0, 0, 13, 0);
            paramsLoading.setMargins(0, 0, 1, 0);
            paramsDownloading.setMargins(0, 0, -38, 0);
        }

        final int skipBack = hasPremium ? Integer.valueOf(prefs.getString("pref_playback_skip_back", String.valueOf(getResources().getInteger(R.integer.default_playback_skip)))) : 30;
        final int skipForward = hasPremium ? Integer.valueOf(prefs.getString("pref_playback_skip_forward", String.valueOf(getResources().getInteger(R.integer.default_playback_skip)))) : 30;

        mSkipBack.setText(String.valueOf(skipBack));
        mSkipForward.setText(String.valueOf(skipForward));

        mVolumeDown.setOnClickListener(view -> {
            final AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);

            if (audioManager != null) {
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
                mVolumeSeekBar = findViewById(R.id.sb_podcast_volume_bar);
                mVolumeSeekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                mVolumeSeekBar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                mVolumeSeekBar.setVisibility(View.VISIBLE);
                findViewById(R.id.ic_episode_volume_bar).setVisibility(View.VISIBLE);
            }
            else
                CommonUtils.showToast(mActivity, getString(R.string.alert_no_system_audio));

        });

        mVolumeUp.setOnClickListener(view -> {
            final AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);

            if (audioManager != null) {
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
                mVolumeSeekBar = findViewById(R.id.sb_podcast_volume_bar);
                mVolumeSeekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                mVolumeSeekBar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                mVolumeSeekBar.setVisibility(View.VISIBLE);
                findViewById(R.id.ic_episode_volume_bar).setVisibility(View.VISIBLE);
                            }
            else
                CommonUtils.showToast(mActivity, getString(R.string.alert_no_system_audio));
        });

        mVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int position, final boolean fromUser) {
                final AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

                if (audioManager != null) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, position, 0);

                final ImageView volumeBarImage = findViewById(R.id.ic_episode_volume_bar);

                if (position == 0)
                    volumeBarImage.setImageDrawable(getDrawable(R.drawable.ic_action_episode_volume_bar_mute));
                else
                    volumeBarImage.setImageDrawable(getDrawable(R.drawable.ic_action_episode_volume_bar));

                    if (mVolumeBarHandler != null)
                        mVolumeBarHandler.removeCallbacksAndMessages(null);

                    mVolumeBarHandler = new Handler();
                    mVolumeBarHandler.postDelayed(() ->
                            {
                                mVolumeSeekBar.setVisibility(View.GONE);
                                volumeBarImage.setVisibility(View.GONE);
                            }
                            , 2000);
                } else
                    CommonUtils.showToast(mActivity, getString(R.string.alert_no_system_audio));
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {}
        });

        /*
        mVolumeUp.setOnClickListener(view -> {
            final AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);

            if (audioManager != null)
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            else
                CommonUtils.showToast(mActivity, getString(R.string.alert_no_system_audio));
        });
         */

        mDownloadImage.setOnClickListener(view -> handleNetwork(true));

        mSkipBackImage.setOnClickListener(view -> {
            final int position = mSeekBar.getProgress() - (skipBack * 1000);
            mSeekBar.setProgress(position);
            MediaControllerCompat.getMediaController(mActivity).getTransportControls().seekTo(position);
        });

        mSkipForwardImage.setOnClickListener(view -> {
            final int position = mSeekBar.getProgress() + (skipForward * 1000);
            mSeekBar.setProgress(position);
            MediaControllerCompat.getMediaController(mActivity).getTransportControls().seekTo(position);
        });

        mPlayPauseImage.setOnClickListener(view -> togglePlayback());

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int position, final boolean fromUser) { }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                MediaControllerCompat.getMediaController(mActivity).getTransportControls().seekTo(seekBar.getProgress());
            }
        });

        final ContentValues cv = new ContentValues();
        cv.put("read", 1);

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(this);
        db.update(cv, getIntent().getExtras().getInt("episodeid"));
        db.close();

        if (WearableButtons.getButtonCount(this) != 1 && WearableButtons.getButtonCount(this) != 3)
        {
            //reset the setting for users who already enable it
            final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mActivity).edit();
            editor.putBoolean("pref_hardware_override_episode", false);
            editor.apply();
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onPause() {
        //mDownloadProgressHandler.removeCallbacks(downloadProgress);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        unregisterNetworkCallback();

        EventBus.getDefault().unregister(this);
        mDownloadProgressHandler.removeCallbacks(downloadProgress);

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        setMainMenu();

        final Intent intent = getIntent();

        if (intent != null && intent.getExtras() != null) {
            mEpisodeID = intent.getExtras().getInt("episodeid");
            mPlaylistID = intent.getExtras().getInt("playlistid");
            mLocalFile = intent.getExtras().getString("local_file");

            if (mLocalFile != null) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                mEpisode = new PodcastItem();
                mEpisode.setTitle(mLocalFile);
                mEpisode.setPosition(prefs.getInt(Utilities.GetLocalPositionKey(mLocalFile), 0));
                mEpisode.setDuration((int)prefs.getLong(Utilities.GetLocalDurationKey(mLocalFile), 0));
            } else if (mEpisodeID > -1)
                mEpisode = GetEpisode(mActivity, mEpisodeID, mPlaylistID);

            MediaBrowserConnect();

        } else
            Utilities.ShowFailureActivity(mActivity, getString(R.string.general_error));

        EventBus.getDefault().register(this);
    }

    private void MediaBrowserConnect() {

        try {
            if (mMediaBrowserCompat != null) {
                if (!mMediaBrowserCompat.isConnected())
                    mMediaBrowserCompat.connect();
                else
                    SetContent();
            } else {
                mMediaBrowserCompat = new MediaBrowserCompat(
                        mContext,
                        new ComponentName(mContext, MediaPlayerService.class),
                        mMediaBrowserCompatConnectionCallback,
                        getIntent().getExtras()
                );

                mMediaBrowserCompat.connect();
            }
        }
        catch (IllegalStateException ex)
        {
            ex.printStackTrace();
        }
    }

    private void SetContent() {

        setMenu();

        mScrollView.setBackground(GetBackgroundLogo(mActivity, mEpisode.getChannel()));

        mEpisodeTitle.setText(CommonUtils.boldText(mEpisode.getTitle()));

        if (mEpisode.getDescription() != null) {
            SpannableString description = CommonUtils.boldText(GetDisplayDate(mActivity, mEpisode.getPubDate()).concat(" - ").concat(mEpisode.getDescription()));
            ((TextView) findViewById(R.id.podcast_episode_description)).setText(description);
        }

        if (MediaControllerCompat.getMediaController(mActivity).getPlaybackState() != null &&
                MediaControllerCompat.getMediaController(mActivity).getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
            mCurrentState = STATE_PLAYING;

            final int episodePlayingID = GetPlayingEpisodeID(mContext);

            mDownloadImage.setVisibility(mEpisode.getEpisodeId() == episodePlayingID ? View.GONE : View.VISIBLE);

            if (mEpisode.getEpisodeId() == episodePlayingID) {
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
                final int position = mEpisode.getPosition();
                final int duration = mEpisode.getDuration();
                mSeekBar.setMax(duration);
                mSeekBar.setProgress(position);
                mDurationView.setText(DateUtils.FormatPositionTime(duration));
                mInfoLayout.setVisibility(View.VISIBLE);
                mVolumeDown.setVisibility(View.VISIBLE);
                mVolumeUp.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                mSkipForwardImage.setVisibility(View.VISIBLE);
                mSkipBackImage.setVisibility(View.VISIBLE);
                mSkipBack.setVisibility(View.VISIBLE);
                mSkipForward.setVisibility(View.VISIBLE);
            } else {
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
                mInfoLayout.setVisibility(View.GONE);
                mVolumeDown.setVisibility(View.GONE);
                mVolumeUp.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                mSkipForwardImage.setVisibility(View.INVISIBLE);
                mSkipBackImage.setVisibility(View.INVISIBLE);
                mSkipBack.setVisibility(View.INVISIBLE);
                mSkipForward.setVisibility(View.INVISIBLE);
            }
        } else {
            mCurrentState = STATE_PAUSED;
            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
            mInfoLayout.setVisibility(View.GONE);
            mVolumeDown.setVisibility(View.GONE);
            mVolumeUp.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.GONE);
            mSkipForwardImage.setVisibility(View.INVISIBLE);
            mSkipBackImage.setVisibility(View.INVISIBLE);
            mSkipBack.setVisibility(View.INVISIBLE);
            mSkipForward.setVisibility(View.INVISIBLE);
            mDownloadImage.setVisibility(View.VISIBLE);
        }

        if (mLocalFile != null || mEpisode.getIsDownloaded())
        {
            mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_delete2));
            mDownloadImage.setOnClickListener(view -> DeleteEpisode());
        }

        if (mLocalFile == null && mEpisode.getMediaUrl() == null) {
            mPlayPauseImage.setEnabled(false);
            //mPlayPauseImage.setText("Error");
            mPlayPauseImage.setBackgroundResource(0);
            ((TextView) findViewById(R.id.podcast_episode_error)).setText(getString(R.string.text_episode_no_media));
            findViewById(R.id.podcast_episode_error).setVisibility(View.VISIBLE);
        }

        mDownloadId = mEpisode.getDownloadId();

        if (mDownloadId > 0)
            mDownloadProgressHandler.post(downloadProgress);
    }

    private void togglePlayback()
    {
        final int playingEpisodeID = GetPlayingEpisodeID(mContext);
        final boolean isCurrentlyPlaying = mEpisode.getEpisodeId() == playingEpisodeID;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        if (playingEpisodeID > 0)
            MediaControllerCompat.getMediaController(mActivity).getTransportControls().pause();

        if (mCurrentState == STATE_PAUSED || (mCurrentState == STATE_PLAYING && !isCurrentlyPlaying))
        {
            final Bundle extras = new Bundle();
            extras.putString("local_file", mLocalFile);
            extras.putInt("playlistid", mPlaylistID);
            extras.putInt("episodeid", mEpisodeID);

            //check for downloaded episode
            if (mLocalFile != null || mEpisode.getIsDownloaded()) {

                final String uri = (mLocalFile != null) ? GetLocalDirectory(mActivity).concat(mLocalFile) : Utilities.GetMediaFile(mActivity, mEpisode);

                MediaControllerCompat.getMediaController(mActivity).getTransportControls().playFromUri(Uri.parse(uri), extras);

                mCurrentState = STATE_PLAYING;
            }
            else {
                if (playingEpisodeID > 0)
                {
                    if (prefs.getBoolean("pref_disable_bluetooth", false) && !Utilities.BluetoothEnabled()) {
                        CommonUtils.showToast(mContext, getString(R.string.alert_episode_network_waiting));
                        new CountDownTimer(10000, 1000) {
                            public void onTick(long millisUntilFinished) {
                                if (Utilities.BluetoothEnabled()) {
                                    handleNetwork(false);
                                    this.cancel();
                                }
                                else
                                    Utilities.enableBluetooth(mContext, false);
                            }

                            public void onFinish() {}
                        }.start();
                    } else
                        handleNetwork(false);
                }
                else
                    handleNetwork(false);
            }
            mSeekBar.setProgress(mEpisode.getPosition());
        }
        else
        {
            if (mLocalFile != null)
                MediaControllerCompat.getMediaController(mActivity).getTransportControls().pause();

            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
            mDownloadImage.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.INVISIBLE);
            mProgressCircleDownloading.setVisibility(View.INVISIBLE);
            mDownloadSpeed.setVisibility(View.INVISIBLE);
            mInfoLayout.setVisibility(View.GONE);
            mVolumeDown.setVisibility(View.GONE);
            mVolumeUp.setVisibility(View.GONE);
            mSkipBackImage.setVisibility(View.INVISIBLE);
            mSkipBack.setVisibility(View.INVISIBLE);
            mSkipForwardImage.setVisibility(View.INVISIBLE);
            mSkipForward.setVisibility(View.INVISIBLE);

            mCurrentState = STATE_PAUSED;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private Runnable downloadProgress = new Runnable() {
            @Override
            public void run() {
            final DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(mDownloadId);
            final Cursor cursor = mDownloadManager.query(query);

            if (cursor.moveToFirst()) {
                final int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                mProgressCircleDownloading.setMax(bytes_total);
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                final int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));

                //Log.d(mContext.getPackageName(), "Status: "  + status);
                //Log.d(mContext.getPackageName(), "Bytes: "  +bytes_total);
                switch (status) {
                    case DownloadManager.STATUS_PAUSED:
                    case DownloadManager.STATUS_RUNNING:
                    case DownloadManager.STATUS_PENDING:
                        final int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));

                        if (status == DownloadManager.STATUS_PAUSED && bytes_downloaded == 0) {
                            showFailedDownload(404);
                            mDownloadProgressHandler.removeCallbacks(downloadProgress);
                        }

                        final int dl_progress = (int) ((bytes_downloaded * 100L) / bytes_total);
                        if (dl_progress > 0) {
                            mDownloadSpeed.setText(dl_progress + "%");
                            mDownloadSpeed.setVisibility(View.VISIBLE);
                            mProgressCircleLoading.setVisibility(View.GONE);
                        }
                        else
                        {
                            mDownloadSpeed.setVisibility(View.INVISIBLE);
                            mProgressCircleLoading.setVisibility(View.VISIBLE);
                        }

                        mProgressCircleDownloading.setProgress(bytes_downloaded);
                        mProgressCircleDownloading.setVisibility(View.VISIBLE);
                        mPlayPauseImage.setVisibility(View.INVISIBLE);

                        mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_cancel));
                        mDownloadImage.setOnClickListener(view -> CancelDownload());
                        mDownloadProgressHandler.postDelayed(downloadProgress, 1000);
                        break;
                    case DownloadManager.STATUS_FAILED:
                        showFailedDownload(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON)));
                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_delete2));
                        mPlayPauseImage.setVisibility(View.VISIBLE);
                        mControlsLayout.setVisibility(View.VISIBLE);
                        mInfoLayout.setVisibility(View.GONE);
                        mVolumeDown.setVisibility(View.GONE);
                        mVolumeUp.setVisibility(View.GONE);
                        mProgressCircleDownloading.setVisibility(View.INVISIBLE);
                        mDownloadSpeed.setVisibility(View.INVISIBLE);
                        mEpisode.setIsDownloaded(true);
                        mDownloadImage.setOnClickListener(view -> DeleteEpisode());
                        mDownloadProgressHandler.removeCallbacks(downloadProgress);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        break;
                }
            }
            else
            {
                mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_circle));
                mDownloadImage.setOnClickListener(view -> DownloadEpisode());
                mPlayPauseImage.setVisibility(View.VISIBLE);
                mProgressCircleLoading.setVisibility(View.INVISIBLE);
                mProgressCircleDownloading.setVisibility(View.INVISIBLE);
                mDownloadSpeed.setVisibility(View.INVISIBLE);
                mInfoLayout.setVisibility(View.GONE);
                mControlsLayout.setVisibility(View.VISIBLE);
                mVolumeDown.setVisibility(View.GONE);
                mVolumeUp.setVisibility(View.GONE);
                mDownloadManager.remove(mDownloadId);
                mDownloadProgressHandler.removeCallbacks(downloadProgress);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                Utilities.DeleteMediaFile(mActivity, mEpisode);
                mEpisode.setIsDownloaded(false);
                SetContent();

                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    final AlertDialog.Builder alert = new AlertDialog.Builder(EpisodeActivity.this);
                    alert.setMessage(getString(R.string.alert_download_error_low));
                    alert.setNeutralButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss()).show();
                }
            }
            cursor.close();
        }
    };

    private void showFailedDownload(final int reason)
    {
        mDownloadManager.remove(mDownloadId);

        if (reason == DownloadManager.ERROR_TOO_MANY_REDIRECTS) {
            mDownloadId = EpisodeUtilities.GetDownloadIDByEpisode(mActivity, mEpisode);
            mDownloadProgressHandler.postDelayed(downloadProgress, 1000);
        }
        else
        {
            mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_circle));
            mDownloadImage.setOnClickListener(view -> DownloadEpisode());
            mPlayPauseImage.setVisibility(View.VISIBLE);
            mProgressCircleLoading.setVisibility(View.INVISIBLE);
            mProgressCircleDownloading.setVisibility(View.INVISIBLE);
            mDownloadSpeed.setVisibility(View.INVISIBLE);
            mInfoLayout.setVisibility(View.GONE);
            mControlsLayout.setVisibility(View.VISIBLE);
            mVolumeDown.setVisibility(View.GONE);
            mVolumeUp.setVisibility(View.GONE);

            CommonUtils.showToast(mActivity, Utilities.GetDownloadErrorReason(mActivity, reason));
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void DownloadEpisode()
    {
        DownloadEpisode(true);
    }

    private void DownloadEpisode(final boolean showToast) {
        mDownloadId = Utilities.startDownload(mContext, mEpisode, showToast);

        //needed for high-bandwidth check
        runOnUiThread(() -> {
            mPlayPauseImage.setVisibility(View.INVISIBLE);
            mDownloadSpeed.setVisibility(View.VISIBLE);
            mProgressCircleDownloading.setVisibility(View.VISIBLE);
            mProgressCircleDownloading.setProgress(0);
            mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_cancel));
            mDownloadImage.setOnClickListener(view -> CancelDownload());
            mProgressCircleLoading.setVisibility(View.VISIBLE);
            //showToast(mActivity, getString(R.string.alert_episode_download_start));
        });

        mDownloadProgressHandler.postDelayed(downloadProgress, 1000);
    }

    public void CancelDownload()
    {
        mDownloadProgressHandler.removeCallbacks(downloadProgress);
        mDownloadManager.remove(mDownloadId);

        mPlayPauseImage.setEnabled(true);
        mControlsLayout.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        mProgressCircleDownloading.setVisibility(View.INVISIBLE);
        mProgressCircleDownloading.setProgress(0);
        mProgressCircleLoading.setVisibility(View.INVISIBLE);
        mDownloadSpeed.setVisibility(View.INVISIBLE);
        mPlayPauseImage.setVisibility(View.VISIBLE);
        mSeekBar.setVisibility(View.GONE);
        mInfoLayout.setVisibility(View.GONE);
        mVolumeDown.setVisibility(View.GONE);
        mVolumeUp.setVisibility(View.GONE);
        Utilities.DeleteMediaFile(mActivity, mEpisode);
        mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_circle));
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mDownloadImage.setOnClickListener(view -> handleNetwork(true));
    }

    private void handleNetwork(final Boolean download) {
        mDownload = download;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (!CommonUtils.isNetworkAvailable(mActivity))
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                alert.setMessage(getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(getString(R.string.confirm_yes), (dialog, which) -> {
                    startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), mDownload ? 1 : 2);
                    dialog.dismiss();
                });

                alert.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss()).show();
            }
        }
        else if (prefs.getBoolean("initialDownload", true) && Utilities.BluetoothEnabled()) {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(EpisodeActivity.this);
                alert.setMessage(mContext.getString(R.string.confirm_initial_download_message));
                alert.setPositiveButton(mContext.getString(R.string.confirm_yes), (dialog, which) -> {
                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("pref_disable_bluetooth", true);
                    editor.apply();
                    handleNetwork(download);
                    dialog.dismiss();
                });
                alert.setNegativeButton(mContext.getString(R.string.confirm_no), (dialog, which) -> {
                    handleNetwork(download);
                    dialog.dismiss();
                }).show();

                final SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("initialDownload", false);
                editor.apply();
            }
        }
        else if (prefs.getBoolean("pref_disable_bluetooth", false) && Utilities.BluetoothEnabled() && Utilities.disableBluetooth(mContext))
        {
            unregisterNetworkCallback();

            if (!CommonUtils.isNetworkAvailable(mContext, true))
                CommonUtils.showToast(mContext, getString(R.string.alert_episode_network_waiting));

            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(final Network network) {
                    mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);
                    if (download)
                        DownloadEpisode();
                    else
                        StreamEpisode();
                }
            };

            final NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            mManager.requestNetwork(request, mNetworkCallback);

            mTimeOutHandler.sendMessageDelayed(
                    mTimeOutHandler.obtainMessage(MESSAGE_CONNECTIVITY_TIMEOUT),
                    NETWORK_CONNECTIVITY_TIMEOUT_MS);
        }
        else {
            if (download)
                DownloadEpisode();
            else
                StreamEpisode();
        }
    }

    private static class TimeOutHandler extends Handler {
        private final WeakReference<EpisodeActivity> mActivityWeakReference;

        TimeOutHandler(final EpisodeActivity activity) {
            mActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(final Message msg) {
            final EpisodeActivity activity = mActivityWeakReference.get();

            if (activity != null) {
                switch (msg.what) {
                    case MESSAGE_CONNECTIVITY_TIMEOUT:
                        if (!activity.isFinishing()) {
                            final AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                            alert.setMessage(activity.getString(R.string.alert_episode_network_notfound));
                            alert.setPositiveButton(activity.getString(R.string.confirm_yes), (dialog, which) -> {
                                activity.startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), mDownload ? 1 : 2);
                                dialog.dismiss();
                            });

                            alert.setNegativeButton(activity.getString(R.string.confirm_no), (dialog, which) -> {
                                Utilities.enableBluetooth(activity);
                                dialog.dismiss();
                            }).show();
                        }
                        activity.unregisterNetworkCallback();
                        break;
                }
            }
        }
    }

    private void unregisterNetworkCallback() {
        if (mNetworkCallback != null) {
            mManager.unregisterNetworkCallback(mNetworkCallback);
            mManager.bindProcessToNetwork(null);
            mNetworkCallback = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1)
                DownloadEpisode();
            else if (requestCode == 2)
                StreamEpisode();
        }
    }

    private void StreamEpisode()
    {
        runOnUiThread(() -> {
            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
            mInfoLayout.setVisibility(View.VISIBLE);
            mDownloadImage.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mSeekBar.setVisibility(View.GONE);
            mVolumeDown.setVisibility(View.VISIBLE);
            mVolumeUp.setVisibility(View.VISIBLE);
        });

        final Bundle extras = new Bundle();
        extras.putInt("id", mEpisode.getEpisodeId());
        extras.putString("local_file", mLocalFile);
        extras.putInt("playlistid", mPlaylistID);
        extras.putInt("episodeid", mEpisodeID);
        mCurrentState = STATE_PLAYING;

        MediaControllerCompat.getMediaController(mActivity).getTransportControls().playFromUri(Uri.parse(mEpisode.getMediaUrl().toString()), extras);
    }

    public void DeleteEpisode() {
        if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
            final AlertDialog.Builder alert = new AlertDialog.Builder(EpisodeActivity.this);
            alert.setMessage(getString(R.string.confirm_delete_episode_download));
            alert.setPositiveButton(getString(R.string.confirm_yes), (dialog, which) -> {

                if (mLocalFile != null)
                {
                    Utilities.deleteLocal(mContext, mEpisode.getTitle());
                    mDownloadImage.setVisibility(View.INVISIBLE);
                }
                else {
                    Utilities.DeleteMediaFile(mActivity, mEpisode);

                    mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_circle));
                    mDownloadImage.setOnClickListener(view -> handleNetwork(true));
                }

                dialog.dismiss();
            });

            alert.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss()).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                DownloadEpisode();
            }
        }
    }

    private MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();
            try {
                final MediaControllerCompat mcc = new MediaControllerCompat(EpisodeActivity.this, mMediaBrowserCompat.getSessionToken());
                mcc.registerCallback(mMediaControllerCompatCallback);
                MediaControllerCompat.setMediaController(mActivity, mcc);

                SetContent();
                findViewById(R.id.podcast_episode_layout).setVisibility(View.VISIBLE);

                if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pref_playback_auto_start", false) && Utilities.hasPremium(mContext) && (mEpisodeID != GetPlayingEpisodeID(mContext) || mLocalFile != null))
                    togglePlayback();

                } catch( Exception e ) {
                Log.e(mActivity.getPackageName(), e.toString());
            }
        }
    };

    private MediaControllerCompat.Callback mMediaControllerCompatCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onExtrasChanged(Bundle extras) {
            super.onExtrasChanged(extras);

            mEpisodeID = extras.getInt("id");
        }

        @Override
        public void onPlaybackStateChanged(final PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);

            if (state == null ) return;

            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING: {
                    mCurrentState = STATE_PLAYING;
                    break;
                }
                case PlaybackStateCompat.STATE_PAUSED: {
                    mCurrentState = STATE_PAUSED;
                    break;
                }
            }
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MediaPlaybackStatus status) {
        if (status.getMediaPlay())
        {
            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
            mSkipBackImage.setVisibility(View.VISIBLE);
            mSkipForwardImage.setVisibility(View.VISIBLE);
            mSkipBack.setVisibility(View.VISIBLE);
            mSkipForward.setVisibility(View.VISIBLE);
            mInfoLayout.setVisibility(View.VISIBLE);
            mDownloadImage.setVisibility(View.GONE);
            final int position = GetEpisodeValue(mActivity, mEpisode, "position");
            final int duration = mEpisode.getDuration();
            mSeekBar.setMax(duration);
            mSeekBar.setProgress(position);
        }
        else if (status.getMediaStart())
        {
            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
            mProgressBar.setVisibility(ProgressBar.VISIBLE);
            mInfoLayout.setVisibility(View.GONE);
            mVolumeDown.setVisibility(View.GONE);
            mVolumeUp.setVisibility(View.GONE);
            mSeekBar.setVisibility(View.GONE);
            mSkipForwardImage.setVisibility(View.INVISIBLE);
            mSkipBackImage.setVisibility(View.INVISIBLE);
            mDownloadImage.setVisibility(View.GONE);
            mSkipBack.setVisibility(View.INVISIBLE);
            mSkipForward.setVisibility(View.INVISIBLE);
            mDurationView.setVisibility(View.INVISIBLE);
            mPositionView.setVisibility(View.INVISIBLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else if (status.getMediaPaused())
        {
            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
            mProgressBar.setVisibility(View.GONE);
            mInfoLayout.setVisibility(View.GONE);
            mVolumeDown.setVisibility(View.GONE);
            mVolumeUp.setVisibility(View.GONE);
            mSkipBackImage.setVisibility(View.INVISIBLE);
            mSkipForwardImage.setVisibility(View.INVISIBLE);
            mSkipBack.setVisibility(View.INVISIBLE);
            mSkipForward.setVisibility(View.INVISIBLE);
            mDownloadImage.setVisibility(View.VISIBLE);
        }
        else if (status.getMediaError())
        {
            String message = getString(R.string.general_error);

            final int errorCode = status.getErrorCode();

            if (errorCode == -11)
                message = getString(R.string.error_playback_timeout);
            else if (errorCode == -15)
                message = getString(R.string.error_playback_notavailable);
            else if (errorCode == -25)
                message = getString(R.string.error_playback_lowdisk);
            else if (errorCode < 0)
                message = getString(R.string.error_playback_other);

            message = message.concat("\n(error code: ".concat(String.valueOf(errorCode).concat(")")));

            Utilities.ShowFailureActivity(mActivity, message);

            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
            mInfoLayout.setVisibility(View.GONE);
            mVolumeDown.setVisibility(View.GONE);
            mVolumeUp.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.GONE);
        }
        else if (status.getMediaCompleted())
        {
            mInfoLayout.setVisibility(View.GONE);
            mVolumeDown.setVisibility(View.GONE);
            mVolumeUp.setVisibility(View.GONE);
            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
            mSkipForwardImage.setVisibility(View.INVISIBLE);
            mSkipBackImage.setVisibility(View.INVISIBLE);
            mSkipBack.setVisibility(View.INVISIBLE);
            mSkipForward.setVisibility(View.INVISIBLE);
        }
        else if (status.getMediaPlaylistSkip())
        {
            mEpisodeID = status.getEpisodeId();
            mPlaylistID = status.getPlaylistId();

            if (status.getLocalFile() != null)
                mLocalFile = status.getLocalFile();
            else
                mEpisode = GetEpisode(mContext, mEpisodeID, mPlaylistID);

            MediaBrowserConnect();
        }
        else if (status.getMediaPlaying())
        {
            int duration;

            if (mLocalFile != null)
                duration = (int) PreferenceManager.getDefaultSharedPreferences(mContext).getLong(Utilities.GetLocalDurationKey(mLocalFile), 0);
            else
                duration = mEpisode.getDuration();

            //fix from PlayerFM sending wrong duration
            if (mEpisode.getDuration() != status.getDuration()) {
                duration = status.getDuration();
                mEpisode.setDuration(duration);
            }

            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));

            mProgressBar.setVisibility(ProgressBar.GONE);
            mInfoLayout.setVisibility(View.VISIBLE);
            mVolumeDown.setVisibility(View.VISIBLE);
            mVolumeUp.setVisibility(View.VISIBLE);

            mSeekBar.setMax(duration);
            mSeekBar.setVisibility(View.VISIBLE);
            mSkipForwardImage.setVisibility(View.VISIBLE);
            mSkipBackImage.setVisibility(View.VISIBLE);
            mSkipBack.setVisibility(View.VISIBLE);
            mSkipForward.setVisibility(View.VISIBLE);
            mDurationView.setText(DateUtils.FormatPositionTime(duration));
            mDurationView.setVisibility(View.VISIBLE);
            mPositionView.setVisibility(View.VISIBLE);
            mDownloadProgressHandler.removeCallbacks(downloadProgress);
            mDownloadSpeed.setVisibility(View.INVISIBLE);
            mProgressCircleDownloading.setVisibility(View.INVISIBLE);
            mDownloadImage.setVisibility(View.INVISIBLE);

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else if (status.getPosition() > 0) {
            mSeekBar.setProgress(status.getPosition());
            mPositionView.setText(DateUtils.FormatPositionTime(status.getPosition()));
        }
    }

    @Override
    public void onItemSelected(final int position) {
        final int id = mNavItems.get(position).getID();
        final Context ctx = this;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final SharedPreferences.Editor editor = prefs.edit();

        switch (id) {
            case 0:
                startActivity(new Intent(ctx, AddPodcastsActivity.class));
                break;
            case 1:
                editor.putBoolean("sleep_timer_running", true);
                editor.apply();
                setMainMenu();
                Utilities.StartSleepTimerJob(ctx);
                Utilities.ShowConfirmationActivity(ctx, ctx.getString(R.string.sleep_timer_started, prefs.getString("pref_sleep_timer", "0")));
                //CommonUtils.showToast(ctx, ctx.getString(R.string.sleep_timer_started, prefs.getString("pref_sleep_timer", "0")));
                break;
            case 2:
                editor.putBoolean("sleep_timer_running", false);
                editor.apply();
                setMainMenu();
                Utilities.CancelSleepTimerJob(ctx);
                Utilities.ShowConfirmationActivity(ctx, ctx.getString(R.string.sleep_timer_stopped));
                //CommonUtils.showToast(ctx, ctx.getString(R.string.sleep_timer_stopped));
                break;
            case 3:
                if (!CommonUtils.isNetworkAvailable(this))
                {
                    if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
                        alert.setMessage(getString(R.string.alert_episode_network_notfound));
                        alert.setPositiveButton(getString(R.string.confirm_yes), (dialog, which) -> {
                            startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), 1);
                            dialog.dismiss();
                        });

                        alert.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss()).show();
                    }
                }
                else
                    CommonUtils.executeSingleThreadAsync(new SyncPodcasts(mActivityRef.get(), 0), (response) -> { });

                break;
            case 4:
                startActivity(new Intent(ctx, SettingsPodcastsActivity.class));
                break;
        }
    }

    private void setMainMenu() {
        mNavItems = Utilities.getNavItems(this);

        mNavDrawer = findViewById(R.id.drawer_nav_episode);
        mNavDrawer.setAdapter(new NavigationAdapter(this, mNavItems));
        mNavDrawer.addOnItemSelectedListener(this);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        mEpisodeTitle.setTextColor(ContextCompat.getColor(this, R.color.wc_text));

        //((ImageView)findViewById(R.id.ic_podcast_playpause)).setColorFilter(getColor(R.color.wc_ambient_playpause_on), PorterDuff.Mode.SRC_IN);

        mScrollView.setBackgroundColor(getColor(R.color.wc_background_amoled));
        mWearableActionDrawer.getController().closeDrawer();
        mNavDrawer.getController().closeDrawer();
        mScrollView.fullScroll(ScrollView.FOCUS_UP);

        if (mThemeID == Enums.ThemeOptions.DARK.getThemeId()) {
            findViewById(R.id.podcast_episode_layout).setBackgroundColor(getColor(R.color.wc_background_amoled));
            findViewById(R.id.drawer_layout).setBackgroundColor(getColor(R.color.wc_background_amoled));
            findViewById(R.id.podcast_episode_buttons_layout).setBackgroundColor(getColor(R.color.wc_background_amoled));
            findViewById(R.id.podcast_episode_info_layout).setBackgroundColor(getColor(R.color.wc_background_amoled));
            mEpisodeTitle.setBackgroundColor(getColor(R.color.wc_background_amoled));
        }
        //mLogo.setVisibility(View.INVISIBLE);
        mSkipForward.setVisibility(View.INVISIBLE);
        mSkipForwardImage.setVisibility(View.INVISIBLE);
        mSkipBackImage.setVisibility(View.INVISIBLE);
        mSkipBack.setVisibility(View.INVISIBLE);
        mDownloadImage.setVisibility(View.INVISIBLE);
        mVolumeDown.setVisibility(View.INVISIBLE);
        mVolumeUp.setVisibility(View.INVISIBLE);
        mInfoLayout.setVisibility(View.INVISIBLE);
        findViewById(R.id.podcast_episode_description).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        if (mThemeID == Enums.ThemeOptions.DEFAULT.getThemeId())
            mScrollView.setBackgroundColor(getColor(R.color.wc_transparent));

        if (mThemeID == Enums.ThemeOptions.DARK.getThemeId()) {
            mScrollView.setBackgroundColor(getColor(R.color.wc_background_dark));
            findViewById(R.id.podcast_episode_layout).setBackgroundColor(getColor(R.color.wc_background_dark));
            findViewById(R.id.drawer_layout).setBackgroundColor(getColor(R.color.wc_background_dark));
            findViewById(R.id.podcast_episode_buttons_layout).setBackgroundColor(getColor(R.color.wc_background_dark));
            findViewById(R.id.podcast_episode_info_layout).setBackgroundColor(getColor(R.color.wc_background_dark));
            mEpisodeTitle.setBackgroundColor(getColor(R.color.wc_background_dark));
        }

        mSkipForward.setVisibility(View.VISIBLE);
        mSkipForwardImage.setVisibility(View.VISIBLE);
        mSkipBackImage.setVisibility(View.VISIBLE);
        mSkipBack.setVisibility(View.VISIBLE);
        mDownloadImage.setVisibility(View.VISIBLE);
        mInfoLayout.setVisibility(View.VISIBLE);
        findViewById(R.id.podcast_episode_description).setVisibility(View.VISIBLE);

        if (mEpisode.getEpisodeId() == GetPlayingEpisodeID(mContext))
            mVolumeUp.setVisibility(View.VISIBLE);

        SetContent();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        final int itemId = menuItem.getItemId();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        switch (itemId) {
            case R.id.menu_drawer_episode_bluetooth_disable:
                CommonUtils.executeAsync(new ToggleBluetooth(mActivity, true), (response) -> {
                    CommonUtils.showToast(this, getString(R.string.alert_disable_bluetooth_disabled_end));
                    setMenu();
                });
                break;
            case R.id.menu_drawer_episode_bluetooth_enable:
                CommonUtils.executeAsync(new ToggleBluetooth(mActivity, false), (response) -> {
                    CommonUtils.showToast(this, getString(R.string.alert_disable_bluetooth_enabled));
                    setMenu();
                });
                break;
  /*          case R.id.menu_drawer_episode_open_wifi:
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                break;
            case R.id.menu_drawer_episode_open_bluetooth:
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                break;*/
            case R.id.menu_drawer_episode_markunplayed:
                markUnplayed(mContext, mEpisode);
                mEpisode = GetEpisode(mActivity, mEpisodeID, mPlaylistID);
                setMenu();
                break;
            case R.id.menu_drawer_episode_markplayed:
                if (mEpisode.getIsDownloaded())
                {
                    mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_circle));
                    mDownloadImage.setOnClickListener(view -> handleNetwork(true));
                }
                markPlayed(mContext, mEpisode);
                mEpisode = GetEpisode(mActivity, mEpisodeID, mPlaylistID);
                setMenu();
                break;
            case R.id.menu_drawer_episode_skip_start_time:
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putString("pref_" + mEpisode.getPodcastId() + "_skip_start_time", String.valueOf(mSeekBar.getProgress()/1000));
                Utilities.ShowConfirmationActivity(mActivity, getString(R.string.saved));
                if (!prefs.getBoolean("skip_start_end_time_dialog_shown", false))
                {
                    CommonUtils.showToast(mActivity, getString(R.string.skip_start_end_time_dialog_shown));
                    editor.putBoolean("skip_start_end_time_dialog_shown", true);
                }
                editor.apply();
                break;
            case R.id.menu_drawer_episode_skip_finish_time:
                final SharedPreferences.Editor editor2 = prefs.edit();
                editor2.putString("pref_" + mEpisode.getPodcastId() + "_finish_end_time", String.valueOf((mSeekBar.getMax() -  mSeekBar.getProgress()) / 1000));
                Utilities.ShowConfirmationActivity(mActivity, getString(R.string.saved));
                if (!prefs.getBoolean("skip_start_end_time_dialog_shown", false))
                {
                    CommonUtils.showToast(mActivity, getString(R.string.skip_start_end_time_dialog_shown));
                    editor2.putBoolean("skip_start_end_time_dialog_shown", true);
                }
                editor2.apply();
                break;
            case R.id.menu_drawer_episode_add_playlist:
                final View playlistAddView = getLayoutInflater().inflate(R.layout.episode_add_playlist, null);
                final AlertDialog.Builder builder = new AlertDialog.Builder(EpisodeActivity.this);
                builder.setView(playlistAddView);

                final List<PlaylistItem> playlistItems = getPlaylists(mActivity);
                final Spinner spinner = playlistAddView.findViewById(R.id.episode_add_playlist_list);

                if (playlistItems.size() == 0) {
                    playlistAddView.findViewById(R.id.episode_add_playlist_empty).setVisibility(View.VISIBLE);
                    playlistAddView.findViewById(R.id.episode_add_playlist_list).setVisibility(View.GONE);
                }
                else
                {
                    final PlaylistItem playlistEmpty = new PlaylistItem();
                    playlistEmpty.setID(mActivity.getResources().getInteger(R.integer.default_playlist_select));
                    playlistEmpty.setName(getString(R.string.dropdown_playlist_select));
                    playlistItems.add(0, playlistEmpty);

                    spinner.setAdapter(new PlaylistsAssignAdapter(this, playlistItems));
                }

                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        final PlaylistItem playlist = (PlaylistItem)parent.getSelectedItem();

                        if (playlist.getID() != getResources().getInteger(R.integer.default_playlist_select)) {
                            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

                            if (prefs.getBoolean("pref_hide_empty_playlists", false) && playlistIsEmpty(mActivity, playlist.getID()))
                            {
                                final SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("refresh_vp", true);
                                editor.apply();
                            }

                            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                            db.addEpisodeToPlaylist(playlist.getID(), mEpisode.getEpisodeId());
                            db.close();

                            Utilities.ShowConfirmationActivity(mActivity, mActivity.getString(R.string.alert_episode_playlist_added, playlist.getName()));

                            //showToast(mActivity, mActivity.getString(R.string.alert_episode_playlist_added, playlist.getName()));
                            mPlaylistDialog.dismiss();
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {}
                });

                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    mPlaylistDialog = builder.show();
                }
                break;
        }

        mWearableActionDrawer.getController().closeDrawer();

        return true;
    }

    private void setMenu()
    {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        final Menu menu = mWearableActionDrawer.getMenu();
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_drawer_episode, menu);

       if (adapter != null)
            menu.removeItem(adapter.isEnabled() ? R.id.menu_drawer_episode_bluetooth_enable : R.id.menu_drawer_episode_bluetooth_disable);
        else
        {
            menu.removeItem(R.id.menu_drawer_episode_bluetooth_enable);
            menu.removeItem(R.id.menu_drawer_episode_bluetooth_disable);
            //menu.removeItem(R.id.menu_drawer_episode_open_bluetooth);
        }

        menu.removeItem(mEpisode.getFinished() ? R.id.menu_drawer_episode_markplayed : R.id.menu_drawer_episode_markunplayed);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (PreferenceManager.getDefaultSharedPreferences(mActivity).getBoolean("pref_hardware_override_episode", false)) {
            if (event.getRepeatCount() == 0) {
                if (WearableButtons.getButtonCount(this) == 1)
                    togglePlayback();
                else {
                    if (keyCode == KeyEvent.KEYCODE_STEM_2) {
                        final AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);

                        if (audioManager != null)
                            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
                        else
                            CommonUtils.showToast(mActivity, getString(R.string.alert_no_system_audio));
                        //final int position = mSeekBar.getProgress() + getResources().getInteger(R.integer.skip_seconds) * 1000;
                        //if (position < mSeekBar.getMax())
                        //MediaControllerCompat.getMediaController(mActivity).getTransportControls().seekTo(position);
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_STEM_1) {
                        final AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);

                        if (audioManager != null)
                            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
                        else
                            CommonUtils.showToast(mActivity, getString(R.string.alert_no_system_audio));

                        //final int position = mSeekBar.getProgress() - getResources().getInteger(R.integer.skip_seconds) * 1000;
                        //if (position > 0)
                        //MediaControllerCompat.getMediaController(mActivity).getTransportControls().seekTo(position);
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_STEM_3) {
                        togglePlayback();
                        return true;
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}