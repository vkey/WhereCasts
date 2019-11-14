package com.krisdb.wearcastslibrary.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.krisdb.wearcastslibrary.Async.GetDirectory;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastCategory;

import java.util.List;

public class DirectoryViewModel extends AndroidViewModel {
    private MutableLiveData<List<PodcastCategory>> categories;
    private Application application;

    public DirectoryViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
    }

    public LiveData<List<PodcastCategory>> getDirectory() {
        return getDirectory(false, false);
    }

    public LiveData<List<PodcastCategory>> getDirectory(boolean forceRefresh, boolean saveThumbs) {
        if (categories == null) {
            categories = new MutableLiveData<>();
            CommonUtils.executeAsync(new GetDirectory(application, forceRefresh, saveThumbs), (categories) -> {
                this.categories.setValue(categories);
            });
        }

        return categories;
    }
}
