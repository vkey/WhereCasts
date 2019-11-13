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
    private int mPlaylistID;

    public PlaylistViewModel(@NonNull Application application, int playlistId) {
        super(application);
        this.application = application;
        this.mPlaylistID = playlistId;
    }

    public LiveData<List<PodcastItem>> getEpisodes() {
        if (episodes == null) {
            episodes = new MutableLiveData<>();
            loadEpisodes();
        }
        return episodes;
    }

    private void loadEpisodes() {
        CommonUtils.executeAsync(new DisplayPlaylistEpisodes(application, mPlaylistID), (episodes) -> {
            this.episodes.setValue(episodes);
        });
    }
}
