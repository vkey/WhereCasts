package com.krisdb.wearcastslibrary.Async;

import android.content.Context;

import com.krisdb.wearcastslibrary.ChannelItem;
import com.krisdb.wearcastslibrary.ChannelParser;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.PodcastItem;
import com.krisdb.wearcastslibrary.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import javax.net.ssl.HttpsURLConnection;

public class SearchPodcasts implements Callable<List<PodcastItem>> {
    private final Context context;
    private String mQuery;

    public SearchPodcasts(final Context context, final String query)
    {
        this.context = context;
        this.mQuery = query;
    }

    @Override
    public List<PodcastItem> call() {
        List<PodcastItem> podcasts = new ArrayList<>();

        try {

            String searchLanguage;

            switch (Locale.getDefault().getLanguage())
            {
                case "fr":
                    searchLanguage = "French";
                    break;
                case "es":
                    searchLanguage = "Spanish";
                    break;
                case "de":
                    searchLanguage = "German";
                    break;
                case "ja":
                    searchLanguage = "Japanese";
                    break;
                case "hi":
                    searchLanguage = "Hindi";
                    break;
                case "sv":
                    searchLanguage = "Swedish";
                    break;
                case "zh":
                    searchLanguage = "Chinese";
                    break;
                default:
                    searchLanguage = "English";
                    break;
            }

            final URL url = new URL(context.getString(R.string.listennotes_rest, searchLanguage, URLEncoder.encode(mQuery, "UTF-8")));
            final HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("X-ListenAPI-Key", context.getString(R.string.listennotes_api_key));
            //conn.setRequestProperty("X-Mashape-Key", mContext.get().getString(R.string.listennotes_api_key));
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() == 200) {
                final BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                final StringBuilder json = new StringBuilder();
                String line;

                while ((line = r.readLine()) != null)
                    json.append(line);

                r.close();

                final JSONArray podcastsArray = new JSONObject(json.toString()).getJSONArray("results");
                final int podcastsLength = podcastsArray.length();

                final PodcastItem titleItem = new PodcastItem();
                final ChannelItem channelTitle = new ChannelItem();
                channelTitle.setTitle(mQuery);
                titleItem.setIsTitle(true);
                titleItem.setChannel(channelTitle);
                podcasts.add(titleItem);

                for (int p = 0; p < podcastsLength; p++) {

                    final JSONObject podcastObj = podcastsArray.getJSONObject(p);

                    final String latestPubDate = podcastObj.getString("lastest_pub_date_ms") != null ? podcastObj.getString("lastest_pub_date_ms").toLowerCase() : "";

                    final Calendar calendarEpisode = Calendar.getInstance(Locale.ENGLISH);
                    calendarEpisode.setTimeInMillis(Long.valueOf(latestPubDate));

                    final Calendar calendarNow = Calendar.getInstance(Locale.ENGLISH);
                    calendarNow.setTime(new Date());

                    if (DateUtils.daysBetween(calendarEpisode, calendarNow) < 365)
                    {
                        final PodcastItem podcast = new PodcastItem();
                        final ChannelItem channel = new ChannelItem();

                        String rssUrl = CommonUtils.getRedirectUrl(podcastObj.getString("rss"));

                        if (rssUrl == null)
                            rssUrl = podcastObj.getString("rss");

                        final ChannelItem channelItem = ChannelParser.parse(rssUrl);

                        if (channelItem.getTitle() == null)
                            channel.setTitle(podcastObj.getString("title"));
                        else
                            channel.setTitle(channelItem.getTitle());

                        if (channelItem.getThumbnailUrl() != null)
                            channel.setThumbnailUrl(channelItem.getThumbnailUrl().toString());
                        else
                            channel.setThumbnailUrl(podcastObj.getString("image"));

                        if (channelItem.getThumbnailUrl() != null)
                            channel.setThumbnailName(CommonUtils.GetThumbnailName(channelItem));

                        channel.setRSSUrl(rssUrl);

                        String description = channel.getDescription();

                        if (description == null)
                            description = podcastObj.getString("description_original");

                        if (description == null) continue;

                        description = description.replace("\n", "");

                        if (description.length() > 130)
                            description = description.substring(0, 130).concat("...");

                        channel.setDescription(description);

                        podcast.setChannel(channel);
                        podcast.setIsREST(true);

                        if (channel.getTitle().equals("N/A") == false)
                            podcasts.add(podcast);
                    }
                }
            }
            conn.disconnect();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return podcasts;
    }
}