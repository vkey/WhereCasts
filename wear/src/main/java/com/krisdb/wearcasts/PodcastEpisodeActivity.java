package com.krisdb.wearcasts;

import android.Manifest;
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
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.PorterDuff;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.input.WearableButtons;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.GetRoundedLogo;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;
import static com.krisdb.wearcastslibrary.DateUtils.GetDisplayDate;

public class PodcastEpisodeActivity extends WearableActivity implements MenuItem.OnMenuItemClickListener, WearableNavigationDrawerView.OnItemSelectedListener {

    private ProgressBar mProgressBar, mProgressCircle;
    private Context mContext;
    private Activity mActivity;
    private PodcastItem mEpisode;
    private SeekBar mSeekBar;
    private RelativeLayout mInfoLayout, mControlsLayout;
    private DownloadManager mDownloadManager;
    private TextView mPositionView, mDurationView, mSkipBack, mSkipForward;
    private int mPlaylistID;
    private long mDownloadId;
    private Handler mDownloadProgressHandler = new Handler();
    private Handler mNetworkHandler = new Handler();
    private ImageView mSkipBackImage, mSkipForwardImage, mPlayPauseImage, mVolumeUp, mVolumeDown, mLogo;
    private static final int STATE_PAUSED = 0, STATE_PLAYING = 1;
    private int mCurrentState, mThemeID;
    private MediaBrowserCompat mMediaBrowserCompat;
    private WearableActionDrawerView mWearableActionDrawer;
    private boolean mDisableBluetooth;
    private String mLocalFile;
    private ConnectivityManager mConnectivityManager;
    private android.support.v4.widget.NestedScrollView mScrollView;
    private static List<NavItem> mNavItems;
    private WeakReference<PodcastEpisodeActivity> mActivityRef;

    @Override
    public Resources.Theme getTheme() {
        final Resources.Theme theme = super.getTheme();

        theme.applyStyle(Utilities.getTheme(this), true);

        return theme;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAmbientEnabled();

        setContentView(R.layout.activity_podcast_episode);

        mContext = getApplicationContext();
        mActivity = this;

        mActivityRef = new WeakReference<>(this);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        mNavItems = Utilities.getNavItems(this);

        final WearableNavigationDrawerView navDrawer = findViewById(R.id.drawer_nav_episode);
        navDrawer.setAdapter(new NavigationAdapter(this, mNavItems));
        navDrawer.addOnItemSelectedListener(this);

        mMediaBrowserCompat = new MediaBrowserCompat(
                mContext,
                new ComponentName(mContext, MediaPlayerService.class),
                mMediaBrowserCompatConnectionCallback,
                getIntent().getExtras()
        );

        mScrollView = findViewById(R.id.podcast_episode_scrollview);
        mProgressBar = findViewById(R.id.podcast_episode_progress_bar);
        mProgressCircle = findViewById(R.id.podcast_episode_progress_circle);
        mSeekBar = findViewById(R.id.sb_podcast_episode);
        mDownloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
        mPlayPauseImage = findViewById(R.id.ic_podcast_playpause);
        mSkipBackImage = findViewById(R.id.ic_skip_back);
        mSkipForwardImage = findViewById(R.id.ic_skip_forward);
        mVolumeUp = findViewById(R.id.ic_volume_up);
        mVolumeDown = findViewById(R.id.ic_volume_down);
        mDurationView = findViewById(R.id.tv_podcast_duration);
        mPositionView = findViewById(R.id.tv_podcast_position);
        mInfoLayout = findViewById(R.id.podcast_episode_info_layout);
        mControlsLayout = findViewById(R.id.podcast_episode_buttons_layout);
        mWearableActionDrawer = findViewById(R.id.drawer_action_episode);
        mSkipBack = findViewById(R.id.tv_skip_back);
        mSkipForward = findViewById(R.id.tv_skip_forward);
        mLogo = findViewById(R.id.podcast_episode_title_image);

        mWearableActionDrawer.setOnMenuItemClickListener(this);

        mLogo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final Intent intent = new Intent(mContext, SettingsPodcastActivity.class);
                final Bundle bundle = new Bundle();
                bundle.putInt("podcastId", mEpisode.getPodcastId());
                intent.putExtras(bundle);

                if (mEpisode.getPodcastId() > 0)
                    startActivity(intent);
                return false;
            }
        });


        final int skipBack = Integer.valueOf(prefs.getString("pref_playback_skip_back", String.valueOf(getResources().getInteger(R.integer.default_playback_skip))));
        final int skipForward = Integer.valueOf(prefs.getString("pref_playback_skip_forward", String.valueOf(getResources().getInteger(R.integer.default_playback_skip))));

        mSkipBack.setText(String.valueOf(skipBack));
        mSkipForward.setText(String.valueOf(skipForward));

        mVolumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
            }
        });

        mVolumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
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
        db.update(cv, getIntent().getExtras().getInt("eid"));
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
                        final int position = mSeekBar.getProgress() + getResources().getInteger(R.integer.skip_seconds) * 1000;
                        if (position < mSeekBar.getMax())
                            MediaControllerCompat.getMediaController(mActivity).getTransportControls().seekTo(position);
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_STEM_1) {
                        final int position = mSeekBar.getProgress() - getResources().getInteger(R.integer.skip_seconds) * 1000;
                        if (position > 0)
                            MediaControllerCompat.getMediaController(mActivity).getTransportControls().seekTo(position);
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

    private void togglePlayback()
    {
        mWearableActionDrawer.setEnabled(true);

        final Boolean isCurrentlyPlaying = mEpisode.getEpisodeId() == DBUtilities.GetPlayingEpisode(mContext).getEpisodeId();

        if (mCurrentState == STATE_PAUSED || (mCurrentState == STATE_PLAYING && isCurrentlyPlaying == false)) { //play
            final Bundle extras = new Bundle();
            extras.putInt("id", mEpisode.getEpisodeId());
            extras.putString("local_file", mLocalFile);
            extras.putInt("playlistid", mPlaylistID);

            //downloaded
            if (mLocalFile != null || DBUtilities.GetEpisodeValue(mActivity, mEpisode, "download") == 1) {

                final String uri = (mLocalFile != null) ? GetLocalDirectory().concat(mLocalFile) : Utilities.GetMediaFile(mActivity, mEpisode);

                MediaControllerCompat.getMediaController(mActivity).getTransportControls().playFromUri(Uri.parse(uri), extras);

                mCurrentState = STATE_PLAYING;
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
                //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_pause_dark : R.drawable.ic_action_episode_pause));
                mInfoLayout.setVisibility(View.VISIBLE);
                mVolumeDown.setVisibility(View.VISIBLE);
                mVolumeUp.setVisibility(View.VISIBLE);
            }
            else {
                handleNetwork(mNetworkStreamCallback);
            }
            int position = DBUtilities.GetEpisodeValue(getApplicationContext(), mEpisode, "position");
            mSeekBar.setProgress(position);
        }
        else
        {
            //pause episode
            MediaControllerCompat.getMediaController(mActivity).getTransportControls().pause();
            //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_play_dark : R.drawable.ic_action_episode_play));
            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
            mProgressBar.setVisibility(View.GONE);
            mInfoLayout.setVisibility(View.GONE);
            mVolumeDown.setVisibility(View.GONE);
            mVolumeUp.setVisibility(View.GONE);
            mSkipBackImage.setVisibility(View.INVISIBLE);
            mSkipForwardImage.setVisibility(View.INVISIBLE);
            mSkipBack.setVisibility(View.INVISIBLE);
            mSkipForward.setVisibility(View.INVISIBLE);
            mCurrentState = STATE_PAUSED;
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        ((TextView)findViewById(R.id.podcast_episode_title)).setTextColor(ContextCompat.getColor(this, R.color.wc_text));

        findViewById(R.id.podcast_episode_clock).setVisibility(View.VISIBLE);
        ((ImageView)findViewById(R.id.ic_podcast_playpause)).setColorFilter(getColor(R.color.wc_ambient_playpause_on), PorterDuff.Mode.SRC_IN);

        mScrollView.setBackgroundColor(getColor(R.color.wc_background_amoled));

        if (mThemeID == Enums.ThemeOptions.DARK.getThemeId()) {
            findViewById(R.id.podcast_episode_layout).setBackgroundColor(getColor(R.color.wc_background_amoled));
            findViewById(R.id.drawer_layout).setBackgroundColor(getColor(R.color.wc_background_amoled));
            findViewById(R.id.podcast_episode_buttons_layout).setBackgroundColor(getColor(R.color.wc_background_amoled));
            findViewById(R.id.podcast_episode_info_layout).setBackgroundColor(getColor(R.color.wc_background_amoled));
            findViewById(R.id.podcast_episode_title).setBackgroundColor(getColor(R.color.wc_background_amoled));
        }
        mLogo.setVisibility(View.INVISIBLE);
        findViewById(R.id.tv_skip_forward).setVisibility(View.INVISIBLE);
        findViewById(R.id.tv_skip_back).setVisibility(View.INVISIBLE);
        findViewById(R.id.podcast_episode_description).setVisibility(View.INVISIBLE);
        findViewById(R.id.ic_volume_down).setVisibility(View.INVISIBLE);
        findViewById(R.id.ic_volume_up).setVisibility(View.INVISIBLE);
        findViewById(R.id.podcast_episode_info_layout).setVisibility(View.INVISIBLE);
        findViewById(R.id.ic_skip_forward).setVisibility(View.INVISIBLE);
        findViewById(R.id.ic_skip_back).setVisibility(View.INVISIBLE);
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
            findViewById(R.id.podcast_episode_title).setBackgroundColor(getColor(R.color.wc_background_dark));
        }

        ((ImageView) findViewById(R.id.ic_podcast_playpause)).setColorFilter(getColor(R.color.wc_ambient_playpause_off), PorterDuff.Mode.SRC_IN);
        mLogo.setVisibility(View.VISIBLE);
        findViewById(R.id.tv_skip_back).setVisibility(View.VISIBLE);
        findViewById(R.id.tv_skip_forward).setVisibility(View.VISIBLE);
        findViewById(R.id.podcast_episode_description).setVisibility(View.VISIBLE);
        findViewById(R.id.ic_volume_down).setVisibility(View.VISIBLE);
        findViewById(R.id.ic_volume_up).setVisibility(View.VISIBLE);
        findViewById(R.id.podcast_episode_info_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.ic_skip_forward).setVisibility(View.VISIBLE);
        findViewById(R.id.ic_skip_back).setVisibility(View.VISIBLE);

        SetContent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        //mEpisode = DBUtilities.GetEpisode(mActivity, intent.getExtras().getInt("eid"));
        if (intent.getExtras() != null)
            SetContent(intent.getExtras().getInt("eid"));
    }

    @Override
    public void onResume() {
        super.onResume();

        mDisableBluetooth = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_disable_bluetooth", false);

       if (mMediaBrowserCompat != null && mMediaBrowserCompat.isConnected() == false) {
           try {
               mMediaBrowserCompat.connect();
           }
           catch (Exception ex) {
               mMediaBrowserCompat.disconnect();
           }
       }
        else
            SetContent();
    }

    private void SetContent()
    {
        SetContent(-1);
    }

    private void SetContent(int episodeId) {

        if (mLocalFile != null) {
            mEpisode = new PodcastItem();
            mEpisode.setTitle(mLocalFile);
        }
        else if (episodeId > -1)
            mEpisode = DBUtilities.GetEpisode(mActivity, episodeId, mPlaylistID);

        mLogo.setImageDrawable(GetRoundedLogo(mActivity, mEpisode.getChannel(), R.drawable.ic_thumb_default));

        mThemeID = Utilities.getThemeOptionId(this);

        int textColor = ContextCompat.getColor(this, R.color.wc_text);

        if (mThemeID == Enums.ThemeOptions.DYNAMIC.getThemeId() && mEpisode.getChannel() != null && mEpisode.getChannel().getThumbnailName() != null) {

            final Pair<Integer, Integer> colors = CommonUtils.GetBackgroundColor(mEpisode);
            textColor = colors.second;

            mScrollView.setBackgroundColor(colors.first);
            ((TextView) findViewById(R.id.podcast_episode_title)).setTextColor(textColor);
            ((android.widget.TextClock)findViewById(R.id.podcast_episode_clock)).setTextColor(textColor);
        }
        //else if (mThemeID == Enums.ThemeOptions.LIGHT.getThemeId())
            //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play_dark));

        ((TextView) findViewById(R.id.podcast_episode_title)).setText(mEpisode.getTitle());

        if (mEpisode.getDescription() != null) {
            ((TextView) findViewById(R.id.podcast_episode_description)).setText(GetDisplayDate(mActivity, mEpisode.getPubDate()).concat(" - ").concat(CommonUtils.CleanDescription(mEpisode.getDescription())));
            if (mThemeID == Enums.ThemeOptions.DYNAMIC.getThemeId())
                ((TextView) findViewById(R.id.podcast_episode_description)).setTextColor(textColor);
        }

        if (MediaControllerCompat.getMediaController(mActivity).getPlaybackState() != null &&
                MediaControllerCompat.getMediaController(mActivity).getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
            mCurrentState = STATE_PLAYING;

            final Boolean isCurrentlyPlaying = mEpisode.getEpisodeId() == DBUtilities.GetPlayingEpisode(mContext).getEpisodeId();

            if (isCurrentlyPlaying) {
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
                //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_pause_dark : R.drawable.ic_action_episode_pause));
                int position = DBUtilities.GetEpisodeValue(mActivity, mEpisode, "position");
                int duration = DBUtilities.GetEpisodeValue(mActivity, mEpisode, "duration");
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
                //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_stop_dark : R.drawable.ic_action_episode_stop));
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
            //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_play_dark : R.drawable.ic_action_episode_play));
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

        final Menu menu = mWearableActionDrawer.getMenu();
        menu.clear();

        getMenuInflater().inflate((DBUtilities.GetEpisodeValue(mActivity, mEpisode, "download") == 1) ?
                R.menu.menu_drawer_episode_download_delete
                : R.menu.menu_drawer_episode_download, menu
        );

        //fail safe in case file failed to download;
        if (Utilities.GetMediaFile(mActivity, mEpisode) == null)
            DBUtilities.SaveEpisodeValue(mActivity, mEpisode, "download", 0);

        if (mLocalFile == null && mEpisode.getMediaUrl() == null) {
            mPlayPauseImage.setEnabled(false);
            //mPlayPauseImage.setText("Error");
            mPlayPauseImage.setBackgroundResource(0);
            ((TextView) findViewById(R.id.podcast_episode_error)).setText(getString(R.string.text_episode_no_media));
            findViewById(R.id.podcast_episode_error).setVisibility(View.VISIBLE);
        }

        mDownloadId = DBUtilities.GetEpisodeValue(mActivity, mEpisode, "downloadid");

        if (mDownloadId > 0)
            downloadProgress.run();

        try {
            LocalBroadcastManager.getInstance(this).registerReceiver(mMediaReceiver, new IntentFilter("media_action"));
            LocalBroadcastManager.getInstance(this).registerReceiver(mMediaPositionReceiver, new IntentFilter("media_position"));
        }
        catch(Exception ignored){}
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

        super.onPause();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        final int itemId = menuItem.getItemId();

        switch (itemId) {
            case R.id.menu_drawer_episode_markplayed:
                DBUtilities.SaveEpisodeValue(mContext, mEpisode, "finished", 1);
                CommonUtils.showToast(mActivity, getString(R.string.alert_marked_played));
                break;
            case R.id.menu_drawer_episode_markunplayed:
                DBUtilities.SaveEpisodeValue(mContext, mEpisode, "finished", 0);
                CommonUtils.showToast(mActivity, getString(R.string.alert_marked_unplayed));
                break;
            case R.id.menu_drawer_episode_add_playlist:
                final View playlistAddView = getLayoutInflater().inflate(R.layout.episode_add_playlist, null);

                final List<PlaylistItem> playlistItems = DBUtilities.getPlaylists(mActivity);
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

                        final PlaylistItem playlist = (PlaylistItem) parent.getSelectedItem();

                        if (playlist.getID() != getResources().getInteger(R.integer.default_playlist_select)) {
                            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

                            if (prefs.getBoolean("pref_hide_empty_playlists", false) && DBUtilities.playlistIsEmpty(mActivity, playlist.getID()))
                            {
                                final SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("refresh_vp", true);
                                editor.apply();
                            }

                            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                            db.addEpisodeToPlaylist(playlist.getID(), mEpisode.getEpisodeId());
                            db.close();

                            showToast(mActivity, mActivity.getString(R.string.alert_episode_playlist_added, playlist.getName()));
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {}
                });

                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(PodcastEpisodeActivity.this);
                    builder.setView(playlistAddView);
                    builder.create().show();
                }
                break;
            case R.id.menu_drawer_episode_download:
                final int writeStorage = ContextCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if (writeStorage != PackageManager.PERMISSION_GRANTED)
                    ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                else {
                    if (Utilities.BluetoothEnabled()) {
                        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
                        if (prefs.getBoolean("initialDownload", true)) {
                            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {

                                final AlertDialog.Builder alert = new AlertDialog.Builder(PodcastEpisodeActivity.this);
                                alert.setMessage(mContext.getString(R.string.confirm_initial_download_message));
                                alert.setPositiveButton(mContext.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //disable bluetooth and enable setting
                                        final SharedPreferences.Editor editor = prefs.edit();
                                        editor.putBoolean("pref_disable_bluetooth", true);
                                        editor.apply();

                                        new AsyncTasks.DisableBluetooth(mContext,
                                                new Interfaces.AsyncResponse() {
                                                    @Override
                                                    public void processFinish() {
                                                        handleNetwork(mNetworkDownloadCallback);
                                                    }
                                                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                                        dialog.dismiss();
                                    }
                                });
                                alert.setNegativeButton(mContext.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        handleNetwork(mNetworkDownloadCallback);
                                        dialog.dismiss();
                                    }
                                });
                                alert.show();
                            }
                            final SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("initialDownload", false);
                            editor.apply();
                        } else if (mDisableBluetooth)
                            new AsyncTasks.DisableBluetooth(mContext,
                                    new Interfaces.AsyncResponse() {
                                        @Override
                                        public void processFinish() {
                                            handleNetwork(mNetworkDownloadCallback);
                                        }
                                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        else
                            handleNetwork(mNetworkDownloadCallback);
                    } else
                        handleNetwork(mNetworkDownloadCallback);
                }
                break;
            case R.id.menu_drawer_episode_download_delete:
                DeleteEpisode();
                break;
            case R.id.menu_drawer_episode_download_cancel:
                CancelDownload();
                break;
        }

        mWearableActionDrawer.getController().closeDrawer();

        return true;
    }

    private Runnable downloadProgress = new Runnable() {
            @Override
            public void run() {
            final DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(mDownloadId);
            final Cursor cursor = mDownloadManager.query(query);
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            if (cursor.moveToFirst()) {
                final int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                mProgressCircle.setMax(bytes_total);
                mProgressCircle.setSecondaryProgress(bytes_total);
                mProgressCircle.setVisibility(View.VISIBLE);

                final int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                final int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));

                //Log.d(mContext.getPackageName(), "Status: " + status);
                //Log.d(mContext.getPackageName(), "Bytes: "  +bytes_total);

                switch (status) {
                    case DownloadManager.STATUS_PAUSED:
                    case DownloadManager.STATUS_RUNNING:
                    case DownloadManager.STATUS_PENDING:
                        final int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        mProgressCircle.setVisibility(View.VISIBLE);
                        mProgressCircle.setProgress(bytes_downloaded);
                        mPlayPauseImage.setVisibility(View.INVISIBLE);
                        mControlsLayout.setVisibility(View.GONE);
                        final Menu menuRunning = mWearableActionDrawer.getMenu();
                        menuRunning.clear();
                        getMenuInflater().inflate(R.menu.menu_drawer_episode_download_cancel, menuRunning);
                        mDownloadProgressHandler.postDelayed(downloadProgress, 1000);
                        break;
                    case DownloadManager.STATUS_FAILED:
                        mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
                        //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_play_dark : R.drawable.ic_action_episode_play));
                        mPlayPauseImage.setVisibility(View.VISIBLE);
                        mProgressCircle.setVisibility(View.GONE);
                        mInfoLayout.setVisibility(View.GONE);
                        mControlsLayout.setVisibility(View.VISIBLE);
                        mVolumeDown.setVisibility(View.GONE);
                        mVolumeUp.setVisibility(View.GONE);
                        final Menu menuFailed = mWearableActionDrawer.getMenu();
                        menuFailed.clear();
                        getMenuInflater().inflate(R.menu.menu_drawer_episode_download, menuFailed);
                        mDownloadManager.remove(mDownloadId);

                        if (prefs.getBoolean("pref_downloads_restart_on_failure", true)) {
                            SystemClock.sleep(3000);
                            mDownloadId = DBUtilities.GetDownloadIDByEpisode(mContext, mEpisode);
                            mDownloadProgressHandler.postDelayed(downloadProgress, 1000);
                        }
                        else
                            showToast(mActivity, Utilities.GetDownloadErrorReason(mContext, reason));

                        mDownloadProgressHandler.removeCallbacksAndMessages(downloadProgress);
                    case DownloadManager.STATUS_SUCCESSFUL:
                        final Menu menuSuccess = mWearableActionDrawer.getMenu();
                        menuSuccess.clear();
                        getMenuInflater().inflate(R.menu.menu_drawer_episode_download_delete, menuSuccess);
                        //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_play_dark : R.drawable.ic_action_episode_play));
                        mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
                        mPlayPauseImage.setVisibility(View.VISIBLE);
                        mControlsLayout.setVisibility(View.VISIBLE);
                        mInfoLayout.setVisibility(View.GONE);
                        mVolumeDown.setVisibility(View.GONE);
                        mVolumeUp.setVisibility(View.GONE);
                        mProgressCircle.setVisibility(View.GONE);
                        mDownloadProgressHandler.removeCallbacksAndMessages(downloadProgress);
                }
            }
            cursor.close();
        }
    };

    private void handleNetwork(final ConnectivityManager.NetworkCallback callback) {
        if (PreferenceManager.getDefaultSharedPreferences(mActivity).getBoolean("pref_high_bandwidth", true) == false) {
            if (callback == mNetworkDownloadCallback)
                DownloadEpisode();
            else
                StreamEpisode();

            return;
        }

        mNetworkHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        mConnectivityManager.bindProcessToNetwork(null);
                        mConnectivityManager.unregisterNetworkCallback(callback);
                        mNetworkHandler.removeMessages(1);

                        if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                            final AlertDialog.Builder alert = new AlertDialog.Builder(PodcastEpisodeActivity.this);
                            alert.setMessage(getString(R.string.alert_episode_network_notfound));
                            alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivityForResult(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"), (callback == mNetworkDownloadCallback) ? 1 : 2);
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
                        else
                            CommonUtils.showToast(mContext, getString(R.string.general_error));

                        break;
                }
            }
        };

        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final Network activeNetwork = mConnectivityManager.getActiveNetwork();

        if (activeNetwork != null) {
            int bandwidth = mConnectivityManager.getNetworkCapabilities(activeNetwork).getLinkDownstreamBandwidthKbps();

            if (bandwidth < getResources().getInteger(R.integer.minimum_bandwidth)) {
                final NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();

                mConnectivityManager.requestNetwork(request, callback);
                showToast(getApplicationContext(), getString(R.string.alert_episode_network_search));
                mNetworkHandler.sendMessageDelayed(mNetworkHandler.obtainMessage(1), 10000);
            } else {

                if (callback == mNetworkDownloadCallback)
                    DownloadEpisode();
                else
                    StreamEpisode();
            }
        } else {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(PodcastEpisodeActivity.this);
                alert.setMessage(getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"), (callback == mNetworkDownloadCallback) ? 1 : 2);
                        dialog.dismiss();
                    }
                });

                alert.setNegativeButton(getString(R.string.confirm_no), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            } else
                CommonUtils.showToast(mContext, getString(R.string.general_error));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1)
                DownloadEpisode();
            else
                StreamEpisode();
        }
    }

    final ConnectivityManager.NetworkCallback mNetworkDownloadCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            DownloadEpisode();
            mNetworkHandler.removeMessages(1);
        }
    };

    final ConnectivityManager.NetworkCallback mNetworkStreamCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            StreamEpisode();
            mNetworkHandler.removeMessages(2);
        }
    };

    private void StreamEpisode()
    {
        final Bundle extras = new Bundle();
        extras.putInt("id", mEpisode.getEpisodeId());
        extras.putString("local_file", mLocalFile);
        extras.putInt("playlistid", mPlaylistID);

        MediaControllerCompat.getMediaController(mActivity).getTransportControls().playFromUri(Uri.parse(mEpisode.getMediaUrl().toString()), extras);

        mCurrentState = STATE_PLAYING;
        //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_pause_dark : R.drawable.ic_action_episode_pause));

        runOnUiThread(new Runnable() {
            public void run(){
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
                mInfoLayout.setVisibility(View.VISIBLE);
                mVolumeDown.setVisibility(View.VISIBLE);
                mVolumeUp.setVisibility(View.VISIBLE);
                //CommonUtils.showToast(mContext, getString(R.string.alert_streaming));
            }
        });

        mNetworkHandler.removeMessages(1);
    }

    private void DownloadEpisode() {
        mDownloadId = Utilities.startDownload(mContext, mEpisode);
        showToast(mActivity, getString(R.string.alert_episode_download_start));
        mDownloadProgressHandler.postDelayed(downloadProgress, 1000);
    }

    public void CancelDownload()
    {
        mDownloadProgressHandler.removeCallbacksAndMessages(downloadProgress);
        mPlayPauseImage.setEnabled(true);
        mControlsLayout.setVisibility(View.VISIBLE);
        mDownloadManager.remove(mDownloadId);
        mProgressBar.setVisibility(View.GONE);
        mProgressCircle.setVisibility(View.GONE);
        mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
        //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_play_dark : R.drawable.ic_action_episode_play));
        mPlayPauseImage.setVisibility(View.VISIBLE);
        mSeekBar.setVisibility(View.GONE);
        mInfoLayout.setVisibility(View.GONE);
        mVolumeDown.setVisibility(View.GONE);
        mVolumeUp.setVisibility(View.GONE);
        Utilities.DeleteMediaFile(mActivity, mEpisode);
        if (mDisableBluetooth && Utilities.BluetoothEnabled() == false)
            BluetoothAdapter.getDefaultAdapter().enable();

        final Menu menu = mWearableActionDrawer.getMenu();
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_drawer_episode_download, menu);

        final ContentValues cv = new ContentValues();
        cv.put("download", 0);
        cv.put("downloadid", 0);

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
        db.update(cv, mEpisode.getEpisodeId());
        db.close();
    }

    public void DeleteEpisode() {
        if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
            final AlertDialog.Builder alert = new AlertDialog.Builder(PodcastEpisodeActivity.this);
            alert.setMessage(getString(R.string.confirm_delete_episode_download));
            alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final ContentValues cv = new ContentValues();
                    cv.put("download", 0);
                    cv.put("downloadid", 0);
                    new DBPodcastsEpisodes(mActivity).update(cv, mEpisode.getEpisodeId());
                    Utilities.DeleteMediaFile(mActivity, mEpisode);

                    new DBPodcastsEpisodes(mActivity).update(cv, mEpisode.getEpisodeId());
                    final Menu menu = mWearableActionDrawer.getMenu();
                    menu.clear();
                    getMenuInflater().inflate(R.menu.menu_drawer_episode_download, menu);
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
    public void onStop() {
        super.onStop();
        if (mConnectivityManager != null) {
            mConnectivityManager.bindProcessToNetwork(null);

            try {
                mConnectivityManager.unregisterNetworkCallback(mNetworkDownloadCallback);
                mConnectivityManager.unregisterNetworkCallback(mNetworkStreamCallback);
            }
            catch(Exception ignored){}
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
                final MediaControllerCompat mcc = new MediaControllerCompat(PodcastEpisodeActivity.this, mMediaBrowserCompat.getSessionToken());
                mcc.registerCallback(mMediaControllerCompatCallback);
                MediaControllerCompat.setMediaController(mActivity, mcc);
                mPlaylistID = getIntent().getExtras().getInt("playlistid");

                mLocalFile = getIntent().getExtras().getString("local_file");

                SetContent(getIntent().getExtras().getInt("eid"));
                mActivity.findViewById(R.id.podcast_episode_layout).setVisibility(View.VISIBLE);

                } catch( RemoteException e ) {
                Log.e(mActivity.getPackageName(), e.toString());
            }
        }
    };

    private MediaControllerCompat.Callback mMediaControllerCompatCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onExtrasChanged(Bundle extras) {
            super.onExtrasChanged(extras);

            SetContent(extras.getInt("id"));
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
                mProgressBar.setIndeterminate(true);
                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                mInfoLayout.setVisibility(View.GONE);
                mVolumeDown.setVisibility(View.GONE);
                mVolumeUp.setVisibility(View.GONE);
                mSkipForwardImage.setVisibility(View.INVISIBLE);
                mSkipBackImage.setVisibility(View.INVISIBLE);
                mSkipBack.setVisibility(View.INVISIBLE);
                mSkipForward.setVisibility(View.INVISIBLE);
                mDurationView.setVisibility(View.INVISIBLE);
                mPositionView.setVisibility(View.INVISIBLE);
                mWearableActionDrawer.setEnabled(false);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                //DBUtilities.SaveEpisodeValue(mActivity, mEpisode, "buffering", 1);
            }
            else if (extras.getBoolean("media_paused"))
            {
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
                //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_play_dark : R.drawable.ic_action_episode_play));
                mProgressBar.setVisibility(View.GONE);
                mInfoLayout.setVisibility(View.GONE);
                mVolumeDown.setVisibility(View.GONE);
                mVolumeUp.setVisibility(View.GONE);
                mSkipBackImage.setVisibility(View.INVISIBLE);
                mSkipForwardImage.setVisibility(View.INVISIBLE);
                mSkipBack.setVisibility(View.INVISIBLE);
                mSkipForward.setVisibility(View.INVISIBLE);
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
            }
            else if (extras.getBoolean("media_error"))
            {
                showToast(mActivity, getString(R.string.general_error));

                //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_play_dark : R.drawable.ic_action_episode_play));
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
                mInfoLayout.setVisibility(View.GONE);
                mVolumeDown.setVisibility(View.GONE);
                mVolumeUp.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
            }
            else if (extras.getBoolean("media_completed"))
            {
                mInfoLayout.setVisibility(View.GONE);
                mVolumeDown.setVisibility(View.GONE);
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
                    mEpisode = DBUtilities.GetEpisode(mContext, extras.getInt("id"), mPlaylistID);
                SetContent(extras.getInt("id"));
            }
            else {
                int duration;

                if (mLocalFile != null)
                    duration = (int) PreferenceManager.getDefaultSharedPreferences(mContext).getLong(Utilities.GetLocalDurationKey(mLocalFile), 0);
                else
                    duration = DBUtilities.GetEpisodeValue(mContext, mEpisode, "duration");

                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
                //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_pause_dark : R.drawable.ic_action_episode_pause));

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

                mWearableActionDrawer.setEnabled(true);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                //DBUtilities.SaveEpisodeValue(mActivity, mEpisode, "buffering", 0);
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
                startActivity(new Intent(this, PodcastEpisodeActivity.class));
                break;
            case 2:
                startActivity(new Intent(this, SettingsPodcastsActivity.class));
                break;
        }
    }
    private BroadcastReceiver mMediaBufferingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //int percent = intent.getExtras().getInt("percent");
            //Log.d(mContext.getPackageName(), "Buffering Percent: " + percent);
        }
    };
}