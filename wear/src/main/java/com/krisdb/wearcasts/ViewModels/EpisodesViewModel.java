package com.krisdb.wearcasts.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.krisdb.wearcasts.Async.DisplayEpisodes;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

public class EpisodesViewModel extends AndroidViewModel {
    private MutableLiveData<List<PodcastItem>> episodes;
    private Application application;
    private int mPodcastID;
    private String mQuery;

    public EpisodesViewModel(@NonNull Application application, int podcastId, String query) {
        super(application);
        this.application = application;
        this.mPodcastID = podcastId;
        this.mQuery = query;
    }

    public LiveData<List<PodcastItem>> getEpisodes() {
        if (episodes == null) {
            episodes = new MutableLiveData<>();

            CommonUtils.executeAsync(new DisplayEpisodes(application, mPodcastID, mQuery), (episodes) -> {
                this.episodes.setValue(episodes);
            });
        }
        return episodes;
    }
}
