package com.krisdb.wearcastslibrary.Async;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.krisdb.wearcastslibrary.ChannelItem;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastCategory;
import com.krisdb.wearcastslibrary.PodcastItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import static com.krisdb.wearcastslibrary.CommonUtils.GetThumbnailDirectory;

public class GetDirectory implements Callable<List<PodcastCategory>> {
    private final Context context;
    private Boolean mForceRefresh = false, mSaveThumbs = false;

    public GetDirectory(final Context context)
    {
        this.context = context;
    }

    public GetDirectory(final Context context, final Boolean forceRefresh, final Boolean saveThumbs) {
        this.context = context;
        this.mForceRefresh = forceRefresh;
        this.mSaveThumbs = saveThumbs;
    }

    @Override
    public List<PodcastCategory> call() {

        String json = null;
        List<PodcastCategory> categories = new ArrayList<>();

        try {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final SharedPreferences.Editor editor = prefs.edit();

            int syncs = prefs.getInt("directory_syncs", 0);

            if (mForceRefresh || syncs == context.getResources().getInteger(com.krisdb.wearcastslibrary.R.integer.directory_resync_amount))
            {
                json = prefs.getString("directory_json","");
                String newJson = CommonUtils.getRemoteContent(context.getString(com.krisdb.wearcastslibrary.R.string.podcast_json_url, Locale.getDefault().getLanguage()));

                if(newJson == null)
                    newJson = CommonUtils.getRemoteContent(context.getString(com.krisdb.wearcastslibrary.R.string.podcast_json_url_default));

                if (mForceRefresh || !newJson.equals(json))
                {
                    json = newJson;
                    editor.putString("directory_json", json);
                }
                syncs = 0;
            }
            else if (prefs.getString("directory_json","").length() > 0)
                json = prefs.getString("directory_json","");
            else {
                json = CommonUtils.getRemoteContent(context.getString(com.krisdb.wearcastslibrary.R.string.podcast_json_url, Locale.getDefault().getLanguage()));
                //json = CommonUtils.getRemoteContent("https://s3.amazonaws.com/aws-website-wearcasts-evoy3/podcasts_test.json");

                if (json == null)
                    json = CommonUtils.getRemoteContent(context.getString(com.krisdb.wearcastslibrary.R.string.podcast_json_url_default));

                editor.putString("directory_json", json);
            }

            editor.putInt("directory_syncs", syncs + 1);
            editor.apply();

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            final JSONArray podcastsArray = new JSONObject(json).getJSONArray("podcasts");

            final int podcastsLength = podcastsArray.length();

            for (int p = 0; p < podcastsLength; p++) {

                final JSONObject categoryObj = podcastsArray.getJSONObject(p);

                final String name = categoryObj.names().get(0).toString();

                final PodcastCategory category = new PodcastCategory();
                category.setName(name);
                final List<PodcastItem> podcasts = new ArrayList<>();

                final PodcastItem categoryItem = new PodcastItem();
                final ChannelItem channelCategory = new ChannelItem();
                channelCategory.setTitle(name);
                categoryItem.setIsTitle(true);
                categoryItem.setChannel(channelCategory);
                podcasts.add(categoryItem);

                final JSONArray categoryArray = categoryObj.getJSONArray(name);

                final int categoryLength = categoryArray.length();
                for (int c = 0; c < categoryLength; c++) {

                    final JSONObject podcastObj = categoryArray.getJSONObject(c);

                    final ChannelItem channel = new ChannelItem();
                    channel.setTitle(podcastObj.getString("name"));
                    channel.setDescription(podcastObj.getString("description"));
                    channel.setRSSUrl(podcastObj.getString("rssurl"));
                    channel.setSiteUrl(podcastObj.getString("siteurl"));
                    if (podcastObj.getString("thumbnail") != null) {
                        channel.setThumbnailUrl(podcastObj.getString("thumbnail"));
                        channel.setThumbnailName(CommonUtils.GetThumbnailName(channel));
                    }

                    if (mSaveThumbs && channel.getThumbnailUrl() != null)
                        CommonUtils.SavePodcastLogo(context, channel.getThumbnailUrl().toString(), GetThumbnailDirectory(context), channel.getThumbnailName(), context.getResources().getInteger(com.krisdb.wearcastslibrary.R.integer.podcast_art_download_width), mForceRefresh);

                    final PodcastItem podcast = new PodcastItem();
                    podcast.setTitle(podcastObj.getString("name"));
                    podcast.setIsTitle(false);
                    podcast.setDescription(podcastObj.getString("description"));
                    podcast.setChannel(channel);
                    podcasts.add(podcast);
                }

                category.setPodcasts(podcasts);
                categories.add(category);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return categories;
    }
}
