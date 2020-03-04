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

    public EpisodesViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
    }

    public LiveData<List<PodcastItem>> updateEpisodes(final int podcastId, final String query) {
        if (episodes == null) {
            episodes = new MutableLiveData<>();

            CommonUtils.executeCachedAsync(new DisplayEpisodes(application, podcastId, query), (episodes) -> {
                this.episodes.setValue(episodes);
            });
        }

        return episodes;
    }

    public void updateEpisodes(final List<PodcastItem> newepisodes)
    {
        episodes.postValue(newepisodes);
    }
}
