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

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.krisdb.wearcasts.Async.ProcessOPML;
import com.krisdb.wearcasts.Async.SaveLogo;
import com.krisdb.wearcasts.Async.SyncArt;
import com.krisdb.wearcasts.Async.SyncPodcasts;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.DBUtilities;
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
import java.util.concurrent.TimeUnit;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodeByTitle;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;

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
                    final DataMap dm = dataMapItem.getDataMap();
                    episode = new PodcastItem();
                    episode.setPodcastId(getResources().getInteger(R.integer.episode_with_no_podcast_id));
                    episode.setTitle(dm.getString("title"));
                    episode.setDescription(dm.getString("description"));
                    episode.setPubDate(dm.getString("pubDate"));
                    episode.setMediaUrl(dm.getString("mediaurl"));
                    episode.setEpisodeUrl(dm.getString("url"));
                    episode.setDuration(dm.getInt("duration"));
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
                    cv.put("thumbnail_url", dm.getString("thumb_url"));

                    final long episodeId = db.insert(cv);
                    episode.setEpisodeId((int) episodeId);

                    if (dm.getString("thumb_url") != null)
                        CommonUtils.executeSingleThreadAsync(new SaveLogo(context, dm.getString("thumb_url"), CommonUtils.GetEpisodesThumbnailDirectory(context), CommonUtils.GetThumbnailName((int)episodeId)), (response) -> { });
                }

                final int playlist = dataMapItem.getDataMap().getInt("playlistid");

                //add third parties to their playlist
                if (playlist < 0) {
                    CommonUtils.DeviceSync(this, PutDataMapRequest.create("/thirdparty"));
                    db.addEpisodeToPlaylist(playlist, episode.getEpisodeId());
                }

                db.close();

                if (dataMapItem.getDataMap().getInt("playlistid") == 0 || dataMapItem.getDataMap().getBoolean("auto_download")) {

                    if (Utilities.disableBluetooth(context)) {
                        unregisterNetworkCallback();

                        if (!CommonUtils.isNetworkAvailable(context, true))
                            CommonUtils.showToast(context, context.getString(R.string.alert_episode_network_waiting));

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
                SendToDevice("started", 0, 0);

                CommonUtils.executeAsync(new ConvertFileToAsset(context, asset), (response) -> {
                    final InputStream inputStream = response.getInputStream();

                    try {
                        int size = 1024;
                        final File f = new File(GetLocalDirectory(mContext.get()).concat(fileName));
                        final OutputStream outputStream = new FileOutputStream(f);
                        final byte buffer[] = new byte[size];

                        int bytes, totalSize = inputStream.available();
                        long total = 0;

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

                final String thumbUrl = dataMapItem.getDataMap().getString("thumbnail_url");

                final ChannelItem channel = new ChannelItem();
                channel.setTitle(dataMapItem.getDataMap().getString("title"));
                channel.setRSSUrl(dataMapItem.getDataMap().getString("rss_url"));
                channel.setThumbnailUrl(dataMapItem.getDataMap().getString("rss_url"));
                channel.setSiteUrl(dataMapItem.getDataMap().getString("site_url"));

                if (thumbUrl != null)
                    channel.setThumbnailUrl(thumbUrl);

                podcast.setChannel(channel);

                final int podcastID = DBUtilities.insertPodcast(context, podcast);

                if (thumbUrl!= null)
                    CommonUtils.executeSingleThreadAsync(new SaveLogo(context, thumbUrl, CommonUtils.GetPodcastsThumbnailDirectory(context), CommonUtils.GetThumbnailName(podcastID)), (response) -> { });

                Utilities.vibrate(this);
            }

            if (type == DataEvent.TYPE_CHANGED && path.equals("/opmlimport")) {

                final Asset asset = dataMapItem.getDataMap().getAsset("opml");

                //podcasts
                CommonUtils.executeSingleThreadAsync(new ProcessOPML(context, asset, true), (response) -> {
                    //episodes
                    CommonUtils.executeSingleThreadAsync(new SyncPodcasts(context, true), (data1) -> {
                        //art
                        CommonUtils.executeAsync(new SyncArt(context, true), (data2) -> {
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
