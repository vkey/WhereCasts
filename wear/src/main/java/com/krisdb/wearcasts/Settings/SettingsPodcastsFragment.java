package com.krisdb.wearcasts.Settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class SettingsPodcastsFragment extends PreferenceFragment {

    private Activity mActivity;
    private static WeakReference<Activity> mActivityRef;
    private Boolean mNoResume = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_podcasts);
        mActivityRef = new WeakReference<>(getActivity());

        mActivity = getActivity();
        //mTimeOutHandler = new TimeOutHandler(this);
        //mManager = (ConnectivityManager)mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        findPreference("pref_updates").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPodcastsUpdatesActivity.class));
                return false;
            }
        });

        findPreference("pref_podcasts").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPodcastsPodcastsActivity.class));
                return false;
            }
        });

        findPreference("pref_episodes").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPodcastsEpisodesActivity.class));
                return false;
            }
        });

        findPreference("pref_display").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPodcastsDisplayActivity.class));
                return false;
            }
        });

        findPreference("pref_downloads").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPodcastsDownloadsActivity.class));
                return false;
            }
        });

        findPreference("pref_playback").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPodcastsPlaybackActivity.class));
                return false;
            }
        });

        findPreference("pref_playlists").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, SettingsPlaylistsActivity.class));
                return false;
            }
        });

        findPreference("pref_sync_podcasts").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                handleNetwork();
                return false;
            }
        });
    }

    private void handleNetwork() {
        //final SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(mActivity);

        if (CommonUtils.getActiveNetwork(mActivity) == null)
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                alert.setMessage(getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), 1);
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
            findPreference("pref_sync_podcasts").setSummary(getString(R.string.syncing));
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            new AsyncTasks.SyncPodcasts(mActivity, 0, true, findPreference("pref_sync_podcasts"),
                    new Interfaces.BackgroundSyncResponse() {
                        @Override
                        public void processFinish(final int newEpisodeCount, final int downloads, final List<PodcastItem> downloadEpisodes) {
                            SetContent(newEpisodeCount);
                            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                findPreference("pref_sync_podcasts").setSummary(getString(R.string.syncing));
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                new AsyncTasks.SyncPodcasts(mActivity, 0, true, findPreference("pref_sync_podcasts"),
                        new Interfaces.BackgroundSyncResponse() {
                            @Override
                            public void processFinish(final int newEpisodeCount, final int downloads, final List<PodcastItem> downloadEpisodes) {
                                SetContent(newEpisodeCount);
                                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void SetContent(final int newEpisodes)
    {
        if (mActivity == null || isAdded() == false) return;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        final String podcastDate = prefs.getString("last_podcast_sync_date", "");

        if (newEpisodes > -1)
            findPreference("pref_sync_podcasts").setSummary(newEpisodes == 1 ? getString(R.string.plurals_single_new_episode) : getString(R.string.plurals_multiple_new_episodes, newEpisodes));
        else if (podcastDate.length() > 0) {
            findPreference("pref_sync_podcasts").setSummary(getString(R.string.last_updated)
                    .concat(":\n")
                    .concat(DateUtils.GetDisplayDate(mActivity, podcastDate, "EEE MMM dd H:mm:ss Z yyyy"))
                    .concat(" @ ")
                    .concat(DateUtils.GetTime(DateUtils.ConvertDate(podcastDate, "EEE MMM dd H:mm:ss Z yyyy"))));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNoResume == false)
            SetContent(-1);
    }
}
