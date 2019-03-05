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
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
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
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.krisdb.wearcasts.Adapters.NavigationAdapter;
import com.krisdb.wearcasts.Adapters.PlaylistsAssignAdapter;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Databases.DBUtilities;
import com.krisdb.wearcasts.Models.NavItem;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Services.MediaPlayerService;
import com.krisdb.wearcasts.Settings.SettingsPodcastActivity;
import com.krisdb.wearcasts.Settings.SettingsPodcastsActivity;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

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
    private TextView mPositionView, mDurationView, mSkipBack, mSkipForward, mDownloadSpeed;
    private int mPlaylistID, mCurrentState, mThemeID;
    private long mDownloadId, mDownloadStartTime;
    private Handler mDownloadProgressHandler = new Handler();
    private ImageView mSkipBackImage, mSkipForwardImage, mPlayPauseImage, mVolumeUp, mLogo, mDownloadImage;
    private static final int STATE_PAUSED = 0, STATE_PLAYING = 1;
    private MediaBrowserCompat mMediaBrowserCompat;
    private WearableActionDrawerView mWearableActionDrawer;
    private String mLocalFile;
    private android.support.v4.widget.NestedScrollView mScrollView;
    private static List<NavItem> mNavItems;
    private WeakReference<PodcastEpisodeActivity> mActivityRef;
    private WearableNavigationDrawerView mNavDrawer;

    @Override
    public Resources.Theme getTheme() {
        final Resources.Theme theme = super.getTheme();

        theme.applyStyle(Utilities.getTheme(this), true);

        return theme;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getBoolean("pref_display_ambient_disable", false) == false)
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

        mScrollView = findViewById(R.id.podcast_episode_scrollview);
        mProgressBar = findViewById(R.id.podcast_episode_progress_bar);
        mProgressCircle = findViewById(R.id.podcast_episode_progress_circle);
        mDownloadImage = findViewById(R.id.ic_podcast_episode_download);
        mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_circle));

        mSeekBar = findViewById(R.id.sb_podcast_episode);
        mDownloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
        mPlayPauseImage = findViewById(R.id.ic_podcast_playpause);
        mSkipBackImage = findViewById(R.id.ic_skip_back);
        mSkipForwardImage = findViewById(R.id.ic_skip_forward);
        mDownloadSpeed = findViewById(R.id.podcast_episode_download_speed);
        mVolumeUp = findViewById(R.id.ic_volume_up);
        //mVolumeDown = findViewById(R.id.ic_volume_down);
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

        /*
        mVolumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            }
        });
        */

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

        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        final Menu menu = mWearableActionDrawer.getMenu();
        if (adapter != null) {
            menu.clear();
            if (adapter.isEnabled())
                getMenuInflater().inflate(R.menu.menu_drawer_episode_bluetooth_enabled, menu);
            else
                getMenuInflater().inflate(R.menu.menu_drawer_episode_bluetooth_disabled, menu);
        }
        else
            getMenuInflater().inflate(R.menu.menu_drawer_episode, menu);

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

    private void togglePlayback()
    {
        mWearableActionDrawer.setEnabled(true);

        final Boolean isCurrentlyPlaying = mEpisode.getEpisodeId() == DBUtilities.GetPlayingEpisode(mContext).getEpisodeId();

        if (mCurrentState == STATE_PAUSED || (mCurrentState == STATE_PLAYING && isCurrentlyPlaying == false)) { //play episode
            final Bundle extras = new Bundle();
            extras.putInt("id", mEpisode.getEpisodeId());
            extras.putString("local_file", mLocalFile);
            extras.putInt("playlistid", mPlaylistID);
            mDownloadImage.setVisibility(View.GONE);
            //check for downloaded episode
            if (mLocalFile != null || DBUtilities.GetEpisodeValue(mActivity, mEpisode, "download") == 1) {

                final String uri = (mLocalFile != null) ? GetLocalDirectory().concat(mLocalFile) : Utilities.GetMediaFile(mActivity, mEpisode);

                MediaControllerCompat.getMediaController(mActivity).getTransportControls().playFromUri(Uri.parse(uri), extras);

                mCurrentState = STATE_PLAYING;
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
                //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_pause_dark : R.drawable.ic_action_episode_pause));
                mInfoLayout.setVisibility(View.VISIBLE);
                //mVolumeDown.setVisibility(View.VISIBLE);
                mVolumeUp.setVisibility(View.VISIBLE);
            }
            else
                handleNetwork(false);

            int position = DBUtilities.GetEpisodeValue(getApplicationContext(), mEpisode, "position");
            mSeekBar.setProgress(position);
        }
        else
        {
            //pause episode
            MediaControllerCompat.getMediaController(mActivity).getTransportControls().pause();
            //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_play_dark : R.drawable.ic_action_episode_play));
            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
            mDownloadImage.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            mInfoLayout.setVisibility(View.GONE);
            //mVolumeDown.setVisibility(View.GONE);
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
            findViewById(R.id.podcast_episode_title).setBackgroundColor(getColor(R.color.wc_background_amoled));
        }
        //mLogo.setVisibility(View.INVISIBLE);
        findViewById(R.id.tv_skip_forward).setVisibility(View.INVISIBLE);
        findViewById(R.id.ic_podcast_episode_download).setVisibility(View.INVISIBLE);
        findViewById(R.id.ic_podcast_playpause).setVisibility(View.INVISIBLE);
        findViewById(R.id.tv_skip_back).setVisibility(View.INVISIBLE);
        findViewById(R.id.podcast_episode_description).setVisibility(View.INVISIBLE);
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

        //((ImageView) findViewById(R.id.ic_podcast_playpause)).setColorFilter(getColor(R.color.wc_ambient_playpause_off), PorterDuff.Mode.SRC_IN);
        //mLogo.setVisibility(View.VISIBLE);
        findViewById(R.id.tv_skip_back).setVisibility(View.VISIBLE);
        findViewById(R.id.ic_podcast_episode_download).setVisibility(View.VISIBLE);
        findViewById(R.id.ic_podcast_playpause).setVisibility(View.VISIBLE);
        findViewById(R.id.tv_skip_forward).setVisibility(View.VISIBLE);
        findViewById(R.id.podcast_episode_description).setVisibility(View.VISIBLE);
        findViewById(R.id.podcast_episode_info_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.ic_skip_forward).setVisibility(View.VISIBLE);
        findViewById(R.id.ic_skip_back).setVisibility(View.VISIBLE);

        if (mEpisode.getEpisodeId() == DBUtilities.GetPlayingEpisode(mContext).getEpisodeId())
            findViewById(R.id.ic_volume_up).setVisibility(View.VISIBLE);

        SetContent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        try {
            if (intent.getExtras() != null)
                SetContent(intent.getExtras().getInt("eid"));
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

            if (mEpisode.getEpisodeId() == DBUtilities.GetPlayingEpisode(mContext).getEpisodeId())
                mDownloadImage.setVisibility(View.GONE);
            else
                mDownloadImage.setVisibility(View.VISIBLE);

            if (mEpisode.getEpisodeId() == DBUtilities.GetPlayingEpisode(mContext).getEpisodeId()) {
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
                //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_pause_dark : R.drawable.ic_action_episode_pause));
                int position = DBUtilities.GetEpisodeValue(mActivity, mEpisode, "position");
                int duration = DBUtilities.GetEpisodeValue(mActivity, mEpisode, "duration");
                mSeekBar.setMax(duration);
                mSeekBar.setProgress(position);
                mDurationView.setText(DateUtils.FormatPositionTime(duration));
                mInfoLayout.setVisibility(View.VISIBLE);
                //mVolumeDown.setVisibility(View.VISIBLE);
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
                //mVolumeDown.setVisibility(View.GONE);
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
            //mVolumeDown.setVisibility(View.GONE);
            mVolumeUp.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.GONE);
            mSkipForwardImage.setVisibility(View.INVISIBLE);
            mSkipBackImage.setVisibility(View.INVISIBLE);
            mSkipBack.setVisibility(View.INVISIBLE);
            mSkipForward.setVisibility(View.INVISIBLE);
            mDownloadImage.setVisibility(View.VISIBLE);
        }

        if (mLocalFile != null || DBUtilities.GetEpisodeValue(mActivity, mEpisode, "download") == 1)
        {
            mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_delete2));
            mDownloadImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DeleteEpisode();
                }
            });
        }
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
            case R.id.menu_drawer_episode_bluetooth_enable:
                BluetoothAdapter.getDefaultAdapter().enable();
                final Menu menu1 = mWearableActionDrawer.getMenu();
                menu1.clear();
                getMenuInflater().inflate(R.menu.menu_drawer_episode_bluetooth_enabled, menu1);
                CommonUtils.showToast(mActivity, getString(R.string.alert_disable_bluetooth_enabled));
                break;
            case R.id.menu_drawer_episode_bluetooth_disable:
                BluetoothAdapter.getDefaultAdapter().disable();
                final Menu menu2 = mWearableActionDrawer.getMenu();
                menu2.clear();
                getMenuInflater().inflate(R.menu.menu_drawer_episode_bluetooth_disabled, menu2);
                CommonUtils.showToast(mActivity, getString(R.string.alert_disable_bluetooth_disabled_end));
                break;
            //case R.id.menu_drawer_episode_open_wifi:
                //startActivity(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"));
                //break;
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
                //mProgressCircle.setSecondaryProgress(bytes_total);

                final int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                //final int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));

                //Log.d(mContext.getPackageName(), "Status: " + status);
                //Log.d(mContext.getPackageName(), "Bytes: "  +bytes_total);
                final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

                switch (status) {
                    case DownloadManager.STATUS_PAUSED:
                    case DownloadManager.STATUS_RUNNING:
                    case DownloadManager.STATUS_PENDING:
                        final int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));

                        if ((System.nanoTime() - mDownloadStartTime) > 0)
                        {
                            final float bytesPerSec = bytes_downloaded / ((System.nanoTime() - mDownloadStartTime) / 1000000000);

                            if (bytesPerSec < 1000000.0)
                                mDownloadSpeed.setText(String.format("%.02f", bytesPerSec / 1024, Locale.US).concat(" KB/s"));
                            else
                                mDownloadSpeed.setText((String.format("%.02f", (bytesPerSec / 1024) / 1024, Locale.US)).concat(" MB/s"));
                        }
                        else
                            mDownloadSpeed.setVisibility(View.INVISIBLE);

                        mProgressCircle.setProgress(bytes_downloaded);
                        mProgressCircle.setVisibility(View.VISIBLE);

                        mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_cancel));
                        mDownloadImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                CancelDownload();
                            }
                        });
                        findViewById(R.id.podcast_episode_download_speed).setVisibility(View.VISIBLE);
                        mDownloadProgressHandler.postDelayed(downloadProgress, 1000);
                        break;
                    case DownloadManager.STATUS_FAILED:
                        mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_circle));

                        mDownloadImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                handleNetwork(true);
                            }
                        });
                        mPlayPauseImage.setVisibility(View.VISIBLE);
                        mProgressCircle.setVisibility(View.INVISIBLE);
                        mDownloadSpeed.setVisibility(View.INVISIBLE);
                        mInfoLayout.setVisibility(View.GONE);
                        mControlsLayout.setVisibility(View.VISIBLE);
                        //mVolumeDown.setVisibility(View.GONE);
                        mVolumeUp.setVisibility(View.GONE);
                        mDownloadManager.remove(mDownloadId);
                        final SharedPreferences.Editor editor = prefs.edit();
                        Utilities.DeleteMediaFile(mContext, mEpisode);

                        if (prefs.getBoolean("pref_downloads_restart_on_failure", true)) {

                            final int count = prefs.getInt("downloads_" + mEpisode.getEpisodeId(), 0);
                            if (count < 10) {
                                mDownloadStartTime = System.nanoTime();
                                Utilities.startDownload(mContext, mEpisode);
                                editor.putInt("downloads_" + mEpisode.getEpisodeId(), count + 1);
                                showToast(mContext, mContext.getString(R.string.alert_download_error_restart));
                            } else {
                                editor.putInt("downloads_" + mEpisode.getEpisodeId(), 0);
                                final int downloadCount = prefs.getInt("new_downloads_count", 0);

                                if (downloadCount > 0)
                                    editor.putInt("new_downloads_count", downloadCount - 1);
                                showToast(mContext, mContext.getString(R.string.alert_download_error_failed));
                            }
                        }
                        else
                        {
                            final int downloadCount = prefs.getInt("new_downloads_count", 0);

                            if (downloadCount > 0)
                                editor.putInt("new_downloads_count", downloadCount - 1);
                        }
                        editor.apply();
                        /*
                        if (prefs.getBoolean("pref_downloads_restart_on_failure", true)) {
                            SystemClock.sleep(3000);
                            mDownloadId = DBUtilities.GetDownloadIDByEpisode(mContext, mEpisode);
                            mDownloadProgressHandler.postDelayed(downloadProgress, 1000);
                        }
                        else {
                            showToast(mActivity, Utilities.GetDownloadErrorReason(mContext, reason));
                            mDownloadProgressHandler.removeCallbacksAndMessages(downloadProgress);
                        }
                        */
                    case DownloadManager.STATUS_SUCCESSFUL:
                        mPlayPauseImage.setVisibility(View.VISIBLE);
                        mControlsLayout.setVisibility(View.VISIBLE);
                        mInfoLayout.setVisibility(View.GONE);
                        mVolumeUp.setVisibility(View.GONE);
                        mProgressCircle.setVisibility(View.INVISIBLE);
                        mDownloadSpeed.setVisibility(View.INVISIBLE);
                        mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_delete2));

                        mDownloadImage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                DeleteEpisode();
                            }
                        });
                        mDownloadProgressHandler.removeCallbacksAndMessages(downloadProgress);
                }
            }
            cursor.close();
        }
    };

    private void handleNetwork(final Boolean download) {
        if (CommonUtils.getActiveNetwork(mActivity) == null)
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                alert.setMessage(getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"), download ? 1 : 2);
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
        else if (CommonUtils.HighBandwidthNetwork(mActivity) == false)
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(PodcastEpisodeActivity.this);
                alert.setMessage(getString(R.string.alert_episode_network_no_high_bandwidth));
                alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"), download ? 1 : 2);
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
        else {
            if (download)
                DownloadEpisode();
            else
                StreamEpisode();
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
                CommonUtils.showToast(mContext, getString(R.string.alert_streaming));
            }
        });

        final Bundle extras = new Bundle();
        extras.putInt("id", mEpisode.getEpisodeId());
        extras.putString("local_file", mLocalFile);
        extras.putInt("playlistid", mPlaylistID);
        mCurrentState = STATE_PLAYING;

        MediaControllerCompat.getMediaController(mActivity).getTransportControls().playFromUri(Uri.parse(mEpisode.getMediaUrl().toString()), extras);
    }

    private void DownloadEpisode() {
        mPlayPauseImage.setVisibility(View.INVISIBLE);
        mProgressCircle.setVisibility(View.VISIBLE);
        mDownloadSpeed.setVisibility(View.VISIBLE);
        mDownloadStartTime = System.nanoTime();
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
        mProgressCircle.setVisibility(View.INVISIBLE);
        mDownloadSpeed.setVisibility(View.INVISIBLE);
        //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
        //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_play_dark : R.drawable.ic_action_episode_play));
        mPlayPauseImage.setVisibility(View.VISIBLE);
        mSeekBar.setVisibility(View.GONE);
        mInfoLayout.setVisibility(View.GONE);
        //mVolumeDown.setVisibility(View.GONE);
        mVolumeUp.setVisibility(View.GONE);
        Utilities.DeleteMediaFile(mActivity, mEpisode);
        mDownloadImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_download_circle));

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
    }

    public void DeleteEpisode() {
        if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
            final AlertDialog.Builder alert = new AlertDialog.Builder(PodcastEpisodeActivity.this);
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
                mDownloadImage.setVisibility(View.GONE);
                mDownloadSpeed.setVisibility(View.GONE);
                mProgressCircle.setVisibility(View.GONE);

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