package com.krisdb.wearcasts;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.ProgressBar;

import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastCategory;
import com.krisdb.wearcastslibrary.PodcastItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class AsyncTasks {
    private static WeakReference<Context> mContext;

    public static class GetRadioList extends AsyncTask<Void, Void, Void> {

        private Interfaces.DirectoryResponse mResponse;
        private static WeakReference<ProgressBar> mProgressBar;
        private boolean mForceRefresh;
        private List<PodcastCategory> mCategories;

        public GetRadioList(final Context context, final Boolean refresh, final ProgressBar progressBar, final Interfaces.DirectoryResponse response)
        {
            mContext = new WeakReference<>(context);
            mResponse = response;
            mForceRefresh = refresh;
            mProgressBar = new WeakReference<>(progressBar);
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

                int syncs = prefs.getInt("directory_radio_syncs", 0);

                if (mForceRefresh || syncs == ctx.getResources().getInteger(com.krisdb.wearcastslibrary.R.integer.directory_radio_resync_amount))
                {
                    json = prefs.getString("directory_radio_json","");
                    String newJson = CommonUtils.getRemoteContent(ctx.getString(com.krisdb.wearcastslibrary.R.string.radio_json_url));

                    if (mForceRefresh || !newJson.equals(json))
                    {
                        json = newJson;
                        editor.putString("directory_radio_json", json);
                    }
                    syncs = 0;
                }
                else if (prefs.getString("directory_radio_json","").length() > 0)
                    json = prefs.getString("directory_radio_json","");
                else {
                    json = CommonUtils.getRemoteContent(ctx.getString(com.krisdb.wearcastslibrary.R.string.radio_json_url));

                    editor.putString("directory_radio_json", json);
                }

                editor.putInt("directory_radio_syncs", syncs + 1);
                editor.apply();

            } catch (Exception e) {
                e.printStackTrace();
            }

            try {

                final JSONArray podcastsArray = new JSONObject(json).getJSONArray("radio");

                final int podcastsLength = podcastsArray.length();

                if (mProgressBar != null)
                    mProgressBar.get().setMax(podcastsLength);

                for (int p = 0; p < podcastsLength; p++) {

                    if (mProgressBar != null)
                        mProgressBar.get().setProgress(p);

                    final JSONObject categoryObj = podcastsArray.getJSONObject(p);

                    final String name = categoryObj.names().get(0).toString();

                    final PodcastCategory category = new PodcastCategory();
                    category.setName(name);
                    final List<PodcastItem> stations = new ArrayList<>();

                    final JSONArray categoryArray = categoryObj.getJSONArray(name);

                    final int categoryLength = categoryArray.length();
                    for (int c = 0; c < categoryLength; c++) {

                        final JSONObject podcastObj = categoryArray.getJSONObject(c);

                        final PodcastItem station = new PodcastItem();
                        station.setTitle(podcastObj.getString("name"));
                        station.setDescription(podcastObj.getString("description"));
                        station.setMediaUrl(podcastObj.getString("mediaurl"));

                        if (podcastObj.getString("thumbnail") != null) {

                        }
                        stations.add(station);
                    }

                    category.setPodcasts(stations);
                    mCategories.add(category);
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mResponse.processFinish(mCategories);
        }
    }


    public static class SendToWatch extends AsyncTask<Void, Void, Void> {

         private List<PodcastItem> mPodcasts;
         private Interfaces.PodcastsResponse mResponse;
         private static WeakReference<ProgressBar> mProgressBar;

        public SendToWatch(final Context context, final List<PodcastItem> podcasts, final ProgressBar progressBar, final Interfaces.PodcastsResponse response) {
             mContext = new WeakReference<>(context);
             mProgressBar = new WeakReference<>(progressBar);
             mPodcasts = podcasts;
             mResponse = response;
         }

        @Override
        protected final Void doInBackground(Void... params) {

            final ProgressBar pb = mProgressBar.get();

            pb.setMax(mPodcasts.size());

            int count = 0;

            for (final PodcastItem podcast : mPodcasts) {
                pb.setProgress(count++);
                Utilities.SendToWatch(mContext.get(), podcast, false);
                SystemClock.sleep(3500);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            mResponse.processFinish(mPodcasts);
        }
    }
}
