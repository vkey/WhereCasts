package com.krisdb.wearcasts.Utilities;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.FeedParser;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.Date;
import java.util.List;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodes;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetLatestEpisode;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.SaveEpisodeValue;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.TrimEpisodes;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.episodeExists;
import static com.krisdb.wearcasts.Utilities.Utilities.startDownload;

public class Processor {

    private Context mContext;
    public int newEpisodesCount, downloadCount;
    public List<PodcastItem> downloadEpisodes;

    public Processor(final Context ctx)
    {
        mContext = ctx;
    }

    public void processEpisodes(final PodcastItem podcast)
    {
        final int limit = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(mContext).getString("pref_episode_limit", "50"));

        final List<PodcastItem> newEpisodes = FeedParser.parse(podcast, limit);

        if (newEpisodes == null || newEpisodes.size() == 0) return;

        final PodcastItem latestPodcast = GetLatestEpisode(mContext, newEpisodes.get(0).getPodcastId());

        final Date latestEpisodeDate = latestPodcast != null && latestPodcast.getPubDate() != null ? DateUtils.ConvertDate(latestPodcast.getPubDate(), "yyyy-MM-dd HH:mm:ss") : null;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final boolean autoDownload = prefs.getBoolean("pref_" + podcast.getPodcastId() + "_auto_download", false);
        final int autoAssignDefaultPlaylistId = mContext.getResources().getInteger(R.integer.default_playlist_select);
        final int autoAssignPlaylistId = Integer.valueOf(prefs.getString("pref_" + podcast.getPodcastId() + "_auto_assign_playlist", String.valueOf(autoAssignDefaultPlaylistId)));
        final boolean assignPlaylist = autoAssignPlaylistId != autoAssignDefaultPlaylistId;
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mContext);

        for (final PodcastItem newEpisode : newEpisodes)
        {
            try {
                if (newEpisode == null || newEpisode.getPubDate() == null) continue;

                final Date episodeDate = DateUtils.ConvertDate(newEpisode.getPubDate(), "yyyy-MM-dd HH:mm:ss");

                if (latestEpisodeDate != null && (latestEpisodeDate.equals(episodeDate) || latestEpisodeDate.after(episodeDate)) || newEpisode.getMediaUrl() == null || episodeExists(mContext, newEpisode.getMediaUrl().toString()))
                    continue;

                CommonUtils.writeToFile(mContext, newEpisode.getTitle());

                if (newEpisode.getMediaUrl() != null)
                    CommonUtils.writeToFile(mContext, newEpisode.getMediaUrl().toString());

                if (newEpisode.getEpisodeUrl() != null)
                    CommonUtils.writeToFile(mContext, newEpisode.getEpisodeUrl().toString());

                CommonUtils.writeToFile(mContext, "Exists: " + episodeExists(mContext, newEpisode.getMediaUrl().toString()));

                final ContentValues cv = new ContentValues();
                cv.put("pid", newEpisode.getPodcastId());
                cv.put("title", newEpisode.getTitle() != null ? newEpisode.getTitle() : "");

                if (newEpisode.getDescription() != null)
                    cv.put("description", newEpisode.getDescription().trim());

                if (newEpisode.getMediaUrl() != null)
                    cv.put("mediaurl", newEpisode.getMediaUrl().toString());

                if (newEpisode.getEpisodeUrl() != null)
                    cv.put("url", newEpisode.getEpisodeUrl().toString());
                cv.put("dateAdded", DateUtils.GetDate());
                cv.put("pubDate", newEpisode.getPubDate());
                cv.put("duration", newEpisode.getDuration());

                final long episodeId = db.insert(cv);
                newEpisode.setEpisodeId((int) episodeId);
                CommonUtils.writeToFile(mContext, "EpisodeID: " + episodeId);

                if (assignPlaylist)
                    db.addEpisodeToPlaylist(autoAssignPlaylistId, (int) episodeId);

                if (autoDownload) {
                    downloadEpisodes.add(newEpisode);
                    SaveEpisodeValue(mContext, newEpisode, "download", 1); //so auto-download before doesn't re-download this episode
                    downloadCount++;
                }


                CommonUtils.writeToFile(mContext, "\n\n");

                newEpisodesCount++;
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                CommonUtils.writeToFile(mContext, ex.getMessage());

            }
        }
        db.close();

        TrimEpisodes(mContext, podcast);

        final int episodesDownloadedCount = Integer.valueOf(prefs.getString("pref_" + podcast.getPodcastId() + "_downloaded_episodes_count", "0"));

        if (episodesDownloadedCount > 0)
        {
            List<PodcastItem> episodes2 = GetEpisodes(mContext, podcast.getPodcastId());

            if (episodes2.size() > 0) {
                episodes2 = episodes2.subList(1, episodes2.size());
                int count = 1;
                for (final PodcastItem episode : episodes2) {
                    if (episode.getIsDownloaded() == false)
                        startDownload(mContext, episode);

                    if (count++ == episodesDownloadedCount) break;
                }
            }
        }

        if (downloadCount > 0 && prefs.getBoolean("cleanup_downloads", false) == false)
        {
            //need to detect in download receiver that the download is from a background update and not manual download
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("cleanup_downloads", true);
            editor.apply();
        }
    }
}
