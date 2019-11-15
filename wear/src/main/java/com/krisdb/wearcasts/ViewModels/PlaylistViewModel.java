package com.krisdb.wearcasts.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.krisdb.wearcasts.Async.DisplayPlaylistEpisodes;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

public class PlaylistViewModel extends AndroidViewModel {
    private MutableLiveData<List<PodcastItem>> episodes;
    private Application application;

    public PlaylistViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
    }

    public LiveData<List<PodcastItem>> getEpisodes(final int playlistId) {
        if (episodes == null) {
            episodes = new MutableLiveData<>();

            CommonUtils.executeCachedAsync(new DisplayPlaylistEpisodes(application, playlistId), (episodes) -> {
                this.episodes.setValue(episodes);
            });
        }

        return episodes;
    }
}
