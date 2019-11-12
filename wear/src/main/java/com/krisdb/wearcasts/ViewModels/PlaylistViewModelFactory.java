package com.krisdb.wearcasts.ViewModels;

import android.app.Application;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class PlaylistViewModelFactory implements ViewModelProvider.Factory {
    private Application mApplication;
    private int mPlaylistID;

    public PlaylistViewModelFactory(Application application, int playlistId) {
        this.mApplication = application;
        this.mPlaylistID = playlistId;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        return (T) new PlaylistViewModel(mApplication, mPlaylistID);
    }
}
