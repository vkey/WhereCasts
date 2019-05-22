package com.krisdb.wearcasts.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.wear.widget.drawer.WearableActionDrawerView;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;

import com.krisdb.wearcasts.Adapters.NavigationAdapter;
import com.krisdb.wearcasts.Adapters.PlaylistsAssignAdapter;
import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
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
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisode;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodeValue;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetPlayingEpisode;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.markPlayed;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.markUnplayed;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.playlistIsEmpty;
import static com.krisdb.wearcastslibrary.CommonUtils.GetBackgroundLogo;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;
import static com.krisdb.wearcastslibrary.DateUtils.GetDisplayDate;

public class EpisodeActivity extends WearableActivity implements MenuItem.OnMenuItemClickListener, WearableNavigationDrawerView.OnItemSelectedListener {

    private Context mContext;
    private Activity mActivity;

    private ProgressBar mProgressBar, mProgressCircleDownloading, mProgressCircleLoading;
    private SeekBar mSeekBar;
    private RelativeLayout mInfoLayout, mControlsLayout;
    private TextView mPositionView, mDurationView, mSkipBack, mSkipForward, mDownloadSpeed, mEpisodeTitle;
    private ImageView mSkipBackImage, mSkipForwardImage, mPlayPauseImage, mVolumeUp, mDownloadImage;

    private PodcastItem mEpisode;
    private MediaBrowserCompat mMediaBrowserCompat;
    private WearableActionDrawerView mWearableActionDrawer;
    private NestedScrollView mScrollView;
    private static List<NavItem> mNavItems;
    private WeakReference<EpisodeActivity> mActivityRef;
    private WearableNavigationDrawerView mNavDrawer;
    private Handler mDownloadProgressHandler = new Handler();
    private DownloadManager mDownloadManager;

    private ConnectivityManager mManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private TimeOutHandler mTimeOutHandler;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(7);

    private String mLocalFile;
    private int mPlaylistID, mCurrentState, mThemeID, mEpisodeID, mPodcastID;
    private long mDownloadId, mDownloadStartTime;
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

        mActivityRef = new WeakReference<>(this);

        mNavItems = Utilities.getNavItems(this);

        mNavDrawer = findViewById(R.id.drawer_nav_episode);
        mNavDrawer.setAdapter(new NavigationAdapter(this, mNavItems));
        mNavDrawer.addOnItemSelectedListener(this);

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
        mDownloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
        mPlayPauseImage = findViewById(R.id.ic_podcast_playpause);
        mSkipBackImage = findViewById(R.id.ic_skip_back);
        mSkipForwardImage = findViewById(R.id.ic_skip_forward);
        mDownloadSpeed = findViewById(R.id.podcast_episode_download_speed);
        mVolumeUp = findViewById(R.id.ic_volume_up);
        mDurationView = findViewById(R.id.tv_podcast_duration);
        mPositionView = findViewById(R.id.tv_podcast_position);
        mInfoLayout = findViewById(R.id.podcast_episode_info_layout);
        mControlsLayout = findViewById(R.id.podcast_episode_buttons_layout);
        mWearableActionDrawer = findViewById(R.id.drawer_action_episode);
        mSkipBack = findViewById(R.id.tv_skip_back);
        mSkipForward = findViewById(R.id.tv_skip_forward);

        mWearableActionDrawer.setOnMenuItemClickListener(this);

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

        final int skipBack = Integer.valueOf(prefs.getString("pref_playback_skip_back", String.valueOf(getResources().getInteger(R.integer.default_playback_skip))));
        final int skipForward = Integer.valueOf(prefs.getString("pref_playback_skip_forward", String.valueOf(getResources().getInteger(R.integer.default_playback_skip))));

        mSkipBack.setText(String.valueOf(skipBack));
        mSkipForward.setText(String.valueOf(skipForward));

        mVolumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);

                if (audioManager != null)
                    audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                else
                    CommonUtils.showToast(mActivity, getString(R.string.alert_no_system_audio));
            }
        });

        mDownloadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleNetwork(true);
            }
        });

        mSkipBackImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int position = mSeekBar.getProgress() - (skipBack * 1000);
                mSeekBar.setProgress(position);
                MediaControllerCompat.getMediaController(mActivity).getTransportControls().seekTo(position);
            }
        });

        mSkipForwardImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int position = mSeekBar.getProgress() + (skipForward * 1000);
                mSeekBar.setProgress(position);
                MediaControllerCompat.getMediaController(mActivity).getTransportControls().seekTo(position);
            }
        });

        mPlayPauseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePlayback();
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int position, final boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
            }

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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        try {
            if (intent.getExtras() != null)
            {
                mEpisodeID = intent.getExtras().getInt("episodeid");
                mPlaylistID = -1;
                SetContent();
            }

        } catch (Exception ex) {
            CommonUtils.showToast(this, getString(R.string.general_error));
            ex.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

       if (mMediaBrowserCompat != null && mMediaBrowserCompat.isConnected() == false) {
           try {
               mMediaBrowserCompat.connect();
           }
           catch (Exception ex) {
               mMediaBrowserCompat.disconnect();
           }
       }
        else {
           SetContent();
       }
    }

    private void SetContent() {

        if (mLocalFile != null) {
            mEpisode = new PodcastItem();
            mEpisode.setTitle(mLocalFile);
        }
        else if (mEpisodeID > -1)
            mEpisode = GetEpisode(mActivity, mEpisodeID, mPlaylistID);

        setMenu();

        mScrollView.setBackground(GetBackgroundLogo(mActivity, mEpisode.getChannel()));

        mEpisodeTitle.setText(CommonUtils.boldText(mEpisode.getTitle()));

        if (mEpisode.getDescription() != null) {
            SpannableString description = CommonUtils.boldText(GetDisplayDate(mActivity, mEpisode.getPubDate()).concat(" - ").concat(CommonUtils.CleanDescription(mEpisode.getDescription())));
            ((TextView) findViewById(R.id.podcast_episode_description)).setText(description);
        }

        if (MediaControllerCompat.getMediaController(mActivity).getPlaybackState() != null &&
                MediaControllerCompat.getMediaController(mActivity).getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
            mCurrentState = STATE_PLAYING;

            if (mEpisode.getEpisodeId() == GetPlayingEpisode(mContext).getEpisodeId())
                mDownloadImage.setVisibility(View.GONE);
            else
                mDownloadImage.setVisibility(View.VISIBLE);

            if (mEpisode.getEpisodeId() == GetPlayingEpisode(mContext).getEpisodeId()) {
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
                final int position = GetEpisodeValue(mActivity, mEpisode, "position");
                final int duration = GetEpisodeValue(mActivity, mEpisode, "duration");
                mSeekBar.setMax(duration);
                mSeekBar.setProgress(position);
                mDurationView.setText(DateUtils.FormatPositionTime(duration));
                mInfoLayout.setVisibility(View.VISIBLE);
                mVolumeUp.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                mSkipForwardImage.setVisibility(View.VISIBLE);
                mSkipBackImage.setVisibility(View.VISIBLE);
                mSkipBack.setVisibility(View.VISIBLE);
                mSkipForward.setVisibility(View.VISIBLE);
            } else {
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
                mInfoLayout.setVisibility(View.GONE);
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
            mVolumeUp.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.GONE);
            mSkipForwardImage.setVisibility(View.INVISIBLE);
            mSkipBackImage.setVisibility(View.INVISIBLE);
            mSkipBack.setVisibility(View.INVISIBLE);
            mSkipForward.setVisibility(View.INVISIBLE);
            mDownloadImage.setVisibility(View.VISIBLE);
        }

        if (mLocalFile != null || GetEpisodeValue(mActivity, mEpisode, "download") == 1)
        {
            mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_delete2));
            mDownloadImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DeleteEpisode();
                }
            });
        }

        if (mLocalFile == null && mEpisode.getMediaUrl() == null) {
            mPlayPauseImage.setEnabled(false);
            //mPlayPauseImage.setText("Error");
            mPlayPauseImage.setBackgroundResource(0);
            ((TextView) findViewById(R.id.podcast_episode_error)).setText(getString(R.string.text_episode_no_media));
            findViewById(R.id.podcast_episode_error).setVisibility(View.VISIBLE);
        }

        mDownloadId = GetEpisodeValue(mActivity, mEpisode, "downloadid");

        if (mDownloadId > 0)
            downloadProgress.run();

        try {
            LocalBroadcastManager.getInstance(this).registerReceiver(mMediaReceiver, new IntentFilter("media_action"));
            LocalBroadcastManager.getInstance(this).registerReceiver(mMediaPositionReceiver, new IntentFilter("media_position"));
        }
        catch(Exception ignored){}
    }

    private void togglePlayback()
    {
        mWearableActionDrawer.setEnabled(true);

        final boolean isCurrentlyPlaying = mEpisode.getEpisodeId() == GetPlayingEpisode(mContext).getEpisodeId();

        if (mCurrentState == STATE_PAUSED || (mCurrentState == STATE_PLAYING && !isCurrentlyPlaying)) { //play episode
            final Bundle extras = new Bundle();
            extras.putInt("id", mEpisode.getEpisodeId());
            extras.putString("local_file", mLocalFile);
            extras.putInt("playlistid", mPlaylistID);
            extras.putInt("episodeid", mEpisodeID);
            extras.putInt("podcastid", mPodcastID);
            //check for downloaded episode
            if (mLocalFile != null || GetEpisodeValue(mActivity, mEpisode, "download") == 1) {

                final String uri = (mLocalFile != null) ? GetLocalDirectory(mActivity).concat(mLocalFile) : Utilities.GetMediaFile(mActivity, mEpisode);

                MediaControllerCompat.getMediaController(mActivity).getTransportControls().playFromUri(Uri.parse(uri), extras);

                mCurrentState = STATE_PLAYING;
            }
            else
                handleNetwork(false);

            int position = GetEpisodeValue(getApplicationContext(), mEpisode, "position");
            mSeekBar.setProgress(position);
        }
        else
        {
            //pause episode
            MediaControllerCompat.getMediaController(mActivity).getTransportControls().pause();

            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
            mDownloadImage.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.INVISIBLE);
            mProgressCircleDownloading.setVisibility(View.INVISIBLE);
            mDownloadSpeed.setVisibility(View.INVISIBLE);
            mInfoLayout.setVisibility(View.GONE);
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
        //mBroadcastManger.unregisterReceiver(mMediaReceiver);
    }

    @Override
    public void onPause() {
        //mDownloadProgressHandler.removeCallbacks(downloadProgress);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        unregisterNetworkCallback();
        super.onPause();
    }

    private Runnable downloadProgress = new Runnable() {
            @Override
            public void run() {
            final DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(mDownloadId);
            final Cursor cursor = mDownloadManager.query(query);
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            if (mDownloadStartTime == 0)
                mDownloadStartTime = System.nanoTime();

            if (cursor.moveToFirst()) {
                final int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                mProgressCircleDownloading.setMax(bytes_total);

                //mProgressCircleDownloading.setSecondaryProgress(bytes_total);

                final int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                //final int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));

                //Log.d(mContext.getPackageName(), "Status: " + status);
                //Log.d(mContext.getPackageName(), "Bytes: "  +bytes_total);

                switch (status) {
                    case DownloadManager.STATUS_PAUSED:
                    case DownloadManager.STATUS_RUNNING:
                    case DownloadManager.STATUS_PENDING:
                        final int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                        try {
                            if (bytes_downloaded > 0 && mDownloadStartTime > 0 && (System.nanoTime() - mDownloadStartTime) > 0) {
                                final float bytesPerSec = bytes_downloaded / ((System.nanoTime() - mDownloadStartTime) / 1000000000);

                                if (bytesPerSec < 1000000.0) {
                                    if (bytesPerSec / 1024 == 0.00)
                                        mProgressCircleLoading.setVisibility(View.VISIBLE);
                                    else {
                                        mDownloadSpeed.setText(String.format("%.02f", bytesPerSec / 1024, Locale.US).concat(" KB/s"));
                                        mDownloadSpeed.setVisibility(View.VISIBLE);
                                        mProgressCircleLoading.setVisibility(View.GONE);
                                    }
                                } else {
                                    mDownloadSpeed.setText((String.format("%.02f", (bytesPerSec / 1024) / 1024, Locale.US)).concat(" MB/s"));
                                    mProgressCircleLoading.setVisibility(View.GONE);
                                }
                            } else
                                mDownloadSpeed.setVisibility(View.INVISIBLE);
                        }
                        catch (Exception ex)
                        {
                            mDownloadSpeed.setVisibility(View.INVISIBLE);
                            if (mDownloadStartTime == 0)
                                mDownloadStartTime = System.nanoTime();
                        }

                        mProgressCircleDownloading.setProgress(bytes_downloaded);
                        mProgressCircleDownloading.setVisibility(View.VISIBLE);
                        mPlayPauseImage.setVisibility(View.INVISIBLE);

                        mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_cancel));
                        mDownloadImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                CancelDownload();
                            }
                        });
                        mDownloadProgressHandler.postDelayed(downloadProgress, 1000);
                        break;
                    case DownloadManager.STATUS_FAILED:
                        mPlayPauseImage.setVisibility(View.VISIBLE);
                        mProgressCircleDownloading.setVisibility(View.INVISIBLE);
                        mDownloadSpeed.setVisibility(View.INVISIBLE);
                        mInfoLayout.setVisibility(View.GONE);
                        mControlsLayout.setVisibility(View.VISIBLE);
                        mVolumeUp.setVisibility(View.GONE);
                        mDownloadManager.remove(mDownloadId);
                        if (prefs.getBoolean("pref_downloads_restart_on_failure", true)) {
                            SystemClock.sleep(200);
                            mDownloadId = EpisodeUtilities.GetDownloadIDByEpisode(mContext, mEpisode);
                            mDownloadProgressHandler.postDelayed(downloadProgress, 1000);
                            mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_cancel));
                            mDownloadImage.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    CancelDownload();
                                }
                            });
                            mDownloadStartTime = System.nanoTime();
                        }
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_delete2));
                        mPlayPauseImage.setVisibility(View.VISIBLE);
                        mControlsLayout.setVisibility(View.VISIBLE);
                        mInfoLayout.setVisibility(View.GONE);
                        mVolumeUp.setVisibility(View.GONE);
                        mProgressCircleDownloading.setVisibility(View.INVISIBLE);
                        mDownloadSpeed.setVisibility(View.INVISIBLE);
                        mEpisode.setIsDownloaded(true);
                        mDownloadImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                DeleteEpisode();
                            }
                        });
                        mDownloadProgressHandler.removeCallbacksAndMessages(downloadProgress);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        break;
                }
            }
            cursor.close();
        }
    };

    private void DownloadEpisode() {
        mDownloadId = Utilities.startDownload(mContext, mEpisode);

        //needed for high-bandwidth check
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPlayPauseImage.setVisibility(View.INVISIBLE);
                mDownloadSpeed.setVisibility(View.VISIBLE);
                mProgressCircleDownloading.setVisibility(View.VISIBLE);
                mProgressCircleDownloading.setProgress(0);
                mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_cancel));
                mDownloadImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CancelDownload();
                    }
                });
                mProgressCircleLoading.setVisibility(View.VISIBLE);
                //showToast(mActivity, getString(R.string.alert_episode_download_start));
            }
        });

        mDownloadStartTime = System.nanoTime();
        mDownloadProgressHandler.postDelayed(downloadProgress, 1000);
    }

    public void CancelDownload()
    {
        mDownloadProgressHandler.removeCallbacksAndMessages(downloadProgress);

        mPlayPauseImage.setEnabled(true);
        mControlsLayout.setVisibility(View.VISIBLE);
        mDownloadManager.remove(mDownloadId);
        mProgressBar.setVisibility(View.GONE);
        mProgressCircleDownloading.setVisibility(View.INVISIBLE);
        mProgressCircleDownloading.setProgress(0);
        mProgressCircleLoading.setVisibility(View.INVISIBLE);
        mDownloadSpeed.setVisibility(View.INVISIBLE);
        mPlayPauseImage.setVisibility(View.VISIBLE);
        mSeekBar.setVisibility(View.GONE);
        mInfoLayout.setVisibility(View.GONE);
        mVolumeUp.setVisibility(View.GONE);
        Utilities.DeleteMediaFile(mActivity, mEpisode);
        mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_circle));
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mDownloadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleNetwork(true);
            }
        });

        final ContentValues cv = new ContentValues();
        cv.put("download", 0);
        cv.put("downloadid", 0);

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
        db.update(cv, mEpisode.getEpisodeId());
        db.close();

        //if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pref_disable_bluetooth", false) && !Utilities.BluetoothEnabled())
            //Utilities.enableBlutooth(mContext);
    }

    private void handleNetwork(final Boolean download) {
        mDownload = download;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (CommonUtils.getActiveNetwork(mActivity) == null)
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                alert.setMessage(getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), mDownload ? 1 : 2);
                        dialog.dismiss();
                    }
                });

                alert.setNegativeButton(getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        }
        else if (prefs.getBoolean("initialDownload", true) && Utilities.BluetoothEnabled()) {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(EpisodeActivity.this);
                alert.setMessage(mContext.getString(R.string.confirm_initial_download_message));
                alert.setPositiveButton(mContext.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("pref_disable_bluetooth", true);
                        editor.apply();
                        handleNetwork(download);
                        dialog.dismiss();
                    }
                });
                alert.setNegativeButton(mContext.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handleNetwork(download);
                        dialog.dismiss();
                    }
                }).show();

                final SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("initialDownload", false);
                editor.apply();
            }
        }
        else if (prefs.getBoolean("pref_disable_bluetooth", false) && Utilities.BluetoothEnabled() && Utilities.disableBluetooth(mContext))
        {
            unregisterNetworkCallback();

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
                            alert.setPositiveButton(activity.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), mDownload ? 1 : 2);
                                    dialog.dismiss();
                                }
                            });

                            alert.setNegativeButton(activity.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Utilities.enableBluetooth(activity);
                                    dialog.dismiss();
                                }
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
        runOnUiThread(new Runnable() {
            public void run(){
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
                mInfoLayout.setVisibility(View.VISIBLE);
                mDownloadImage.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
                mSeekBar.setVisibility(View.GONE);
                //mVolumeDown.setVisibility(View.VISIBLE);
                mVolumeUp.setVisibility(View.VISIBLE);
            }
        });

        final Bundle extras = new Bundle();
        extras.putInt("id", mEpisode.getEpisodeId());
        extras.putString("local_file", mLocalFile);
        extras.putInt("playlistid", mPlaylistID);
        extras.putInt("podcastid", mPodcastID);
        extras.putInt("episodeid", mEpisodeID);
        mCurrentState = STATE_PLAYING;

        MediaControllerCompat.getMediaController(mActivity).getTransportControls().playFromUri(Uri.parse(mEpisode.getMediaUrl().toString()), extras);
    }

    public void DeleteEpisode() {
        if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
            final AlertDialog.Builder alert = new AlertDialog.Builder(EpisodeActivity.this);
            alert.setMessage(getString(R.string.confirm_delete_episode_download));
            alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                    if (mLocalFile != null)
                    {
                        Utilities.deleteLocal(mContext, mEpisode.getTitle());
                        mDownloadImage.setVisibility(View.INVISIBLE);
                    }
                    else {
                        Utilities.DeleteMediaFile(mActivity, mEpisode);

                        mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download));
                        mDownloadImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                handleNetwork(true);
                            }
                        });
                    }

                    dialog.dismiss();
                }
            });

            alert.setNegativeButton(getString(R.string.confirm_no), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).show();
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

                final Bundle extras = getIntent().getExtras();

                mPlaylistID = extras.getInt("playlistid");
                mPodcastID = extras.getInt("podcastid");
                mLocalFile = extras.getString("local_file");
                mEpisodeID = extras.getInt("episodeid");

                SetContent();
                findViewById(R.id.podcast_episode_layout).setVisibility(View.VISIBLE);

                } catch( RemoteException e ) {
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

    private BroadcastReceiver mMediaPositionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Bundle extras = intent.getExtras();

            final int position = extras.getInt("position");
            //Log.d(mContext.getPackageName(), "Position: " + position);
            mSeekBar.setProgress(position);
            mPositionView.setText(DateUtils.FormatPositionTime(position));
        }
    };

    private BroadcastReceiver mMediaReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final Bundle extras = intent.getExtras();

            //Log.d(mContext.getPackageName(), extras.getBoolean("media_start") ? "media_start" : "");
            //Log.d(mContext.getPackageName(), "Media played: " + extras.getBoolean("media_played"));

            if (extras.getBoolean("media_start")) {
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                mInfoLayout.setVisibility(View.GONE);
                //mVolumeDown.setVisibility(View.GONE);
                mVolumeUp.setVisibility(View.GONE);
                mSeekBar.setVisibility(View.GONE);
                mSkipForwardImage.setVisibility(View.INVISIBLE);
                mSkipBackImage.setVisibility(View.INVISIBLE);
                mDownloadImage.setVisibility(View.GONE);
                mSkipBack.setVisibility(View.INVISIBLE);
                mSkipForward.setVisibility(View.INVISIBLE);
                mDurationView.setVisibility(View.INVISIBLE);
                mPositionView.setVisibility(View.INVISIBLE);
                mWearableActionDrawer.setEnabled(false);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            else if (extras.getBoolean("media_paused"))
            {
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
                //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_play_dark : R.drawable.ic_action_episode_play));
                mProgressBar.setVisibility(View.GONE);
                mInfoLayout.setVisibility(View.GONE);
                //mVolumeDown.setVisibility(View.GONE);
                mVolumeUp.setVisibility(View.GONE);
                mSkipBackImage.setVisibility(View.INVISIBLE);
                mSkipForwardImage.setVisibility(View.INVISIBLE);
                mSkipBack.setVisibility(View.INVISIBLE);
                mSkipForward.setVisibility(View.INVISIBLE);
                mDownloadImage.setVisibility(View.VISIBLE);
            }
            else if (extras.getBoolean("media_played"))
            {
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
                //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_pause_dark : R.drawable.ic_action_episode_pause));
                mSkipBackImage.setVisibility(View.VISIBLE);
                mSkipForwardImage.setVisibility(View.VISIBLE);
                mSkipBack.setVisibility(View.VISIBLE);
                mSkipForward.setVisibility(View.VISIBLE);
                mInfoLayout.setVisibility(View.VISIBLE);
                mDownloadImage.setVisibility(View.GONE);
                final int position = GetEpisodeValue(mActivity, mEpisode, "position");
                final int duration = GetEpisodeValue(mActivity, mEpisode, "duration");
                mSeekBar.setMax(duration);
                mSeekBar.setProgress(position);
            }
            else if (extras.getBoolean("media_error"))
            {
                String message = getString(R.string.general_error);

                final int errorCode = extras.getInt("error_code");

                if (errorCode == -11)
                    message = getString(R.string.error_playback_timeout);
                else if (errorCode == -15)
                    message = getString(R.string.error_playback_notavailable);
                else if (errorCode == -25)
                    message = getString(R.string.error_playback_lowdisk);
                else if (errorCode < 0)
                    message = getString(R.string.error_playback_other);

                message = message.concat("\n(error code: ".concat(String.valueOf(errorCode).concat(")")));

                showToast(mActivity, message);

                //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_play_dark : R.drawable.ic_action_episode_play));
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
                mInfoLayout.setVisibility(View.GONE);
                //mVolumeDown.setVisibility(View.GONE);
                mVolumeUp.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
            }
            else if (extras.getBoolean("media_completed"))
            {
                mInfoLayout.setVisibility(View.GONE);
                //mVolumeDown.setVisibility(View.GONE);
                mVolumeUp.setVisibility(View.GONE);
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
                //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_play_dark : R.drawable.ic_action_episode_play));
                mSkipForwardImage.setVisibility(View.INVISIBLE);
                mSkipBackImage.setVisibility(View.INVISIBLE);
                mSkipBack.setVisibility(View.INVISIBLE);
                mSkipForward.setVisibility(View.INVISIBLE);
            }
            else if (extras.getBoolean("media_playlist_skip"))
            {
                if (extras.getString("local_file") != null)
                    mLocalFile = extras.getString("local_file");
                else
                    mEpisode = GetEpisode(mContext, extras.getInt("id"), mPlaylistID);
                mEpisodeID = extras.getInt("id");
                SetContent();
            }
            else {
                int duration;

                if (mLocalFile != null)
                    duration = (int) PreferenceManager.getDefaultSharedPreferences(mContext).getLong(Utilities.GetLocalDurationKey(mLocalFile), 0);
                else
                    duration = GetEpisodeValue(mContext, mEpisode, "duration");

                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
                //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_pause_dark : R.drawable.ic_action_episode_pause));

                mProgressBar.setVisibility(ProgressBar.GONE);
                mInfoLayout.setVisibility(View.VISIBLE);
                //mVolumeDown.setVisibility(View.VISIBLE);
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
                mDownloadProgressHandler.removeCallbacksAndMessages(downloadProgress);
                mDownloadSpeed.setVisibility(View.INVISIBLE);
                mProgressCircleDownloading.setVisibility(View.INVISIBLE);
                mDownloadImage.setVisibility(View.INVISIBLE);

                mWearableActionDrawer.setEnabled(true);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                //SaveEpisodeValue(mActivity, mEpisode, "buffering", 0);
            }
        }
    };

    @Override
    public void onItemSelected(final int position) {
        final int id = mNavItems.get(position).getID();
        switch (id) {
            case 0:
                startActivity(new Intent(this, AddPodcastsActivity.class));
                break;
            case 1:
                startActivity(new Intent(this, EpisodeActivity.class));
                break;
            case 2:
                startActivity(new Intent(this, SettingsPodcastsActivity.class));
                break;
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        mEpisodeTitle.setTextColor(ContextCompat.getColor(this, R.color.wc_text));

        findViewById(R.id.podcast_episode_clock).setVisibility(View.VISIBLE);

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
        mVolumeUp.setVisibility(View.INVISIBLE);
        mInfoLayout.setVisibility(View.INVISIBLE);
        findViewById(R.id.podcast_episode_description).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        findViewById(R.id.podcast_episode_clock).setVisibility(View.GONE);

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

        if (mEpisode.getEpisodeId() == GetPlayingEpisode(mContext).getEpisodeId())
            findViewById(R.id.ic_volume_up).setVisibility(View.VISIBLE);

        SetContent();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        final int itemId = menuItem.getItemId();

        switch (itemId) {
            case R.id.menu_drawer_episode_bluetooth_disable:
                new AsyncTasks.ToggleBluetooth(mActivity, true,
                        new Interfaces.AsyncResponse() {
                            @Override
                            public void processFinish() {
                                setMenu();
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            case R.id.menu_drawer_episode_bluetooth_enable:
                new AsyncTasks.ToggleBluetooth(mActivity, false,
                        new Interfaces.AsyncResponse() {
                            @Override
                            public void processFinish() {
                                mEpisode = GetEpisode(mActivity, mEpisodeID, mPlaylistID);
                                setMenu();
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                break;
            case R.id.menu_drawer_episode_open_wifi:
                startActivity(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent));
                break;
            case R.id.menu_drawer_episode_open_bluetooth:
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                break;
            case R.id.menu_drawer_episode_markunplayed:
                markUnplayed(mContext, mEpisode);
                mEpisode = GetEpisode(mActivity, mEpisodeID, mPlaylistID);
                setMenu();
                break;
            case R.id.menu_drawer_episode_markplayed:
                markPlayed(mContext, mEpisode);
                mEpisode = GetEpisode(mActivity, mEpisodeID, mPlaylistID);
                setMenu();
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

                            showToast(mActivity, mActivity.getString(R.string.alert_episode_playlist_added, playlist.getName()));
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
            menu.removeItem(R.id.menu_drawer_episode_open_bluetooth);
        }

        menu.removeItem(mEpisode.getFinished() ? R.id.menu_drawer_episode_markplayed : R.id.menu_drawer_episode_markunplayed);
    }

    private BroadcastReceiver mMediaBufferingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //int percent = intent.getExtras().getInt("percent");
            //Log.d(mContext.getPackageName(), "Buffering Percent: " + percent);
        }
    };
}