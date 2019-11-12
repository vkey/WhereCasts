package com.krisdb.wearcastslibrary.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.krisdb.wearcastslibrary.Async.GetDirectory;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastCategory;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

public class DirectoryViewModel extends AndroidViewModel {
    private MutableLiveData<List<PodcastCategory>> categories;
    private Application application;
    private boolean mForceRefresh, mSaveThumbs;

    public DirectoryViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        this.mForceRefresh = false;
        this.mSaveThumbs = false;
    }

    public DirectoryViewModel(@NonNull Application application, boolean forceRefresh, boolean saveThumbs) {
        super(application);
        this.application = application;
        this.mForceRefresh = forceRefresh;
        this.mSaveThumbs = saveThumbs;
    }

    public LiveData<List<PodcastCategory>> getDirectory() {
        if (categories == null) {
            categories = new MutableLiveData<>();
            loadDirectory();
        }
        return categories;
    }

    private void loadDirectory() {
        CommonUtils.executeAsync(new GetDirectory(application, mForceRefresh, mSaveThumbs), (categories) -> {
            this.categories.setValue(categories);
        });
    }
}
