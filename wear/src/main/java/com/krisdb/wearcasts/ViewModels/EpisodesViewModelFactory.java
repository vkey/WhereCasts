package com.krisdb.wearcasts.ViewModels;

import android.app.Application;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class EpisodesViewModelFactory implements ViewModelProvider.Factory {
    private Application mApplication;
    private String mQuery;
    private int mPodcastID;

    public EpisodesViewModelFactory(Application application, int podcastId, String query) {
        this.mApplication = application;
        this.mPodcastID = podcastId;
        this.mQuery = query;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        return (T) new EpisodesViewModel(mApplication, mPodcastID, mQuery);
    }
}
