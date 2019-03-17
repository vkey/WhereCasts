package com.krisdb.wearcasts;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Utilities.CacheUtils;
import com.krisdb.wearcasts.Utilities.EpisodeUtilities;
import com.krisdb.wearcasts.Utilities.PlaylistsUtilities;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodes;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.SearchEpisodes;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.getNextEpisodeNotDownloaded;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylistItems;
import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcast;
import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcasts;
import static com.krisdb.wearcasts.Utilities.Utilities.ProcessEpisodes;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.GetThumbnailDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.getCurrentPosition;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class AsyncTasks {

    private static WeakReference<Context> mContext;

    public static class FinishMedia extends AsyncTask<Void, Void, Void> {

        private PodcastItem mEpisode;
        private String mLocalFile;
        private Interfaces.PodcastsResponse mResponse;
        private int mPlaylistID, mPodcastID;
        private List<PodcastItem> mEpisodes;

        public FinishMedia(final Context context, final PodcastItem episode, final int playlistId, final int podcastId, final String localFile, final Interfaces.PodcastsResponse response)
        {
            mContext = new WeakReference<>(context);
            mEpisode = episode;
            mLocalFile = localFile;
            mPlaylistID = playlistId;
            mPodcastID = podcastId;
            mResponse = response;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Context ctx = mContext.get();

            mEpisodes = mPodcastID > -1 ? EpisodeUtilities.GetEpisodes(ctx, mEpisode.getPodcastId()) : getPlaylistItems(mContext.get(), mPlaylistID, mLocalFile == null);

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

            if (prefs.getBoolean("pref_" + mEpisode.getPodcastId() + "_download_next", false)) {
                final PodcastItem nextEpisode = getNextEpisodeNotDownloaded(ctx, mEpisode);

                if (nextEpisode != null && Utilities.getDownloadId(ctx, nextEpisode.getEpisodeId()) == 0)
                    Utilities.startDownload(ctx, nextEpisode);
            }

            if (mLocalFile == null) {
                final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
                final ContentValues cv = new ContentValues();
                cv.put("finished", 1);
                cv.put("playing", 0);
                cv.put("buffering", 0);
                cv.put("position", 0);
                db.update(cv, mEpisode.getEpisodeId());

                if (prefs.getBoolean("pref_auto_delete", true))
                    Utilities.DeleteMediaFile(ctx, mEpisode);

                if (mPlaylistID != -1 && prefs.getBoolean("pref_remove_playlist_onend", false))
                    db.deleteEpisodeFromPlaylists(mEpisode.getEpisodeId());

                db.close();
            }
            else
            {
                if (prefs.getBoolean("pref_auto_delete", true))
                {
                    final File localFile = new File(GetLocalDirectory(ctx).concat(mLocalFile));

                    if (localFile.exists())
                        localFile.delete();
                }

                final SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(Utilities.GetLocalPositionKey(mLocalFile), 0);
                editor.apply();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mResponse.processFinish(mEpisodes);
        }
    }

    public static class Unsubscribe extends AsyncTask<Void, Void, Void> {
        private Interfaces.BooleanResponse mResponse;
        private int mPodcastID;
        @Override
        protected void onPreExecute() {
            CommonUtils.showToast(mContext.get(), mContext.get().getString(R.string.alert_unsubscribing));
        }

        public Unsubscribe(final Context context, final int podcastId, final Interfaces.BooleanResponse response)
        {
            mContext = new WeakReference<>(context);
            mPodcastID = podcastId;
            mResponse = response;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Context ctx = mContext.get();
            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
            db.deletePodcast(mPodcastID);
            Utilities.DeleteFiles(ctx, mPodcastID);
            db.unsubscribe(ctx, mPodcastID);
            db.close();
            CacheUtils.deletePodcastsCache(ctx);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mResponse.processFinish(true);
        }
    }

    public static class ToggleBluetooth extends AsyncTask<Void, Void, Void> {
        private Interfaces.AsyncResponse mResponse;
        private Boolean mDisable;

        public ToggleBluetooth(final Context context, final Boolean disable, final Interfaces.AsyncResponse response)
        {
            mContext = new WeakReference<>(context);
            mDisable = disable;
            mResponse = response;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

            if (mDisable)
                adapter.disable();
            else
                adapter.enable();

            SystemClock.sleep(1300);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            CommonUtils.showToast(mContext.get(), mContext.get().getString(mDisable ? R.string.alert_disable_bluetooth_disabled_end : R.string.alert_disable_bluetooth_enabled));
            mResponse.processFinish();
        }
    }

    public static class DownloadMultipleEpisodes extends AsyncTask<Void, Void, Void> {
        private Interfaces.AsyncResponse mResponse;
        private List<PodcastItem> mEpisodes;

        public DownloadMultipleEpisodes(final Context context, final List<PodcastItem> episodes, final Interfaces.AsyncResponse response)
        {
            mContext = new WeakReference<>(context);
            mEpisodes = episodes;
            mResponse = response;
        }

        @Override
        protected void onPreExecute() {
            CommonUtils.showToast(mContext.get(), mContext.get().getString(mEpisodes.size() == 1 ? R.string.alert_download_episode_selected : R.string.alert_download_episodes_selected, mEpisodes.size()));
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (final PodcastItem episode : mEpisodes) {
                Utilities.startDownload(mContext.get(), episode);
                SystemClock.sleep(500);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mResponse.processFinish();
        }
    }

    public static class DisplayPodcasts extends AsyncTask<Void, Void, Void> {
        private Interfaces.PodcastsResponse mResponse;
        private List<PodcastItem> mPodcasts;

        public DisplayPodcasts(final Context context, final Interfaces.PodcastsResponse response) {
            mContext = new WeakReference<>(context);
            mResponse = response;
       }

        @Override
        protected Void doInBackground(Void... params) {

            final Context ctx = mContext.get();

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            final Boolean hideEmpty = prefs.getBoolean("pref_hide_empty", false);
            final Boolean showDownloaded = prefs.getBoolean("pref_display_show_downloaded", false);

            mPodcasts = GetPodcasts(ctx, hideEmpty, showDownloaded);

            return null;
        }

        protected void onPostExecute(Void param) {
            mResponse.processFinish(mPodcasts);
        }
    }

    public static class DisplayEpisodes extends AsyncTask<Void, Void, Void> {
        private int mPodcastId, mPlayListId;
        private Interfaces.PodcastsResponse mResponse;
        private List<PodcastItem> mEpisodes;
        private String mQuery;

        public DisplayEpisodes(final Context context, final int playlistId, final Interfaces.PodcastsResponse response) {
            mContext = new WeakReference<>(context);
            mPodcastId = -1;
            mPlayListId = playlistId;
            mQuery = null;
            mResponse = response;
        }

        public DisplayEpisodes(final Context context, final int podcastId, final String query, final Interfaces.PodcastsResponse response) {
            mContext = new WeakReference<>(context);
            mPodcastId = podcastId;
            mPlayListId = -1;
            mQuery = query;
            mResponse = response;
       }

        @Override
        protected Void doInBackground(Void... params) {

            final Context ctx = mContext.get();

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

            final boolean hidePlayed = prefs.getBoolean("pref_" + mPodcastId + "_hide_played", false);

            final int numberOfEpisode = Integer.valueOf(prefs.getString("pref_episode_limit", ctx.getString(R.string.episode_list_default)));

            if (mQuery == null)
                mEpisodes = GetEpisodes(ctx, mPodcastId, hidePlayed, numberOfEpisode, null);
            else
                mEpisodes = SearchEpisodes(ctx, mPodcastId, mQuery);

            return null;
        }

        protected void onPostExecute(Void param) {
            mResponse.processFinish(mEpisodes);
        }
    }

    public static class DisplayPlaylistEpisodes extends AsyncTask<Void, Void, Void> {
        private int mPlayListId;
        private Interfaces.PodcastsResponse mResponse;
        private List<PodcastItem> mEpisodes;

        public DisplayPlaylistEpisodes(final Context context, final int playlistId, final Interfaces.PodcastsResponse response) {
            mContext = new WeakReference<>(context);
            mPlayListId = playlistId;
            mResponse = response;
        }

        @Override
        protected Void doInBackground(Void... params) {

            mEpisodes = PlaylistsUtilities.GetEpisodes(mContext.get(), mPlayListId);

            return null;
        }

        protected void onPostExecute(Void param) {
            mResponse.processFinish(mEpisodes);
        }
    }

    public static class SyncPodcasts extends AsyncTask<Void, Void, Void> {
        private int mPodcastId, mNewEpisodes, mDownloadCount;
        private int[] mQuantities;
        private Interfaces.BackgroundSyncResponse mResponse;
        private Boolean mDisableToast;

        public SyncPodcasts(final Context context, final int podcastId, final Boolean disableToast, final Interfaces.BackgroundSyncResponse response) {
            mContext = new WeakReference<>(context);
            mResponse = response;
            mPodcastId = podcastId;
            mDisableToast = disableToast;
        }

        @Override
        protected void onPreExecute() {
            if (mDisableToast == false)
                showToast(mContext.get(), mContext.get().getString(R.string.alert_sync_started));
        }

        @Override
        protected Void doInBackground(Void... params) {
            mQuantities = new int[1];
            mNewEpisodes = 0;
            mDownloadCount = 0;
            if (mPodcastId > 0)
            {
                final PodcastItem podcast = GetPodcast(mContext.get(), mPodcastId);
                mQuantities = ProcessEpisodes(mContext.get(), podcast);
                mNewEpisodes = mNewEpisodes + mQuantities[0];
                mDownloadCount = mDownloadCount + mQuantities[1];
            }
            else
            {
                final List<PodcastItem> podcasts = GetPodcasts(mContext.get());

                for (final PodcastItem podcast : podcasts) {
                    mQuantities = ProcessEpisodes(mContext.get(), podcast);
                    mNewEpisodes = mNewEpisodes + mQuantities[0];
                    mDownloadCount = mDownloadCount + mQuantities[1];
                }
            }

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext.get());
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString("last_podcast_sync_date", new Date().toString());
            editor.apply();

            return null;
        }

        protected void onPostExecute(Void param) {
            if (mDisableToast == false)
                showToast(mContext.get(), mContext.get().getString(R.string.alert_sync_finished));

            mResponse.processFinish(mNewEpisodes, mDownloadCount);
        }
    }

    public static class SyncWithMobileDevice extends AsyncTask<Void, Void, Void> {

        private PodcastItem mEpisode;
        private MediaPlayer mMediaPlayer;
        private Boolean mFinished;

        public SyncWithMobileDevice(final Context context, final PodcastItem episode, final MediaPlayer mp, final Boolean finished) {
            mContext = new WeakReference<>(context);
            mEpisode= episode;
            mMediaPlayer = mp;
            mFinished = finished;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final PutDataMapRequest dataMap = PutDataMapRequest.create("/syncdevice");

            if (mMediaPlayer == null || mEpisode == null) return null;

            final PodcastItem podcast = GetPodcast(mContext.get(), mEpisode.getPodcastId());

            if (podcast == null) return null;

            if (podcast.getChannel() != null)
                dataMap.getDataMap().putString("podcast_title", podcast.getChannel().getTitle());

            if (mEpisode.getMediaUrl() != null)
                dataMap.getDataMap().putString("episode_url", mEpisode.getMediaUrl().toString());

            dataMap.getDataMap().putString("episode_title", mEpisode.getTitle());

            dataMap.getDataMap().putInt("position", getCurrentPosition(mMediaPlayer));
            dataMap.getDataMap().putInt("duration", mMediaPlayer.getDuration());
            dataMap.getDataMap().putInt("id", mFinished ? 0 : mEpisode.getEpisodeId());
            dataMap.getDataMap().putLong("time", new Date().getTime());

            CommonUtils.DeviceSync(mContext.get(), dataMap);
            return null;
        }

    }

    public static class SyncArt extends AsyncTask<Void, Void, Void>
    {
        private Interfaces.AsyncResponse mResponse;

        public SyncArt(final Context context, final Interfaces.AsyncResponse response)
        {
            mContext = new WeakReference<>(context);
            mResponse = response;
        }

        @Override
        protected void onPreExecute() {
            //Utilities.showToast(mContext.get(), mContext.get().getString(R.string.alert_sync_started));
        }

        @Override
        protected Void doInBackground(Void... params) {

            final File dirThumbs = new File(GetThumbnailDirectory(mContext.get()));

            final List<PodcastItem> podcasts = GetPodcasts(mContext.get());

            if (dirThumbs.exists() == false)
               dirThumbs.mkdirs();
            else {
                for (final PodcastItem podcast : podcasts)
                {
                    if (podcast.getChannel() != null && podcast.getChannel().getThumbnailName() != null) {
                        final File thumb = new File(dirThumbs, podcast.getChannel().getThumbnailName());

                        if (thumb.exists())
                            thumb.delete();
                    }
                }
            }

            for (final PodcastItem podcast : podcasts) {
                if (podcast.getChannel().getThumbnailUrl() != null)
                    CommonUtils.SavePodcastLogo(mContext.get(), podcast.getChannel().getThumbnailUrl().toString(), GetThumbnailDirectory(mContext.get()), podcast.getChannel().getThumbnailName(), mContext.get().getResources().getInteger(R.integer.podcast_art_download_width));
            }

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext.get());
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString("last_thumbnail_sync_date", new Date().toString());
            editor.apply();

            return null;
        }

        protected void onPostExecute(Void param)
        {
            //Utilities.showToast(mContext.get(), mContext.get().getString(R.string.alert_sync_finished));
            mResponse.processFinish();
        }
    }

    public static class GetPodcastEpisodes extends AsyncTask<Void, Void, Void> {

        int mPodcastId;
        private Interfaces.IntResponse mResponse;
        private int mCount;

        public GetPodcastEpisodes(final Context context, final int podcastId, final Interfaces.IntResponse response)
        {
            mContext = new WeakReference<>(context);
            mPodcastId = podcastId;
            mResponse = response;
        }

        @Override
        protected Void doInBackground(Void... params) {

            final PodcastItem podcast = GetPodcast(mContext.get(), mPodcastId);
            final int[] response = ProcessEpisodes(mContext.get(), podcast);

            mCount = response[0];

            return null;
        }

        protected void onPostExecute(Void param)
        {
            mResponse.processFinish(mCount);
        }
    }
}
