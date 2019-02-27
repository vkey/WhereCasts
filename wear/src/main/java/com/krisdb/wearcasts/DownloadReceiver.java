package com.krisdb.wearcasts;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static android.content.Context.DOWNLOAD_SERVICE;

public class DownloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (Objects.equals(intent.getAction(), DownloadManager.ACTION_NOTIFICATION_CLICKED)) {
            context.startActivity(new Intent(context, MainActivity.class));
        } else if (Objects.equals(intent.getAction(), DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {

            final long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);

            if (downloadId == 0) return;

            final PodcastItem episode = DBUtilities.GetEpisodeByDownloadID(context, (int) downloadId);

            final DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            final DownloadManager manager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);

            final Cursor cursor = manager.query(query);

            if (cursor.moveToFirst()) {
                final int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                final String path = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    //Log.d(context.getPackageName(), "Background service: Download Finished (" + episode.getTitle() + ")");
                    final ContentValues cvSuccess = new ContentValues();
                    cvSuccess.put("download", 1);
                    cvSuccess.put("downloadid", 0);
                    //cvSuccess.put("downloadurl", path);
                    cvSuccess.put("dateDownload", DateUtils.GetDate());

                    final DBPodcastsEpisodes db = new DBPodcastsEpisodes(context);
                    db.update(cvSuccess, episode.getEpisodeId());
                    db.close();

                    final String disableStart = prefs.getString("pref_download_sound_disable_start", "0");
                    final String disableEnd = prefs.getString("pref_download_sound_disable_end", "0");

                    Boolean playSound = prefs.getBoolean("pref_download_complete_sound", true);

                    if (playSound && DateUtils.isTimeBetweenTwoTime(disableStart, disableEnd, DateUtils.FormatDate(new Date(), "HH:mm:ss")))
                        playSound = false;

                    if (playSound) {
                        final MediaPlayer mPlayer = MediaPlayer.create(context, R.raw.download_complete2);
                        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mPlayer.start();
                    }

                    if (prefs.getBoolean("pref_hide_empty_playlists", false) && Utilities.downloadsCount() == 1) {
                        final SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("refresh_vp", true);
                        editor.apply();
                    }
                } else if (status == DownloadManager.STATUS_FAILED) {

                }
            }

            cursor.close();

            /*
            if (prefs.getBoolean("enable_bluetooth", false) && isCurrentDownload(context) == false)
            {
                final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

                if (adapter != null && adapter.isEnabled() == false)
                    adapter.enable();
            }
            */

            //if (prefs.getBoolean("cleanup_downloads", false) && isCurrentDownload(context) == false)
            {
                final List<PodcastItem> podcasts = DBUtilities.GetPodcasts(context);

                for (final PodcastItem podcast : podcasts) {
                    final int downloadsSaved = Integer.valueOf(prefs.getString("pref_" + podcast.getPodcastId() + "_downloads_saved", "0"));

                    if (downloadsSaved > 0) {
                        List<PodcastItem> downloads = DBUtilities.GetEpisodesWithDownloads(context, podcast.getPodcastId(), downloadsSaved);

                        if (downloads.size() > 0) {

                            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(context);

                            for (final PodcastItem download : downloads) {
                                Utilities.DeleteMediaFile(context, download);
                                SystemClock.sleep(500);
                            }
                            db.close();
                        }
                    }

                }
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("cleanup_downloads", false);
                editor.apply();
            }
        }
    }

    private void clearFailedDownload(final Context context, final PodcastItem episode)
    {
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