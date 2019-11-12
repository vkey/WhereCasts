package com.krisdb.wearcastslibrary.ViewModels;

import android.app.Application;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class DirectoryViewModelFactory implements ViewModelProvider.Factory {
    private Application mApplication;
    private boolean mForceRefresh, mSaveThumbs;

    public DirectoryViewModelFactory(Application application, boolean forceRefresh, boolean saveThumbs) {
        this.mApplication = application;
        this.mForceRefresh = forceRefresh;
        this.mSaveThumbs = saveThumbs;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        return (T) new DirectoryViewModel(mApplication, mForceRefresh, mSaveThumbs);
    }
}
