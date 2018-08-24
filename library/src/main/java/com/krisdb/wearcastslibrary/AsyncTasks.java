package com.krisdb.wearcastslibrary;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ProgressBar;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

import static com.krisdb.wearcastslibrary.CommonUtils.GetThumbnailDirectory;

public class AsyncTasks {

    private static WeakReference<Context> mContext;

    public static class CacheDirectory extends AsyncTask<Void, Void, Void> {

        public CacheDirectory(final Context context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... params) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext.get());
            final SharedPreferences.Editor editor = prefs.edit();

            String json = CommonUtils.getRemoteContent(mContext.get().getString(com.krisdb.wearcastslibrary.R.string.podcast_json_url, Locale.getDefault().getLanguage()));

            if(json == null)
                json = CommonUtils.getRemoteContent(mContext.get().getString(com.krisdb.wearcastslibrary.R.string.podcast_json_url_default));

            editor.putString("directory_json", json);
            editor.apply();

            return null;
        }

        protected void onPostExecute(Void param) { }
    }

    public static class WatchConnected extends AsyncTask<Void, Void, Void> {
        private Boolean mWatchConnected = false;
        private Interfaces.BooleanResponse mResponse;

        public WatchConnected(final Context context, final Interfaces.BooleanResponse response) {
            mContext = new WeakReference<>(context);
            mResponse = response;
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Task<List<Node>> nodeListTask = Wearable.getNodeClient(mContext.get()).getConnectedNodes();

            try {
                List<Node> nodes = Tasks.await(nodeListTask);
                mWatchConnected = nodes.size() > 0;
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(Void param) {
            mResponse.processFinish(mWatchConnected);
        }
    }

    public static class GetDirectory extends AsyncTask<Void, Void, Void> {

        private Interfaces.DirectoryResponse mResponse;
        private List<PodcastCategory> mCategories;
        private Boolean mForceRefresh = false, mSaveThumbs = false;
        private static WeakReference<ProgressBar> mProgressBar;

        public GetDirectory(final Context context, final Interfaces.DirectoryResponse response)
        {
            mContext = new WeakReference<>(context);
            mResponse = response;
            mCategories = new ArrayList<>();
        }

        public GetDirectory(final Context context, final Boolean forceRefresh, final Boolean saveThumbs, final ProgressBar progressBar, final Interfaces.DirectoryResponse response)
        {
            mContext = new WeakReference<>(context);
            mProgressBar = new WeakReference<>(progressBar);
            mResponse = response;
            mForceRefresh = forceRefresh;
            mSaveThumbs = saveThumbs;
            mCategories = new ArrayList<>();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Context ctx = mContext.get();
            String json = null;

            try {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
                final SharedPreferences.Editor editor = prefs.edit();

                int syncs = prefs.getInt("directory_syncs", 0);

                if (mForceRefresh || syncs == ctx.getResources().getInteger(R.integer.directory_resync_amount))
                {
                    json = prefs.getString("directory_json","");
                    String newJson = CommonUtils.getRemoteContent(ctx.getString(R.string.podcast_json_url, Locale.getDefault().getLanguage()));

                    if(newJson == null)
                        newJson = CommonUtils.getRemoteContent(ctx.getString(R.string.podcast_json_url_default));

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
                    json = CommonUtils.getRemoteContent(ctx.getString(R.string.podcast_json_url, Locale.getDefault().getLanguage()));
                    //json = CommonUtils.getRemoteContent("https://s3.amazonaws.com/aws-website-wearcasts-evoy3/podcasts_test.json");

                    if (json == null)
                        json = CommonUtils.getRemoteContent(ctx.getString(R.string.podcast_json_url_default));

                    editor.putString("directory_json", json);
                }

                editor.putInt("directory_syncs", syncs + 1);
                editor.apply();

            } catch (Exception e) {
                Log.e(ctx.getPackageName(), e.getLocalizedMessage());
            }

            try {

                final JSONArray podcastsArray = new JSONObject(json).getJSONArray("podcasts");

                final int podcastsLength = podcastsArray.length();

                if (mProgressBar != null) {
                    mProgressBar.get().setMax(podcastsLength);
                    mProgressBar.get().setIndeterminate(false);
                }

                for (int p = 0; p < podcastsLength; p++) {

                    if (mProgressBar != null)
                        mProgressBar.get().setProgress(p);

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
                            CommonUtils.SavePodcastLogo(ctx, channel.getThumbnailUrl().toString(), GetThumbnailDirectory(), channel.getThumbnailName(), ctx.getResources().getInteger(R.integer.podcast_art_download_width), mForceRefresh);

                        final PodcastItem podcast = new PodcastItem();
                        podcast.setTitle(podcastObj.getString("name"));
                        podcast.setIsTitle(false);
                        podcast.setDescription(podcastObj.getString("description"));
                        podcast.setChannel(channel);
                        podcasts.add(podcast);
                    }

                    category.setPodcasts(podcasts);
                    mCategories.add(category);
                }
            }
            catch (Exception ex)
            {
                Log.e(mContext.get().getPackageName(), ex.getLocalizedMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mResponse.processFinish(mCategories);
        }
    }

    public static class GetEpisodes extends AsyncTask<Void, Void, Void> {
        private Interfaces.PodcastsResponse mResponse;
        private PodcastItem mPodcast;
        private List<PodcastItem> mEpisodes;
        private int mCount;

        public GetEpisodes(final PodcastItem podcast, final int count, final Interfaces.PodcastsResponse response)
        {
            mPodcast = podcast;
            mCount = count;
            mResponse = response;
        }

        @Override
        protected Void doInBackground(Void... params) {
            mEpisodes = FeedParser.parse(mPodcast, mCount);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mResponse.processFinish(mEpisodes);
        }
    }

    public static class GetPodcastsDirectory extends AsyncTask<Void, Void, Void> {

        private Interfaces.PodcastsResponse mResponse;
        private List<PodcastItem> mPodcasts;
        private String mQuery;

        public GetPodcastsDirectory(final Context context, final String query, final Interfaces.PodcastsResponse response)
        {
            mContext = new WeakReference<>(context);
            mResponse = response;
            mQuery = query;
        }

        @Override
        protected Void doInBackground(Void... args) {
            try {
                mPodcasts = new ArrayList<>();

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

                final URL url = new URL(mContext.get().getString(R.string.listennotes_rest, searchLanguage, URLEncoder.encode(mQuery, "UTF-8")));
                final HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("X-Mashape-Key", mContext.get().getString(R.string.listennotes_api_key));
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

                        /*
                        final List<PodcastItem> episodes = FeedParser.parse(podcast);

                        if (episodes == null || episodes.size() == 0) continue;

                        final Date firstEpisode = DateUtils.ConvertDate(episodes.get(0).getPubDate(), "yyyy-MM-dd HH:mm:ss");

                        final Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.DATE, mContext.get().getResources().getInteger(R.integer.search_results_day_limit));

                        final Date lastYearDate = cal.getTime();

                        if (firstEpisode == null || firstEpisode.before(lastYearDate)) continue;
                        */

                            if (channel.getTitle().equals("N/A") == false)
                                mPodcasts.add(podcast);
                        }
                    }
                }
                conn.disconnect();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mResponse.processFinish(mPodcasts);
        }
    }

    public static class SaveLogo extends AsyncTask<Void, Void, Void> {

        private String mUrl, mFileName;
        private Interfaces.AsyncResponse mResponse;
        private Boolean mForce;

        public SaveLogo(final Context context, final String url, final String filename, final Interfaces.AsyncResponse response)
        {
            mContext = new WeakReference<>(context);
            mFileName = filename;
            mUrl = url;
            mResponse = response;
            mForce = false;
        }

        public SaveLogo(final Context context, final String url, final String filename, final Boolean force, final Interfaces.AsyncResponse response)
        {
            mContext = new WeakReference<>(context);
            mFileName = filename;
            mUrl = url;
            mResponse = response;
            mForce = force;
        }

        @Override
        protected Void doInBackground(Void... args) {
            CommonUtils.SavePodcastLogo(
                    mContext.get(),
                    mUrl,
                    GetThumbnailDirectory(),
                    mFileName,
                    mContext.get().getResources().getInteger(R.integer.podcast_art_download_width),
                    mForce
                    );
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mResponse.processFinish();
        }
    }

    public static class NodesConnected extends AsyncTask<Void, Void, Void> {

        private Interfaces.BooleanResponse mResponse;
        private Boolean isConnected = false;

        public NodesConnected(final Context context, final Interfaces.BooleanResponse response)
        {
            mContext = new WeakReference<>(context);
            mResponse = response;
        }

        @Override
        protected Void doInBackground(Void... args) {

            final Task<List<Node>> nodeListTask = Wearable.getNodeClient(mContext.get()).getConnectedNodes();

            try {
                List<Node> nodes = Tasks.await(nodeListTask);
                isConnected = nodes.size() > 0;
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mResponse.processFinish(isConnected);
        }
    }

    public static class EpisodeCount extends AsyncTask<Void, Void, Void> {

        private Interfaces.IntResponse mResponse;
        private PodcastItem mPodcast;
        private int mOutput;

        public EpisodeCount(final Context context, final PodcastItem podcast, final Interfaces.IntResponse response)
        {
            mContext = new WeakReference<>(context);
            mPodcast = podcast;
            mResponse = response;
        }

        @Override
        protected Void doInBackground(Void... args) {
            mOutput = FeedParser.parse(mPodcast, 1).size();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mResponse.processFinish(mOutput);
        }
    }

    public static class ConvertFileToAsset extends AsyncTask<Void, Void, Void> {

        private Interfaces.AssetResponse mResponse;
        private Asset mAsset;
        private DataClient.GetFdForAssetResponse mResponseAsset;

        public ConvertFileToAsset(final Context context, Asset asset, final Interfaces.AssetResponse response)
        {
            mContext = new WeakReference<>(context);
            mResponse = response;
            mAsset = asset;
        }

        @Override
        protected Void doInBackground(Void... args) {

            final Task<DataClient.GetFdForAssetResponse> getFdForAssetResponseTask = Wearable.getDataClient(mContext.get()).getFdForAsset(mAsset);
            try {
                mResponseAsset = Tasks.await(getFdForAssetResponseTask);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mResponse.processFinish(mResponseAsset);
        }
    }

    public static class HasUnlockedPremium extends AsyncTask<Void, Void, Void> {

        private IInAppBillingService mService;
        private Interfaces.PremiumResponse mResponse;
        private Boolean mPurchased = false;
        private int mPlaylistCount = 0;

        public HasUnlockedPremium(final Context context, final IInAppBillingService service, final Interfaces.PremiumResponse response)
        {
            mContext = new WeakReference<>(context);
            mService =  service;
            mResponse = response;
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected Void doInBackground(Void... params) {

            try {
                final Bundle ownedItems = mService.getPurchases(
                        mContext.get().getResources().getInteger(R.integer.billing_apk_version),
                        mContext.get().getPackageName(),
                        "inapp",
                        null
                );

                final ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");

                if (ownedSkus != null && ownedSkus.size() > 0)
                {
                    for(String sku: ownedSkus)
                    {
                        if (sku.equals(mContext.get().getString(R.string.inapp_premium_product_id)))
                            mPurchased = true;

                        if (sku.toLowerCase().contains(mContext.get().getString(R.string.inapp_playlist_prefix))) {
                            final String[] skuArray = sku.split("_");
                            mPlaylistCount = Integer.valueOf(skuArray[1]);
                            break;
                        }
                    }

                }
                else
                    mPurchased = false;

            } catch (RemoteException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mResponse.processFinish(mPurchased, mPlaylistCount);
        }
    }
}
