package com.krisdb.wearcasts.Services;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.krisdb.wearcasts.Async.InsertPodcasts;
import com.krisdb.wearcasts.Async.SaveLogo;
import com.krisdb.wearcasts.Async.SyncArt;
import com.krisdb.wearcasts.Async.SyncPodcasts;
import com.krisdb.wearcasts.Databases.DBPodcasts;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.DBUtilities;
import com.krisdb.wearcasts.Utilities.OPMLParser;
import com.krisdb.wearcasts.Utilities.PodcastUtilities;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.Async.ConvertFileToAsset;
import com.krisdb.wearcastslibrary.ChannelItem;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodeByTitle;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.getCurrentPosition;

public class ImportService extends WearableListenerService implements DataClient.OnDataChangedListener, CapabilityClient.OnCapabilityChangedListener {

    private static WeakReference<Context> mContext;
    private ConnectivityManager mManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private static TimeOutHandler mTimeOutHandler;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = new WeakReference<>(getApplicationContext());

        mTimeOutHandler = new TimeOutHandler(this);
        mManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        Wearable.getDataClient(mContext.get()).addListener(this);
        Wearable.getCapabilityClient(mContext.get()).addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Wearable.getDataClient(mContext.get()).removeListener(this);
        Wearable.getCapabilityClient(mContext.get()).removeListener(this);
    }

    @Override
    public void onDataChanged(final DataEventBuffer dataEvents) {
        DataMapItem dataMapItem;
        String path;
        int type;

        final Context context = mContext.get();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        for (DataEvent event : dataEvents) {
            dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
            path = event.getDataItem().getUri().getPath();
            type = event.getType();

              if (type == DataEvent.TYPE_CHANGED && path.equals("/premium")) {
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("premium", dataMapItem.getDataMap().getBoolean("unlock"));
                editor.apply();

                if (dataMapItem.getDataMap().getBoolean("confirm")) {
                    CommonUtils.DeviceSync(this, PutDataMapRequest.create("/premiumconfirm"));
                }
            }

            if (type == DataEvent.TYPE_CHANGED && path.equals("/episodeimport")) {

                final DBPodcastsEpisodes db = new DBPodcastsEpisodes(context);
                //final Boolean isRadio = dataMapItem.getDataMap().getBoolean("radio");

                PodcastItem episode = GetEpisodeByTitle(this, dataMapItem.getDataMap().getString("title"));

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
                    //cv.put("radio", isRadio);

                    final long episodeId = db.insert(cv);
                    episode.setEpisodeId((int) episodeId);
                }

                final int playlist = dataMapItem.getDataMap().getInt("playlistid");

                //add third parties to their playlist
                if (playlist < 0) {
                    CommonUtils.DeviceSync(this, PutDataMapRequest.create("/thirdparty"));
                    db.addEpisodeToPlaylist(playlist, episode.getEpisodeId());
                }

                db.close();

                if (dataMapItem.getDataMap().getInt("playlistid") == 0 || dataMapItem.getDataMap().getBoolean("auto_download")) {

                    if (prefs.getBoolean("pref_disable_bluetooth", false) && Utilities.BluetoothEnabled() && Utilities.disableBluetooth(context)) {
                        unregisterNetworkCallback();

                        final PodcastItem finalEpisode = episode;
                        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                            @Override
                            public void onAvailable(final Network network) {
                                mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);
                                Utilities.startDownload(context, finalEpisode);
                            }
                        };

                        final NetworkRequest request = new NetworkRequest.Builder()
                                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                .build();

                        mManager.requestNetwork(request, mNetworkCallback);

                        mTimeOutHandler.sendMessageDelayed(
                                mTimeOutHandler.obtainMessage(MESSAGE_CONNECTIVITY_TIMEOUT),
                                NETWORK_CONNECTIVITY_TIMEOUT_MS);
                    }
                    else
                    {
                        Utilities.startDownload(this, episode);
                    }
                }
                Utilities.vibrate(this);
            }

            if (type == DataEvent.TYPE_CHANGED && path.equals("/uploadfile")) {

                final File dirLocal = new File(GetLocalDirectory(mContext.get()));

                if (dirLocal.exists() == false)
                    dirLocal.mkdirs();

                final Asset asset = dataMapItem.getDataMap().getAsset("local_file");
                final String fileName = dataMapItem.getDataMap().getString("local_filename");

                CommonUtils.executeSingleThreadAsync(new ConvertFileToAsset(context, asset), (response) -> {
                    final InputStream inputStream = response.getInputStream();

                    try {
                        int size = 1024;
                        final File f = new File(GetLocalDirectory(mContext.get()).concat(fileName));
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
                });

            }

            if (type == DataEvent.TYPE_CHANGED && path.equals("/syncwear")) {
                final ContentValues cv = new ContentValues();
                cv.put("position", dataMapItem.getDataMap().getInt("position"));
                cv.put("finished", dataMapItem.getDataMap().getBoolean("finished") ? 1 : 0);

                new DBPodcastsEpisodes(getApplicationContext()).update(cv, dataMapItem.getDataMap().getInt("id"));
            }

            if (type == DataEvent.TYPE_CHANGED && path.equals("/addplaylists")) {
                int number = dataMapItem.getDataMap().getInt("number");
                DBPodcastsEpisodes db = new DBPodcastsEpisodes(context);

                final List<PodcastItem> podcasts = PodcastUtilities.GetPodcasts(context);

                final int autoAssignDefaultPlaylistId = getResources().getInteger(R.integer.default_playlist_select);
                final SharedPreferences.Editor editor = prefs.edit();

                for (final PodcastItem podcast : podcasts)
                    editor.putString("pref_" + podcast.getPodcastId() + "_auto_assign_playlist", String.valueOf(autoAssignDefaultPlaylistId));

                final int swipeActionId = Integer.valueOf(Objects.requireNonNull(prefs.getString("pref_episodes_swipe_action", "0")));

                if (swipeActionId > 0) {
                    editor.putString("pref_episodes_swipe_action", "0");
                    editor.apply();
                }

                db.deleteAllPlaylists();

                for (int i = 1; i <= number; i++)
                    db.insertPlaylist(getString(R.string.playlists_title_default, i));
            }

            if (type == DataEvent.TYPE_CHANGED && path.equals("/podcastimport")) {

                final PodcastItem podcast = new PodcastItem();
                podcast.setTitle(dataMapItem.getDataMap().getString("title"));

                final ChannelItem channel = new ChannelItem();
                channel.setRSSUrl(dataMapItem.getDataMap().getString("rss_url"));
                channel.setThumbnailUrl(dataMapItem.getDataMap().getString("rss_url"));
                channel.setSiteUrl(dataMapItem.getDataMap().getString("site_url"));

                if (dataMapItem.getDataMap().getString("thumbnail_url") != null) {
                    channel.setThumbnailUrl(dataMapItem.getDataMap().getString("thumbnail_url"));
                    channel.setThumbnailName(dataMapItem.getDataMap().getString("thumbnail_name"));
                }

                podcast.setChannel(channel);

                DBUtilities.insertPodcast(context, podcast);

                Utilities.vibrate(this);
            }

            if (type == DataEvent.TYPE_CHANGED && path.equals("/opmlimport")) {

                final Asset asset = dataMapItem.getDataMap().getAsset("opml");

                final Task<DataClient.GetFdForAssetResponse> getFdForAssetResponseTask = Wearable.getDataClient(getApplicationContext()).getFdForAsset(asset);

                DataClient.GetFdForAssetResponse getFdForAssetResponse = null;
                try {
                    getFdForAssetResponse = Tasks.await(getFdForAssetResponseTask);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }

                final InputStream in = getFdForAssetResponse.getInputStream();

                final List<PodcastItem> podcasts = OPMLParser.parse(this, in);

                //podcasts
                CommonUtils.executeSingleThreadAsync(new InsertPodcasts(context, podcasts), (response) -> {

                    //episodes
                    CommonUtils.DeviceSync(mContext.get(), PutDataMapRequest.create("/opmlimport_episodes"));
                    CommonUtils.executeSingleThreadAsync(new SyncPodcasts(context), (data1) -> {

                        //art
                        CommonUtils.DeviceSync(mContext.get(), PutDataMapRequest.create("/opmlimport_art"));
                        CommonUtils.executeSingleThreadAsync(new SyncArt(context), (data2) -> {

                            CommonUtils.DeviceSync(mContext.get(), PutDataMapRequest.create("/opmlimport_complete"));
                            Utilities.vibrate(context);
                        });
                    });
                });
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

    private static class TimeOutHandler extends Handler {
        private final WeakReference<ImportService> mMainActivityWeakReference;

        TimeOutHandler(final ImportService service) {
            mMainActivityWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(final Message msg) {
            final ImportService service = mMainActivityWeakReference.get();

            if (service != null) {
                switch (msg.what) {
                    case MESSAGE_CONNECTIVITY_TIMEOUT:
                        service.unregisterNetworkCallback();
                        mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);
                        CommonUtils.showToast(mContext.get(), mContext.get().getString(R.string.alert_no_network));
                        if (!Utilities.BluetoothEnabled())
                            Utilities.enableBluetooth(mContext.get());
                        break;
                }
            }
        }
    }

    private void unregisterNetworkCallback() {
        if (mManager != null && mNetworkCallback != null) {
            mManager.unregisterNetworkCallback(mNetworkCallback);
            mManager.bindProcessToNetwork(null);
            mNetworkCallback = null;
        }
    }
}
