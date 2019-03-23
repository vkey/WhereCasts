package com.krisdb.wearcastslibrary;

import android.os.Bundle;

import com.google.android.gms.wearable.DataClient;

import java.util.List;

public class Interfaces {

    public interface FetchPodcastResponse {
        void processFinish(PodcastItem podcast);
        void processFinish(List<PodcastItem> podcasts);
    }

    public interface AsyncResponse {
        void processFinish();
    }

    public interface PremiumResponse {
        void processFinish(Boolean purchased, int playlistCount);
    }

    public interface PodcastsResponse {
        void processFinish(List<PodcastItem> episodes);
    }

    public interface DirectoryResponse {
        void processFinish(List<PodcastCategory> categories);
    }

    public interface IntResponse {
        void processFinish(int response);
    }

    public interface BackgroundSyncResponse {
        void processFinish(int newEpisodes, int downloadedEpisodes, List<PodcastItem> downloads);
    }

    public interface BooleanResponse {
        void processFinish(Boolean success);
    }

    public interface AssetResponse {
        void processFinish(DataClient.GetFdForAssetResponse response);
    }

    public interface BundleResponse {
        void processFinish(Bundle bundle);
    }

}
