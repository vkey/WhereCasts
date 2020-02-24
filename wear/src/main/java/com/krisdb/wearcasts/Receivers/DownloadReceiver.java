package com.krisdb.wearcasts.Receivers;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;

import com.krisdb.wearcasts.Activities.MainActivity;
import com.krisdb.wearcasts.Async.CleanupDownloads;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Models.DownloadComplete;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.Async.GetRedirectURL;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import org.greenrobot.eventbus.EventBus;

import java.util.Date;
import java.util.Objects;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodeByDownloadID;
import static com.krisdb.wearcastslibrary.CommonUtils.isCurrentDownload;

public class DownloadReceiver extends BroadcastReceiver  {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int failedDownloadReason = 0;
        final DownloadManager managerDownload = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);

        if (Objects.equals(intent.getAction(), DownloadManager.ACTION_NOTIFICATION_CLICKED)) {
            final Intent intentMain = new Intent(context, MainActivity.class);
            intentMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK );
            context.startActivity(intentMain);
        } else if (Objects.equals(intent.getAction(), DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {

            //https://stackoverflow.com/questions/14073323/is-it-possible-to-cancel-stop-a-download-started-using-downloadmanager
            final long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);

            if (downloadId == 0) return;

            final PodcastItem episode = GetEpisodeByDownloadID(context, (int) downloadId);

            final DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);

            final Cursor cursor = managerDownload.query(query);

            if (cursor.moveToFirst()) {
                final int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                //final String path = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    //CommonUtils.writeToFile(context,"download successful (" + episode.getTitle() + ")");

                    final ContentValues cvSuccess = new ContentValues();
                    cvSuccess.put("download", 1);
                    cvSuccess.put("downloadid", 0);
                    cvSuccess.put("dateDownload", DateUtils.GetDate());

                    final DBPodcastsEpisodes db = new DBPodcastsEpisodes(context);
                    db.update(cvSuccess, episode.getEpisodeId());
                    db.close();

                    final String disableStart = prefs.getString("pref_download_sound_disable_start", "0");
                    final String disableEnd = prefs.getString("pref_download_sound_disable_end", "0");

                    boolean playSound = prefs.getBoolean("pref_download_complete_sound", true);

                    if (playSound && DateUtils.isTimeBetweenTwoTime(disableStart, disableEnd, DateUtils.FormatDate(new Date(), "HH:mm:ss")))
                        playSound = false;

                    if (playSound) {
                        final MediaPlayer mPlayer = MediaPlayer.create(context, R.raw.download_complete2);
                        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mPlayer.start();
                    }

                    if (prefs.getBoolean("pref_hide_empty_playlists", false) && Utilities.downloadsCount(context) == 1) {
                        final SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("refresh_vp", true);
                        editor.apply();
                    }

                } else if (status == DownloadManager.STATUS_FAILED) {
                    failedDownloadReason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));

                    if (failedDownloadReason == DownloadManager.ERROR_TOO_MANY_REDIRECTS) {
                        CommonUtils.executeAsync(new GetRedirectURL(episode.getMediaUrl()), (new_url) -> {
                            if (new_url != null) {
                                episode.setMediaUrl(new_url.toString());
                                Utilities.startDownload(context, episode, false);
                            } else
                                CommonUtils.showToast(context, Utilities.GetDownloadErrorReason(context, 1));
                        });
                    }

                    //CommonUtils.writeToFile(context,"download failed (" + episode.getTitle() + ")");
                    clearFailedDownload(context, episode);
/*                    final int downloadCount = prefs.getInt("downloads_" + episode.getEpisodeId(), 0);

                    final SharedPreferences.Editor editor = prefs.edit();

                    if (prefs.getBoolean("pref_downloads_restart_on_failure", true) && downloadCount < 10) {
                        long id = startDownload(context, episode);
                        editor.putInt("downloads_" + episode.getEpisodeId(), downloadCount + 1);
                        editor.apply();
                        showToast(context, context.getString(R.string.alert_download_error_restart));
                    } else {
                        showToast(context, context.getString(R.string.alert_download_error_failed));
                        editor.putInt("downloads_" + episode.getEpisodeId(), 0);
                    }

                    editor.apply();*/
                }
            }

            cursor.close();

            if (!isCurrentDownload(context)) {
                if (prefs.getBoolean("from_job", false) && prefs.getBoolean("pref_disable_bluetooth", false) && !Utilities.BluetoothEnabled()) {
                    EventBus.getDefault().post(new DownloadComplete());
                }

                final SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("from_job", false);
                editor.apply();

                if (failedDownloadReason != DownloadManager.ERROR_TOO_MANY_REDIRECTS) {
                    Utilities.enableBluetooth(context, !prefs.getBoolean("from_job", false));
                    CommonUtils.executeSingleThreadAsync(new CleanupDownloads(context), (response) -> { });
                }

                //clear all downloads just in case
                final Cursor cursorClear = managerDownload.query(new DownloadManager.Query());

                if (cursorClear.moveToFirst()) {
                    while (!cursorClear.isAfterLast()) {
                        managerDownload.remove(cursorClear.getInt(cursorClear.getColumnIndex(DownloadManager.COLUMN_STATUS)));
                        cursorClear.moveToNext();
                    }
                }
                cursorClear.close();
            }
        }
    }

    private void clearFailedDownload(final Context context, final PodcastItem episode) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        final ContentValues cvFailed = new ContentValues();
        cvFailed.put("download", 0);
        cvFailed.put("downloadid", 0);

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(context);
        db.update(cvFailed, episode.getEpisodeId());
        db.close();

        Utilities.DeleteMediaFile(context, episode);

        final SharedPreferences.Editor editor = prefs.edit();
        final int downloadCount = prefs.getInt("new_downloads_count", 0);

        if (downloadCount > 0) {
            editor.putInt("new_downloads_count", downloadCount - 1);
            editor.apply();
        }
    }
}