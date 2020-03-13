package com.krisdb.wearcasts.Async;


import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.EpisodeUtilities;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylistItems;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;

public class FinishMedia implements Callable<List<PodcastItem>> {
    private final Context context;
    private PodcastItem mEpisode;
    private String mLocalFile;
    private int mPlaylistID;
    private boolean mPlaybackError;

    public FinishMedia(final Context context, final PodcastItem episode, final int playlistId, final String localFile, final boolean playbackError)
    {
        this.context = context;
        mEpisode = episode;
        mLocalFile = localFile;
        mPlaylistID = playlistId;
        mPlaybackError = playbackError;
    }

    @Override
    public List<PodcastItem> call() {
        //CommonUtils.writeToFile(context, "Episode: " + mEpisode.getTitle());
        //CommonUtils.writeToFile(context, "PlaylistID: " + mPlaylistID);
        //CommonUtils.writeToFile(context, "PodcastID: " + mPodcastID);

        List<PodcastItem> episodes;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (mPlaylistID == context.getResources().getInteger(R.integer.playlist_episodes))
            episodes = EpisodeUtilities.GetEpisodes(context, mEpisode.getPodcastId());
        else
            episodes = getPlaylistItems(context, mPlaylistID, mLocalFile == null);

        //CommonUtils.writeToFile(context, "Episodes size: " + episodes.size());

            /*
            if (prefs.getBoolean("pref_" + mEpisode.getPodcastId() + "_download_next", false)) {
                final PodcastItem nextEpisode = getNextEpisodeNotDownloaded(ctx, mEpisode);

                if (nextEpisode != null && Utilities.getDownloadId(ctx, nextEpisode.getEpisodeId()) == 0)
                    Utilities.startDownload(ctx, nextEpisode);
            }
            */

        if (!mPlaybackError) {
            if (mLocalFile == null) {
                final DBPodcastsEpisodes db = new DBPodcastsEpisodes(context);
                final ContentValues cv = new ContentValues();
                cv.put("finished", 1);
                cv.put("playing", 0);
                cv.put("buffering", 0);
                cv.put("position", 0);
                db.update(cv, mEpisode.getEpisodeId());

                if (Integer.valueOf(prefs.getString("pref_downloads_auto_delete", "1")) == context.getResources().getInteger(R.integer.autodelete_played))
                    Utilities.DeleteMediaFile(context, mEpisode);

                if (mPlaylistID != -1 && prefs.getBoolean("pref_remove_playlist_onend", false))
                    db.deleteEpisodeFromPlaylists(mEpisode.getEpisodeId());

                db.close();
            } else {
                if (Integer.valueOf(prefs.getString("pref_downloads_auto_delete", "1")) == context.getResources().getInteger(R.integer.autodelete_played)) {
                    final File localFile = new File(GetLocalDirectory(context).concat(mLocalFile));

                    if (localFile.exists())
                        localFile.delete();
                }

                final SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(Utilities.GetLocalPositionKey(mLocalFile), 0);
                editor.apply();
            }
        }

        return episodes;
    }
}