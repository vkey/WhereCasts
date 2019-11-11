package com.krisdb.wearcasts.Async;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.DBUtilities;
import com.krisdb.wearcasts.Utilities.OPMLParser;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodes;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.SearchEpisodes;

public class ProcessOPML implements Callable<Void> {
    private final Context context;
    private boolean mOpmlImport = false;
    private Asset mAsset;

    public ProcessOPML(final Context context, final Asset asset) {
        this.context = context;
        this.mAsset = asset;
    }

    public ProcessOPML(final Context context, final Asset asset, final boolean opmlImport) {
        this.context = context;
        this.mAsset = asset;
        this.mOpmlImport = opmlImport;
    }

    @Override
    public Void call() {

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(context);

        final Task<DataClient.GetFdForAssetResponse> getFdForAssetResponseTask = Wearable.getDataClient(context).getFdForAsset(mAsset);

        DataClient.GetFdForAssetResponse getFdForAssetResponse = null;
        try {
            getFdForAssetResponse = Tasks.await(getFdForAssetResponseTask);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        final InputStream in = getFdForAssetResponse.getInputStream();

        final List<PodcastItem> podcasts = OPMLParser.parse(context, in);

        for(final PodcastItem podcast : podcasts) {

            if (mOpmlImport) {
                final PutDataMapRequest dataMapRequest = PutDataMapRequest.create("/opmlimport_podcasts");
                final DataMap dataMap = dataMapRequest.getDataMap();
                dataMap.putString("podcast_title", podcast.getChannel().getTitle());
                CommonUtils.DeviceSync(context, dataMapRequest);
            }

            DBUtilities.insertPodcast(context, db, podcast, false, false);
        }
        db.close();

        return null;
    }
}