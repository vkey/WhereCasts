package com.krisdb.wearcastslibrary.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.krisdb.wearcastslibrary.Async.SearchPodcasts;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

public class SearchPodcastsViewModel extends AndroidViewModel {
    private MutableLiveData<List<PodcastItem>> results;
    private Application application;
    private String mQuery;

    public SearchPodcastsViewModel(@NonNull Application application, String query) {
        super(application);
        this.application = application;
        this.mQuery = query;
    }

    public LiveData<List<PodcastItem>> getResults() {
        if (results == null) {
            results = new MutableLiveData<>();
            loadSearchResults();
        }
        return results;
    }

    private void loadSearchResults() {
        CommonUtils.executeAsync(new SearchPodcasts(application, mQuery), (results) -> {
            this.results.setValue(results);
        });
    }
}
