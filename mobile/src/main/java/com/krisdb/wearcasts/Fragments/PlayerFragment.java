package com.krisdb.wearcasts.Fragments;

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.krisdb.wearcasts.Models.MediaPlaybackStatus;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Services.MediaPlayerService;
import com.krisdb.wearcasts.Utilities;
import com.krisdb.wearcastslibrary.DateUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static android.Manifest.permission.READ_PHONE_STATE;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class PlayerFragment extends Fragment {

    private Handler mPositionHandler = new Handler();
    private ImageView mSkipBackImage, mSkipForwardImage, mPlayPauseImage;
    private static final int STATE_PAUSED = 0;
    private static final int STATE_PLAYING = 1;
    private int mCurrentState;
    private MediaBrowserCompat mMediaBrowserCompat;
    private SeekBar mSeekBar;
    private RelativeLayout mInfoLayout;
    private ProgressBar mProgressBar;
    private TextView mPositionView, mDurationView;
    private Activity mActivity;
    private View mView;

    public static PlayerFragment newInstance() {

        final PlayerFragment f = new PlayerFragment();

        //final Bundle bundle = new Bundle();
        //bundle.putSerializable("podcasts", (Serializable)podcasts);
        //plf.setArguments(bundle);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();

        ActivityCompat.requestPermissions(mActivity, new String[]
                {
                        READ_PHONE_STATE
                }, 122);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        setRetainInstance(true);

        mView = inflater.inflate(R.layout.fragment_player, container, false);

        mProgressBar = mView.findViewById(R.id.podcast_progress_bar);
        mSeekBar = mView.findViewById(R.id.sb_podcast_episode);
        mPlayPauseImage = mView.findViewById(R.id.iv_podcast_playpause);
        mSkipBackImage = mView.findViewById(R.id.iv_skip_back);
        mSkipForwardImage = mView.findViewById(R.id.iv_skip_forward);
        mDurationView = mView.findViewById(R.id.tv_podcast_duration);
        mPositionView = mView.findViewById(R.id.tv_podcast_position);
        mInfoLayout = mView.findViewById(R.id.podcast_episode_info_layout);

        mSkipBackImage.setOnClickListener(view -> {
            int position = mSeekBar.getProgress() - 30000;
            mSeekBar.setProgress(position);
            MediaControllerCompat.getMediaController(mActivity).getTransportControls().seekTo(position);
        });

        mSkipForwardImage.setOnClickListener(view -> {
            int position = mSeekBar.getProgress() + 30000;
            mSeekBar.setProgress(position);
            MediaControllerCompat.getMediaController(mActivity).getTransportControls().seekTo(position);
        });

        mPlayPauseImage.setOnClickListener(view -> {
            if (mCurrentState == STATE_PAUSED) {

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

                String media_url = prefs.getString("episode_url", "");
                //String media_url = "http://media.adknit.com/a/f/11/amanpour/bczdoq.1-1.mp3";

                Bundle extras = new Bundle();
                extras.putString("episode_url", media_url);

                boolean isDownloaded = false;

                //downloaded
                if (isDownloaded) {

                    //MediaControllerCompat.getMediaController(mActivity).getTransportControls().playFromUri(Uri.parse(Utilities.GetMediaFile(mActivity, mEpisode)), extras);

                    mCurrentState = STATE_PLAYING;
                    mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
                    mInfoLayout.setVisibility(View.VISIBLE);
                } else if (Utilities.IsNetworkConnected(mActivity) == false) {
                    Toast.makeText(mActivity, "No network available", Toast.LENGTH_SHORT).show();
                    mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
                    mProgressBar.setVisibility(View.GONE);
                    mInfoLayout.setVisibility(View.GONE);
                    mCurrentState = STATE_PAUSED;
                } else {
                    //stream
                    MediaControllerCompat.getMediaController(mActivity).getTransportControls().playFromUri(Uri.parse(media_url), extras);

                    mCurrentState = STATE_PLAYING;
                    mPlayPauseImage.setBackgroundResource(R.drawable.ic_action_episode_pause);
                    mInfoLayout.setVisibility(View.VISIBLE);
                }
                currentPositionDisplay.run();
            } else {
                //pause episode
                MediaControllerCompat.getMediaController(mActivity).getTransportControls().pause();
                mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
                mProgressBar.setVisibility(View.GONE);
                mInfoLayout.setVisibility(View.GONE);
                mSkipBackImage.setVisibility(View.INVISIBLE);
                mSkipForwardImage.setVisibility(View.INVISIBLE);
                mCurrentState = STATE_PAUSED;
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MediaControllerCompat.getMediaController(mActivity).getTransportControls().seekTo(seekBar.getProgress());
            }
        });

        mMediaBrowserCompat = new MediaBrowserCompat(
                mActivity,
                new ComponentName(mActivity, MediaPlayerService.class),
                mMediaBrowserCompatConnectionCallback,
                mActivity.getIntent().getExtras()
        );

        mMediaBrowserCompat.connect();

        return mView;
    }

    @Override
    public void onActivityCreated(final Bundle icicle)
    {
        super.onActivityCreated(icicle);
    }

    @Override
    public void onResume() {
        super.onResume();
        SetContent();

        EventBus.getDefault().register(this);
    }


    private void SetContent() {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        ((TextView) mView.findViewById(R.id.podcast_title)).setText(prefs.getString("podcast_title", ""));
        ((TextView) mView.findViewById(R.id.podcast_episode_title)).setText(prefs.getString("episode_title", ""));

        if (prefs.getBoolean("isplaying", false)) {
            mCurrentState = STATE_PLAYING;

            //Log.d(mContext.getPackageName(), "Current playing: " + isCurrentlyPlaying);

            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_pause));
            mSeekBar.setMax(prefs.getInt("duration", 0));
            mSeekBar.setProgress(prefs.getInt("position", 0));
            mDurationView.setText(DateUtils.FormatPositionTime(prefs.getInt("duration", 0)));
            mInfoLayout.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            mSkipForwardImage.setVisibility(View.VISIBLE);
            mSkipBackImage.setVisibility(View.VISIBLE);
            currentPositionDisplay.run();

        } else {
            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
            mCurrentState = STATE_PAUSED;
            mInfoLayout.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.GONE);
            mSkipForwardImage.setVisibility(View.INVISIBLE);
            mSkipBackImage.setVisibility(View.INVISIBLE);
        }
    }

    private MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();
            try {
                MediaControllerCompat mcc = new MediaControllerCompat(mActivity, mMediaBrowserCompat.getSessionToken());
                mcc.registerCallback(mMediaControllerCompatCallback);
                MediaControllerCompat.setMediaController(mActivity, mcc);

                SetContent();

            } catch( RemoteException e ) {
                Log.e(mActivity.getPackageName(), e.toString());
            }
        }
    };

    private MediaControllerCompat.Callback mMediaControllerCompatCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
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
            mInfoLayout.setVisibility(View.VISIBLE);
        }
        else if (status.getMediaStart())
        {
            mProgressBar.setIndeterminate(true);
            mProgressBar.setVisibility(ProgressBar.VISIBLE);
            mInfoLayout.setVisibility(View.GONE);
            mSkipForwardImage.setVisibility(View.INVISIBLE);
            mSkipBackImage.setVisibility(View.INVISIBLE);
            mDurationView.setVisibility(View.INVISIBLE);
            mPositionView.setVisibility(View.INVISIBLE);
            mPlayPauseImage.setEnabled(false);
        }
        else if (status.getMediaPaused())
        {
            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
            mProgressBar.setVisibility(View.GONE);
            mInfoLayout.setVisibility(View.GONE);
            mSkipBackImage.setVisibility(View.INVISIBLE);
            mSkipForwardImage.setVisibility(View.INVISIBLE);
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

            showToast(mActivity, message);

            //mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, mThemeID == Enums.ThemeOptions.LIGHT.getThemeId() ? R.drawable.ic_action_episode_play_dark : R.drawable.ic_action_episode_play));
            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
            mInfoLayout.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.GONE);
        }
        else if (status.getMediaCompleted())
        {
            mPlayPauseImage.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_episode_play));
            mSkipForwardImage.setVisibility(View.INVISIBLE);
            mSkipBackImage.setVisibility(View.INVISIBLE);
            mSeekBar.setVisibility(View.INVISIBLE);
            mDurationView.setVisibility(View.INVISIBLE);
            mPositionView.setVisibility(View.INVISIBLE);
            mPlayPauseImage.setEnabled(true);
        }
        else if (status.getMediaSync())
            SetContent();
        else if (status.getMediaPlaying())
        {
            int duration = PreferenceManager.getDefaultSharedPreferences(mActivity).getInt("duration", 0);
            mProgressBar.setVisibility(ProgressBar.GONE);
            mInfoLayout.setVisibility(View.VISIBLE);
            mSeekBar.setMax(duration);
            mSkipForwardImage.setVisibility(View.VISIBLE);
            mSkipBackImage.setVisibility(View.VISIBLE);
            mDurationView.setText(DateUtils.FormatPositionTime(duration));
            mDurationView.setVisibility(View.VISIBLE);
            mPositionView.setVisibility(View.VISIBLE);
            mPlayPauseImage.setEnabled(true);
        }
        else if (status.getPosition() > 0)
            mSeekBar.setProgress(status.getPosition());
    }

    private Runnable currentPositionDisplay = new Runnable() {
        @Override
        public void run() {
            int progress = mSeekBar.getProgress();
            //Log.d(mContext.getPackageName(), "Current position: " + progress);
            mPositionView.setText(DateUtils.FormatPositionTime(progress));
            mPositionHandler.postDelayed(currentPositionDisplay, 1000);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mMediaBrowserCompat != null)
            mMediaBrowserCompat.disconnect();
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        mPositionHandler.removeCallbacks(currentPositionDisplay);
    }
}