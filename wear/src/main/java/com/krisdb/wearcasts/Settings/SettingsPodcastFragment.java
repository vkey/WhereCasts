package com.krisdb.wearcasts.Settings;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.CacheUtils;
import com.krisdb.wearcasts.Utilities.DBUtilities;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;
import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcast;

public class SettingsPodcastFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Activity mActivity;
    private int mPodcastId;
    private PodcastItem mPodcast;
    private static WeakReference<Activity> mActivityRef;

    public static SettingsPodcastFragment newInstance(int podcastId) {

        final SettingsPodcastFragment pf = new SettingsPodcastFragment();
        final Bundle bundle = new Bundle();
        bundle.putInt("podcastId", podcastId);
        pf.setArguments(bundle);

        return pf;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_podcast);

        mActivity = getActivity();
        mActivityRef = new WeakReference<>(mActivity);

        final Resources resources = mActivity.getResources();

        mPodcastId = getArguments().getInt("podcastId");

        mPodcast = GetPodcast(mActivity, mPodcastId);

        final PreferenceCategory category = (PreferenceCategory)findPreference("podcast_settings");

        int count = 0;

        final EditTextPreference etRename = new EditTextPreference(mActivity);
        etRename.setText(mPodcast.getChannel().getTitle());
        etRename.setTitle(mPodcast.getChannel().getTitle());
        etRename.setSummary(R.string.rename);
        etRename.getEditText().setImeOptions(EditorInfo.IME_ACTION_DONE);
        etRename.setOrder(count++);

        etRename.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE){
                    etRename.setText(v.getText().toString());
                    etRename.setTitle(v.getText().toString());
                    CacheUtils.deletePodcastsCache(mActivity);
                    etRename.onClick(etRename.getDialog(), Dialog.BUTTON_POSITIVE);
                    etRename.getDialog().dismiss();
                    return true;
                }
                return false;
            }
        });

        etRename.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String value = (String) newValue;

                if (value == null || value.length() == 0) {
                    CommonUtils.showToast(mActivity, getString(R.string.validation_podcast_rename_title));
                    return true;
                }

                if (value.length() > 100) {
                    CommonUtils.showToast(mActivity, getString(R.string.validation_podcast_rename_length));
                    return true;
                }

                final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                final ContentValues cv = new ContentValues();
                cv.put("title", value);
                db.updatePodcast(cv, mPodcast.getPodcastId());
                db.close();
                return true;
            }
        });

        final SwitchPreference cbAutoDownload = new SwitchPreference(mActivity);
        cbAutoDownload.setTitle(R.string.settings_podcast_label_autodownload);
        cbAutoDownload.setKey("pref_" + mPodcastId + "_auto_download");
        cbAutoDownload.setChecked(false);
        cbAutoDownload.setOrder(count++);

        final ListPreference lpDownloadsCount = new ListPreference(mActivity);
        lpDownloadsCount.setTitle(R.string.settings_podcast_label_episodes_downloaded);
        lpDownloadsCount.setKey("pref_" + mPodcastId + "_downloaded_episodes_count");
        lpDownloadsCount.setEntries(R.array.downloaded_episodes_count_text);
        lpDownloadsCount.setEntryValues(R.array.downloaded_episodes_count_values);
        lpDownloadsCount.setIcon(ContextCompat.getDrawable(mActivity, R.drawable.ic_setting_dropdown_indicator));
        lpDownloadsCount.setOrder(count++);

        final ListPreference lpDownloadsSaved = new ListPreference(mActivity);
        lpDownloadsSaved.setTitle(R.string.settings_podcast_label_downloads_saved);
        lpDownloadsSaved.setKey("pref_" + mPodcastId + "_downloads_saved");
        lpDownloadsSaved.setEntries(R.array.downloads_saved_text);
        lpDownloadsSaved.setEntryValues(R.array.downloads_saved_values);
        lpDownloadsSaved.setIcon(ContextCompat.getDrawable(mActivity, R.drawable.ic_setting_dropdown_indicator));
        lpDownloadsSaved.setOrder(count++);

        final SwitchPreference cbDownloadNext = new SwitchPreference(mActivity);
        cbDownloadNext.setTitle(R.string.settings_podcast_label_download_next);
        cbDownloadNext.setKey("pref_" + mPodcastId + "_download_next");
        cbDownloadNext.setChecked(false);
        cbDownloadNext.setOrder(count++);

        /*
        final ListPreference lpDownloadNextCount = new ListPreference(mActivity);
        lpDownloadNextCount.setTitle("Auto-download next count");
        lpDownloadNextCount.setKey("pref_" + mPodcastId + "_download_next_count");
        lpDownloadNextCount.setEntries(R.array.numbers_text);
        lpDownloadNextCount.setEntryValues(R.array.numbers_values);
        lpDownloadNextCount.setIcon(ContextCompat.getDrawable(mActivity, R.drawable.ic_setting_dropdown_indicator));
        lpDownloadNextCount.setDefaultValue(String.valueOf(resources.getInteger(R.integer.default_download_next_count)));
        lpDownloadNextCount.setOrder(count++);
        */

        final ListPreference lpSortOrder = new ListPreference(mActivity);
        lpSortOrder.setTitle(R.string.settings_podcast_label_download_sort_order);
        lpSortOrder.setKey("pref_" + mPodcastId + "_sort_order");
        lpSortOrder.setEntries(R.array.sort_order_episodes_text);
        lpSortOrder.setEntryValues(R.array.sort_order_episodes_values);
        lpSortOrder.setIcon(ContextCompat.getDrawable(mActivity, R.drawable.ic_setting_dropdown_indicator));
        lpSortOrder.setDefaultValue(String.valueOf(resources.getInteger(R.integer.default_episodes_sort_order)));
        lpSortOrder.setOrder(count++);
        lpSortOrder.setSummary(lpSortOrder.getEntry());

        final ListPreference lpPlaybackSpeed = new ListPreference(mActivity);
        lpPlaybackSpeed.setTitle(R.string.settings_podcast_label_download_playback_speed);
        lpPlaybackSpeed.setKey("pref_" + mPodcastId + "_playback_speed");
        lpPlaybackSpeed.setEntries(R.array.playback_speed_podcast_text);
        lpPlaybackSpeed.setEntryValues(R.array.playback_speed_podcast_values);
        lpPlaybackSpeed.setIcon(ContextCompat.getDrawable(mActivity, R.drawable.ic_setting_dropdown_indicator));
        lpPlaybackSpeed.setDefaultValue("0");
        lpPlaybackSpeed.setOrder(count++);
        lpPlaybackSpeed.setSummary(lpPlaybackSpeed.getEntry());

        final List<PlaylistItem> playlistItems = getPlaylists(mActivity);

        final PlaylistItem playlistEmpty = new PlaylistItem();
        playlistEmpty.setID(resources.getInteger(R.integer.default_playlist_select));
        playlistEmpty.setName(getString(R.string.none));
        playlistItems.add(0, playlistEmpty);

        final ArrayList<String> entriesText = new ArrayList<>();

        for (final PlaylistItem playlist : playlistItems)
            entriesText.add(playlist.getName());

        final ArrayList<String> entriesValue = new ArrayList<>();

        for (final PlaylistItem playlist : playlistItems)
            entriesValue.add(String.valueOf(playlist.getID()));

        final ListPreference lpPlaylist = new ListPreference(mActivity);
        lpPlaylist.setTitle(getString(R.string.settings_podcast_label_assign_playlist));
        lpPlaylist.setKey("pref_" + mPodcastId + "_auto_assign_playlist");
        lpPlaylist.setEntries(entriesText.toArray(new CharSequence[entriesText.size()]));
        lpPlaylist.setEntryValues(entriesValue.toArray(new CharSequence[entriesValue.size()]));
        lpPlaylist.setIcon(ContextCompat.getDrawable(mActivity, R.drawable.ic_setting_dropdown_indicator));
        lpPlaylist.setDefaultValue(String.valueOf(resources.getInteger(R.integer.default_playlist_select)));
        lpPlaylist.setOrder(count++);
        lpPlaylist.setSummary(lpPlaylist.getEntry());

        final ListPreference lpSkipStartTime = new ListPreference(mActivity);
        lpSkipStartTime.setTitle(R.string.settings_podcasts_label_skip_start_time);
        lpSkipStartTime.setKey("pref_" + mPodcastId + "_skip_start_time");
        lpSkipStartTime.setEntryValues(R.array.skip_start_time_values);
        lpSkipStartTime.setEntries(R.array.skip_start_time_text);
        lpSkipStartTime.setIcon(ContextCompat.getDrawable(mActivity, R.drawable.ic_setting_dropdown_indicator));
        lpSkipStartTime.setDefaultValue(String.valueOf(resources.getInteger(R.integer.default_skip_start_time)));
        lpSkipStartTime.setOrder(count++);

        final boolean hasPremium = Utilities.hasPremium(mActivity);

        if (!hasPremium)
        {
            lpSkipStartTime.setSummary(getString(R.string.premium_locked_playback_speed));
            lpSkipStartTime.setEnabled(false);
            cbDownloadNext.setSummary(getString(R.string.premium_locked_playback_speed));
            cbDownloadNext.setEnabled(false);
            lpDownloadsSaved.setSummary(getString(R.string.premium_locked_playback_speed));
            lpDownloadsSaved.setEnabled(false);
            lpDownloadsCount.setSummary(getString(R.string.premium_locked_playback_speed));
            lpDownloadsCount.setEnabled(false);
            lpPlaybackSpeed.setSummary(getString(R.string.premium_locked_playback_speed));
            lpPlaybackSpeed.setEnabled(false);
        }
        else
            cbDownloadNext.setSummary(R.string.settings_podcast_label_download_next_summary);

        final ListPreference lpEndStopTime = new ListPreference(mActivity);
        lpEndStopTime.setTitle(R.string.settings_podcasts_label_finish_end_time);
        lpEndStopTime.setKey("pref_" + mPodcastId + "_finish_end_time");
        lpEndStopTime.setEntryValues(R.array.finish_end_time_values);
        lpEndStopTime.setEntries(R.array.finish_end_time_text);
        lpEndStopTime.setIcon(ContextCompat.getDrawable(mActivity, R.drawable.ic_setting_dropdown_indicator));
        lpEndStopTime.setDefaultValue(String.valueOf(resources.getInteger(R.integer.default_finish_end_time)));
        lpEndStopTime.setOrder(count++);

        if (!hasPremium)
        {
            lpEndStopTime.setSummary(getString(R.string.premium_locked_playback_speed));
            lpEndStopTime.setEnabled(false);
        }

        final SwitchPreference cbHidePlayed = new SwitchPreference(mActivity);
        cbHidePlayed.setTitle(R.string.settings_podcast_label_hideplayed);
        cbHidePlayed.setKey("pref_" + mPodcastId + "_hide_played");
        cbHidePlayed.setChecked(false);
        cbHidePlayed.setOrder(count++);

        final Preference pfUnsubscribe = findPreference("pref_delete_podcast");
        pfUnsubscribe.setOrder(count++);
        pfUnsubscribe.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                    alert.setMessage(getString(R.string.confirm_delete_podcast));
                    alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(final DialogInterface dialog, int which) {
                            new AsyncTasks.Unsubscribe(mActivity, mPodcastId,
                                    new Interfaces.BooleanResponse() {
                                        @Override
                                        public void processFinish(Boolean done) {
                                            dialog.dismiss();
                                            mActivity.finish();
                                        }
                                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    });
                    alert.setNegativeButton(getString(R.string.confirm_no), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    alert.show();
                }
                return false;
            }
        });

        category.addPreference(etRename);
        category.addPreference(cbAutoDownload);
        category.addPreference(lpDownloadsCount);
        category.addPreference(lpDownloadsSaved);
        category.addPreference(cbDownloadNext);
        //category.addPreference(lpDownloadNextCount);
        category.addPreference(lpSortOrder);
        category.addPreference(lpPlaybackSpeed);
        category.addPreference(lpPlaylist);
        category.addPreference(lpSkipStartTime);
        category.addPreference(lpEndStopTime);
        category.addPreference(cbHidePlayed);

        findPreference("pref_" + mPodcastId + "_auto_assign_playlist").setSummary(((ListPreference)findPreference("pref_" + mPodcastId + "_auto_assign_playlist")).getEntry());

        if (hasPremium) {
            final int skipTime = Integer.valueOf(((ListPreference) findPreference("pref_" + mPodcastId + "_skip_start_time")).getValue());
            final int endTime = Integer.valueOf(((ListPreference) findPreference("pref_" + mPodcastId + "_finish_end_time")).getValue());

            if (skipTime > 0)
                findPreference("pref_" + mPodcastId + "_skip_start_time").setSummary(((ListPreference) findPreference("pref_" + mPodcastId + "_skip_start_time")).getEntry());

            if (endTime > 0)
                findPreference("pref_" + mPodcastId + "_finish_end_time").setSummary(((ListPreference) findPreference("pref_" + mPodcastId + "_finish_end_time")).getEntry());

            //findPreference("pref_" + mPodcastId + "_download_next_count").setSummary(((ListPreference)findPreference("pref_" + mPodcastId + "_download_next_count")).getEntry());
            findPreference("pref_" + mPodcastId + "_sort_order").setSummary(((ListPreference)findPreference("pref_" + mPodcastId + "_sort_order")).getEntry());

            findPreference("pref_" + mPodcastId + "_playback_speed").setSummary(((ListPreference)findPreference("pref_" + mPodcastId + "_playback_speed")).getEntry());

            final CharSequence downloadsSavedEntry = ((ListPreference)findPreference("pref_" + mPodcastId + "_downloads_saved")).getEntry();

            if (downloadsSavedEntry != null && Integer.parseInt(downloadsSavedEntry.toString()) > 0)
                findPreference("pref_" + mPodcastId + "_downloads_saved").setSummary(downloadsSavedEntry);
            else
                findPreference("pref_" + mPodcastId + "_downloads_saved").setSummary(R.string.settings_podcast_label_downloads_saved_summary);

            final CharSequence lpEpisodeCount = ((ListPreference)findPreference("pref_" + mPodcastId + "_downloaded_episodes_count")).getEntry();

            if (lpEpisodeCount != null && Integer.parseInt(lpEpisodeCount.toString()) > 0)
                findPreference("pref_" + mPodcastId + "_downloaded_episodes_count").setSummary(lpEpisodeCount);
            else
                findPreference("pref_" + mPodcastId + "_downloaded_episodes_count").setSummary(R.string.settings_podcast_label_episodes_downloaded_summary);
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        if (prefs.getBoolean("long_press_tip_shown", false) == false) {
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("long_press_tip_shown", true);
            editor.apply();
        }

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        SystemClock.sleep(500);

        final Boolean hasPremium = Utilities.hasPremium(mActivity);
        if (hasPremium)
        {
            final int skipTime = Integer.valueOf(((ListPreference) findPreference("pref_" + mPodcastId + "_skip_start_time")).getValue());
            final int endTime = Integer.valueOf(((ListPreference) findPreference("pref_" + mPodcastId + "_finish_end_time")).getValue());

            if (key.equals("pref_" + mPodcastId + "_skip_start_time"))
                findPreference("pref_" + mPodcastId + "_skip_start_time").setSummary(skipTime > 0 ? ((ListPreference) findPreference("pref_" + mPodcastId + "_skip_start_time")).getEntry() : "");

            if (key.equals("pref_" + mPodcastId + "_finish_end_time"))
                findPreference("pref_" + mPodcastId + "_finish_end_time").setSummary(endTime > 0 ?((ListPreference) findPreference("pref_" + mPodcastId + "_finish_end_time")).getEntry() : "");

            if (key.equals("pref_" + mPodcastId + "_sort_order"))
                findPreference("pref_" + mPodcastId + "_sort_order").setSummary(((ListPreference)findPreference("pref_" + mPodcastId + "_sort_order")).getEntry());

            if (key.equals("pref_" + mPodcastId + "_playback_speed"))
                findPreference("pref_" + mPodcastId + "_playback_speed").setSummary(((ListPreference)findPreference("pref_" + mPodcastId + "_playback_speed")).getEntry());

            if (key.equals("pref_" + mPodcastId + "_downloads_saved")) {
                final CharSequence downloadsSavedEntry = ((ListPreference) findPreference("pref_" + mPodcastId + "_downloads_saved")).getEntry();

                if (downloadsSavedEntry != null)
                    findPreference("pref_" + mPodcastId + "_downloads_saved").setSummary(((ListPreference) findPreference("pref_" + mPodcastId + "_downloads_saved")).getEntry());
            }

            if (key.equals("pref_" + mPodcastId + "_downloaded_episodes_count")) {
                final CharSequence downloadsSavedEntry = ((ListPreference) findPreference("pref_" + mPodcastId + "_downloaded_episodes_count")).getEntry();

                if (downloadsSavedEntry != null)
                    findPreference("pref_" + mPodcastId + "_downloaded_episodes_count").setSummary(((ListPreference) findPreference("pref_" + mPodcastId + "_downloaded_episodes_count")).getEntry());
            }
        }

        if (key.equals("pref_" + mPodcastId + "_auto_assign_playlist"))
            findPreference("pref_" + mPodcastId + "_auto_assign_playlist").setSummary(((ListPreference)findPreference("pref_" + mPodcastId + "_auto_assign_playlist")).getEntry());

        if (key.equals("pref_" + mPodcastId + "_download_next") && ((SwitchPreference)findPreference("pref_" + mPodcastId + "_download_next")).isChecked())
            CommonUtils.showToast(mActivity, getString(R.string.settings_podcast_label_download_next_alert));
    }
}
