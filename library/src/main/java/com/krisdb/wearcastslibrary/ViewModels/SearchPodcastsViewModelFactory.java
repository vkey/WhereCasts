package com.krisdb.wearcastslibrary.ViewModels;

import android.app.Application;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class SearchPodcastsViewModelFactory implements ViewModelProvider.Factory {
    private Application mApplication;
    private String mQuery;

    public SearchPodcastsViewModelFactory(Application application, String query) {
        this.mApplication = application;
        this.mQuery = query;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        return (T) new SearchPodcastsViewModel(mApplication, mQuery);
    }
}
