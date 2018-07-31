package com.krisdb.wearcastslibrary;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

public class FetchPodcast extends AsyncTask<Void, Void, Void> {
    private String mTitle, mUrl;
    public Interfaces.FetchPodcastResponse mResponse;
    private PodcastItem mPodcastReturn;
    private List<PodcastItem> mPodcasts;
    private List<PodcastItem> mPodcastsReturn;

    public FetchPodcast(String title, String url, Interfaces.FetchPodcastResponse response) {
        mTitle = title;
        mUrl = url;
        mResponse = response;
    }

    public FetchPodcast(List<PodcastItem> podcasts, Interfaces.FetchPodcastResponse response)
    {
        mPodcasts = podcasts;
        mResponse = response;
    }

    @Override
    protected void onPreExecute() {
        //Toast.makeText(mContext, "Syncing " + mUrl, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected Void doInBackground(Void... params) {

        if (mPodcasts != null && mPodcasts.size() > 0)
        {
            mPodcastsReturn = new ArrayList<>();
            for (PodcastItem podcast : mPodcasts) {

                if(podcast.getChannel().getRSSUrl() != null) {
                    podcast.setChannel(ChannelParser.parse(podcast.getChannel().getTitle(), podcast.getChannel().getDescription(), podcast.getChannel().getRSSUrl().toString()));
                    mPodcastsReturn.add(podcast);
                }
            }
        }
        else {
            mPodcastReturn = new PodcastItem();
            mPodcastReturn.setChannel(ChannelParser.parse(mTitle, null, mUrl));
        }

        return null;
    }

    protected void onPostExecute(Void param) {
        if (mPodcastsReturn != null && mPodcastsReturn.size() > 0)
            mResponse.processFinish(mPodcastsReturn);
        else
            mResponse.processFinish(mPodcastReturn);
    }
}