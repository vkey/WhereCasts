package com.krisdb.wearcasts.ViewModels;

import android.app.Application;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.krisdb.wearcasts.Adapters.PodcastsAdapter;
import com.krisdb.wearcasts.Async.DisplayPodcasts;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

public class PodcastsViewModel extends AndroidViewModel {
    private MutableLiveData<List<PodcastItem>> podcasts;
    private Application application;

    public PodcastsViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
    }

    public LiveData<List<PodcastItem>> getPodcasts() {
        if (podcasts == null) {
            podcasts = new MutableLiveData<>();
            loadPodcasts();
        }
        return podcasts;
    }

    private void loadPodcasts() {
        final Boolean hideEmpty = PreferenceManager.getDefaultSharedPreferences(application).getBoolean("pref_hide_empty", false);

        CommonUtils.executeAsync(new DisplayPodcasts(application, hideEmpty), (podcasts) -> {
            this.podcasts.setValue(podcasts);
        });
    }
}
