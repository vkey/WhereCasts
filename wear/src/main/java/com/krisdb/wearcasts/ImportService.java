package com.krisdb.wearcasts;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;

public class ImportService extends WearableListenerService implements DataClient.OnDataChangedListener {

    @Override
    public void onCreate()
    {
        super.onCreate();
        Wearable.getDataClient(this).addListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Wearable.getDataClient(this).removeListener(this);
    }

    @Override
    public void onDataChanged(final DataEventBuffer dataEvents) {
        DataMapItem dataMapItem;
        String path;
        int type;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        for (DataEvent event : dataEvents) {
            dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
            path = event.getDataItem().getUri().getPath();
            type = event.getType();

             if (type == DataEvent.TYPE_CHANGED && path.equals("/premium")) {
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("premium", dataMapItem.getDataMap().getBoolean("unlock"));
                editor.apply();

                if (dataMapItem.getDataMap().getBoolean("confirm"))
                    CommonUtils.DeviceSync(this, PutDataMapRequest.create("/premiumconfirm"));
            }

            if (type == DataEvent.TYPE_CHANGED && path.equals("/episodeimport")) {

                PodcastItem episode = DBUtilities.GetEpisodeByTitle(this, dataMapItem.getDataMap().getString("title"));

                if (episode == null) {
                    episode = new PodcastItem();
                    episode.setPodcastId(getResources().getInteger(R.integer.episode_with_no_podcast_id));
                    episode.setTitle(dataMapItem.getDataMap().getString("title"));
                    episode.setDescription(dataMapItem.getDataMap().getString("description"));
                    episode.setPubDate(dataMapItem.getDataMap().getString("pubDate"));
                    episode.setMediaUrl(dataMapItem.getDataMap().getString("mediaurl"));
                    episode.setEpisodeUrl(dataMapItem.getDataMap().getString("url"));
                    episode.setDuration(dataMapItem.getDataMap().getInt("duration"));
                    final ContentValues cv = new ContentValues();
                    cv.put("pid", episode.getPodcastId());
                    cv.put("title", episode.getTitle());
                    cv.put("description", episode.getDescription());
                    if (episode.getMediaUrl() != null)
                        cv.put("mediaurl", episode.getMediaUrl().toString());
                    if (episode.getEpisodeUrl() != null)
                        cv.put("url", episode.getEpisodeUrl().toString());
                    cv.put("pubDate", episode.getPubDate());
                    cv.put("duration", episode.getDuration());
                    cv.put("dateAdded", DateUtils.GetDate());

                    final DBPodcastsEpisodes db = new DBPodcastsEpisodes(this);
                    final long episodeId = db.insert(cv);

                    episode.setEpisodeId((int)episodeId);

                    //only add third parties to playlist
                    if (dataMapItem.getDataMap().getInt("playlistid") < 0)
                        db.addEpisodeToPlaylist(dataMapItem.getDataMap().getInt("playlistid"), episode.getEpisodeId());

                    db.close();
                }

                //auto download for non-third party episodes
                if (dataMapItem.getDataMap().getInt("playlistid") == 0)
                    Utilities.startDownload(this, episode);
            }

            if (type == DataEvent.TYPE_CHANGED && path.equals("/uploadfile")) {

                final File dirLocal = new File(GetLocalDirectory());

                if (dirLocal.exists() == false)
                    dirLocal.mkdirs();

                final Asset asset = dataMapItem.getDataMap().getAsset("local_file");
                final String fileName = dataMapItem.getDataMap().getString("local_filename");

                new com.krisdb.wearcastslibrary.AsyncTasks.ConvertFileToAsset(this, asset,
                        new Interfaces.AssetResponse() {
                            @Override
                            public void processFinish(DataClient.GetFdForAssetResponse response) {
                                final InputStream inputStream = response.getInputStream();

                                try {
                                    int size = 1024;
                                    final File f = new File(GetLocalDirectory().concat(fileName));
                                    final OutputStream outputStream = new FileOutputStream(f);
                                    final byte buffer[] = new byte[size];

                                    int bytes, totalSize = inputStream.available();
                                    long total = 0;
                                    SendToDevice("started", 0, totalSize);

                                    while ((bytes = inputStream.read(buffer)) > 0) {
                                        //total += bytes;
                                        //int progress = (int)(total * 100 / totalSize);
                                        outputStream.write(buffer, 0, bytes);
                                        //SendToDevice("processing", 0, 0);
                                    }

                                    SendToDevice("finished", 0, 0);

                                    outputStream.close();
                                    inputStream.close();

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            if (type == DataEvent.TYPE_CHANGED && path.equals("/syncwear")) {
                final ContentValues cv = new ContentValues();
                cv.put("position", dataMapItem.getDataMap().getInt("position"));
                cv.put("finished", dataMapItem.getDataMap().getBoolean("finished") ? 1 : 0);

                new DBPodcastsEpisodes(getApplicationContext()).update(cv, dataMapItem.getDataMap().getInt("id"));
            }

            if (type == DataEvent.TYPE_CHANGED && path.equals("/addplaylists")) {
                int number = dataMapItem.getDataMap().getInt("number");
                DBPodcastsEpisodes db = new DBPodcastsEpisodes(this);

                final List<PodcastItem> podcasts = DBUtilities.GetPodcasts(this);

                final int autoAssignDefaultPlaylistId = getResources().getInteger(R.integer.default_playlist_select);
                final SharedPreferences.Editor editor = prefs.edit();

                for (final PodcastItem podcast : podcasts)
                        editor.putString("pref_" + podcast.getPodcastId() + "_auto_assign_playlist", String.valueOf(autoAssignDefaultPlaylistId));

                editor.apply();

                db.deleteAllPlaylists();

                for (int i = 1; i <= number; i++)
                    db.insertPlaylist(getString(R.string.playlists_title_default, i));
            }

            if (type == DataEvent.TYPE_CHANGED && path.equals("/podcastimport")) {
                final ContentValues cv = new ContentValues();
                cv.put("title", dataMapItem.getDataMap().getString("title"));
                cv.put("url", dataMapItem.getDataMap().getString("rss_url"));
                cv.put("site_url", dataMapItem.getDataMap().getString("site_url"));
                cv.put("dateAdded", DateUtils.GetDate());
                String thumbnailUrl = null;
                String fileName = null;
                if (dataMapItem.getDataMap().getString("thumbnail_url") != null) {
                    thumbnailUrl = dataMapItem.getDataMap().getString("thumbnail_url");
                    fileName = dataMapItem.getDataMap().getString("thumbnail_name");

                    new com.krisdb.wearcastslibrary.AsyncTasks.SaveLogo(this, thumbnailUrl, fileName,
                            new Interfaces.AsyncResponse() {
                                @Override
                                public void processFinish() {}
                            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                cv.put("thumbnail_url", thumbnailUrl);
                cv.put("thumbnail_name", fileName);

                final int podcastId = (int)new DBPodcasts(getApplicationContext()).insert(cv);

                new AsyncTasks.GetPodcastEpisodes(getApplicationContext(), podcastId).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                CacheUtils.deletePodcastsCache(this);
            }

            if (type == DataEvent.TYPE_CHANGED && path.equals("/opmlimport")) {
                //new DBPodcasts(getApplicationContext()).deleteAll();
                final Asset asset = dataMapItem.getDataMap().getAsset("opml");

                final Task<DataClient.GetFdForAssetResponse> getFdForAssetResponseTask = Wearable.getDataClient(getApplicationContext()).getFdForAsset(asset);

                DataClient.GetFdForAssetResponse getFdForAssetResponse = null;
                try {
                    getFdForAssetResponse = Tasks.await(getFdForAssetResponseTask);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }

                final InputStream in = getFdForAssetResponse.getInputStream();

                new DBPodcasts(getApplicationContext()).insert(OPMLParser.parse(this, in));
                CacheUtils.deletePodcastsCache(this);
            }
        }

        if (prefs.getInt("uploads", 0) == 0)
        {
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("uploads", 1);
            editor.apply();
        }
    }

    private void SendToDevice(final String status, final int progress, final int length) {

        final PutDataMapRequest dataMap = PutDataMapRequest.create("/fileuploadprogress");
        dataMap.getDataMap().putBoolean(status, true);
        dataMap.getDataMap().putInt("length", length);
        dataMap.getDataMap().putInt("progress", progress);

        CommonUtils.DeviceSync(this, dataMap);
    }
}
