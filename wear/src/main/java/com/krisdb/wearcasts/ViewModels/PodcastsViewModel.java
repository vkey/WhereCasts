package com.krisdb.wearcasts.ViewModels;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.krisdb.wearcasts.Async.GetPodcasts;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;
import java.util.concurrent.Executors;

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

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);

            final Boolean hideEmpty = prefs.getBoolean("pref_hide_empty", false);
            final Boolean showDownloaded = prefs.getBoolean("pref_display_show_downloaded", false);

            //CommonUtils.executeAsync(new GetPodcasts(application, hideEmpty, showDownloaded), (podcasts) ->
            //this.podcasts.setValue(podcasts));

            CommonUtils.executeCachedAsync(new GetPodcasts(application, hideEmpty, showDownloaded), (podcasts) ->
                    this.podcasts.setValue(podcasts));
        }

        return podcasts;
    }

    /*
    public MutableLiveData<List<PodcastItem>> getPodcasts() {
        if (podcasts == null) {
            podcasts = new MutableLiveData<List<PodcastItem>>() {
                @Override
                public void onActive() {
                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);

                    final Boolean hideEmpty = prefs.getBoolean("pref_hide_empty", false);
                    final Boolean showDownloaded = prefs.getBoolean("pref_display_show_downloaded", false);

                    CommonUtils.executeAsync(new GetPodcasts(application, hideEmpty, showDownloaded), (p) ->
                            {
                                podcasts.postValue(p);
                                application.getContentResolver().registerContentObserver(FilesProvider.getFilePath(application.getDatabasePath("episodes.db")), true, podcastContentObserver);
                                //application.getContentResolver().registerContentObserver(FilesProvider.getFilePath(new android.content.ContextWrapper(application).getFilesDir()), true, podcastContentObserver);
                            }
                    );
                }

                @Override
                public void onInactive() {
                    application.getContentResolver().unregisterContentObserver(podcastContentObserver);
                }
            };
        }

       return podcasts;
    }

    private final ContentObserver podcastContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            getPodcasts();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            getPodcasts();
        }
    };
    */

}
