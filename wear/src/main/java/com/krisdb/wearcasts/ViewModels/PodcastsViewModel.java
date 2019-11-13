package com.krisdb.wearcasts.ViewModels;

import android.app.Application;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.krisdb.wearcasts.Async.DisplayEpisodes;
import com.krisdb.wearcasts.Async.GetPodcasts;
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

    public MutableLiveData<List<PodcastItem>> getPodcasts() {
        if (podcasts == null) {
            podcasts = new MutableLiveData<>();
            loadPodcasts();
        }
        return podcasts;
    }

    public void loadPodcasts() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);

        final Boolean hideEmpty = prefs.getBoolean("pref_hide_empty", false);

        CommonUtils.executeAsync(new GetPodcasts(application, hideEmpty), (podcasts) -> this.podcasts.setValue(podcasts));
    }


    private final ContentObserver contentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            loadPodcasts();
        }
    };
}
