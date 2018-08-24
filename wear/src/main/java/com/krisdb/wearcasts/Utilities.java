package com.krisdb.wearcasts;


import android.app.DownloadManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.FeedParser;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.krisdb.wearcastslibrary.CommonUtils.GetThumbnailDirectory;

public class Utilities {

    static List<NavItem> getNavItems(final Context ctx) {
        return getNavItems(ctx, BluetoothAdapter.getDefaultAdapter());
    }

    static List<NavItem> getNavItems(final Context ctx, final BluetoothAdapter adapter)
    {
        List<NavItem> items = new ArrayList<>();
        final NavItem navItemDiscover = new NavItem();
        navItemDiscover.setID(0);
        navItemDiscover.setTitle(ctx.getString(R.string.nav_main_discover));
        navItemDiscover.setIcon("ic_action_add_podcast");
        items.add(navItemDiscover);

        /*
        CommonUtils.showToast(ctx, adapter.isEnabled() ? "Bluetooth on" : "Bluetooth off");

        if (adapter != null) {
            final NavItem navToggleBluetooth = new NavItem();
            navToggleBluetooth.setID(1);
            if (adapter.isEnabled()) {
                navToggleBluetooth.setTitle("Bluetooth Off");
                navToggleBluetooth.setIcon("ic_setting_bluetooth_off");
            } else {
                navToggleBluetooth.setTitle("Bluetooth On");
                navToggleBluetooth.setIcon("ic_setting_bluetooth_on");
            }
            items.add(navToggleBluetooth);
        }
        */
        final NavItem navItemSettings = new NavItem();
        navItemSettings.setID(2);
        navItemSettings.setTitle(ctx.getString(R.string.nav_main_settings));
        navItemSettings.setIcon("ic_action_settings");
        items.add(navItemSettings);

        return items;
    }

    static Boolean hasPremium(final Context ctx)
    {
        return ((0 != (ctx.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) || PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("premium", false));
    }

    public static int getThemeOptionId(final Context ctx)
    {
        return Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(ctx).getString("pref_theme", String.valueOf(ctx.getResources().getInteger(R.integer.default_theme))));
    }

    public static int getTheme(final Context ctx)
    {
        final int option = getThemeOptionId(ctx);

        int output;

        if (option == Enums.ThemeOptions.DARK.getThemeId())
            output = R.style.Dark;
        else if (option == Enums.ThemeOptions.LIGHT.getThemeId())
            output = R.style.Light;
        else if (option == Enums.ThemeOptions.AMOLED.getThemeId())
            output = R.style.AMOLED;
        else
            output = R.style.Main;

        return output;
    }


    public static void resetHomeScreen(final Context ctx)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString("pref_display_home_screen", "0");
        editor.apply();
    }

    public static Boolean BluetoothEnabled()
    {
        return HasBluetooth() && BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    public static Boolean HasBluetooth()
    {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    public static int getHeaderColor(final Context ctx) {

        final int headerId = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(ctx).getString("pref_header_color", String.valueOf(ctx.getResources().getInteger(R.integer.default_header_color))));

        switch (headerId)
        {
            case 0:
                return ctx.getColor(R.color.wc_header_color_amoled);
            case 1:
                return ctx.getColor(R.color.wc_header_color_blue);
            case 2:
                return ctx.getColor(R.color.wc_header_color_lite_blue);
            case 3:
                return ctx.getColor(R.color.wc_header_color_dark_blue);
            case 4:
                return ctx.getColor(R.color.wc_header_color_gray);
            case 5:
                return ctx.getColor(R.color.wc_header_color_dark_grey);
            case 6:
                return ctx.getColor(R.color.wc_header_color_green);
            case 7:
                return ctx.getColor(R.color.wc_header_color_dark_green);
            case 8:
                return ctx.getColor(R.color.wc_header_color_orange);
            case 9:
                return ctx.getColor(R.color.wc_header_color_dark_orange);
            case 10:
                return ctx.getColor(R.color.wc_header_color_red);
            case 11:
                return ctx.getColor(R.color.wc_header_color_purple);
        }

       return ctx.getColor(R.color.wc_header_color_red);
    }

    static int getBackgroundColor(final Context ctx)
    {
        final int optionId = Utilities.getThemeOptionId(ctx);

        if (optionId == Enums.ThemeOptions.LIGHT.getThemeId())
            return ctx.getColor(R.color.wc_background_light);
        else if (optionId == Enums.ThemeOptions.DARK.getThemeId())
            return ctx.getColor(R.color.wc_background_dark);
        else if (optionId == Enums.ThemeOptions.AMOLED.getThemeId())
            return ctx.getColor(R.color.wc_background_amoled);
        else
            return ctx.getColor(R.color.wc_transparent);
    }

    static void StartJob(final Context ctx) {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        final ComponentName serviceComponent = new ComponentName(ctx, BackgroundService.class);

        final JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setPeriodic(Long.valueOf(prefs.getString("updateInterval", String.valueOf(ctx.getResources().getInteger(R.integer.default_update_interval)))));
        builder.setPersisted(true);

        builder.setRequiresCharging(prefs.getBoolean("updateCharging", true));

        final JobScheduler jobScheduler = ctx.getSystemService(JobScheduler.class);

        if (jobScheduler != null)
            jobScheduler.schedule(builder.build());
    }

    static void CancelJob(final Context ctx)
    {
        final JobScheduler jobScheduler = ctx.getSystemService(JobScheduler.class);

        if (jobScheduler != null)
            jobScheduler.cancelAll();
    }

    static String GetOrderClause(int orderId)
    {
        String orderString;

        if (orderId == Enums.SortOrder.NAMEASC.getSorderOrderCode())
            orderString = "[title] ASC";
        else if (orderId == Enums.SortOrder.NAMEDESC.getSorderOrderCode())
            orderString = "[title] DESC";
        else if (orderId == Enums.SortOrder.DATEASC.getSorderOrderCode())
            orderString = "[pubDate] ASC";
        else if (orderId == Enums.SortOrder.DATEDESC.getSorderOrderCode())
            orderString = "[pubDate] DESC";
        else if (orderId == Enums.SortOrder.DATEDOWNLOADED_DESC.getSorderOrderCode())
            orderString = "[dateDownload] DESC";
        else if (orderId == Enums.SortOrder.DATEDOWNLOADED_ASC.getSorderOrderCode())
            orderString = "[dateDownload] ASC";
        else if (orderId == Enums.SortOrder.DATEADDED_ASC.getSorderOrderCode())
            orderString = "[dateAdded] ASC";
        else if (orderId == Enums.SortOrder.DATEADDED_DESC.getSorderOrderCode())
            orderString = "[dateAdded] DESC";
        else if (orderId == Enums.SortOrder.PROGRESS.getSorderOrderCode())
            orderString = "[position] DESC";
        else if (orderId == Enums.SortOrder.NEWEPISODES.getSorderOrderCode())
            orderString = "[title] ASC";
        else
            orderString = "[title] ASC";

        return orderString;
    }

    static String GetLocalPositionKey(final String name)
    {
        return "local_file_position_".concat(CommonUtils.CleanString(name));
    }

    static String GetLocalDurationKey(final String name)
    {
        return "local_file_duration_".concat(CommonUtils.CleanString(name));
    }

    static int[] ProcessEpisodes(final Context ctx, final PodcastItem podcast) {

        final int limit = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(ctx).getString("pref_episode_limit", "50"));

        final List<PodcastItem> episodes = FeedParser.parse(podcast, limit);
        int[] quantities = new int[2];

        if (episodes == null || episodes.size() == 0) return quantities;

        final PodcastItem latestPodcast = DBUtilities.GetLatestEpisode(ctx, episodes.get(0).getPodcastId());

        final Date latestEpisodeDate = latestPodcast != null && latestPodcast.getPubDate() != null ? DateUtils.ConvertDate(latestPodcast.getPubDate(), "yyyy-MM-dd HH:mm:ss") : null;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final boolean autoDownload = prefs.getBoolean("pref_" + podcast.getPodcastId() + "_auto_download", false);
        final int autoAssignDefaultPlaylistId = ctx.getResources().getInteger(R.integer.default_playlist_select);
        final int autoAssignPlaylistId = Integer.valueOf(prefs.getString("pref_" + podcast.getPodcastId() + "_auto_assign_playlist", String.valueOf(autoAssignDefaultPlaylistId)));
        final Boolean assignPlaylist = autoAssignPlaylistId != autoAssignDefaultPlaylistId;
        final Boolean disabledBT = prefs.getBoolean("pref_disable_bluetooth", false);
        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        final Boolean hasBTAdapter = btAdapter != null;
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        int newEpisodesCount = 0, downloadCount = 0;

        for (final PodcastItem episode : episodes)
        {
            try {
                if (episode == null || episode.getPubDate() == null) continue;

                final Date episodeDate = DateUtils.ConvertDate(episode.getPubDate(), "yyyy-MM-dd HH:mm:ss");

                if (latestEpisodeDate != null && (latestEpisodeDate.equals(episodeDate) || latestEpisodeDate.after(episodeDate)) || episode.getMediaUrl() == null || DBUtilities.episodeExists(ctx, episode.getMediaUrl().toString()))
                    continue;

                if (autoDownload && hasBTAdapter && disabledBT && btAdapter.isEnabled()) {
                    btAdapter.disable();
                    SystemClock.sleep(5000);
                }

                final ContentValues cv = new ContentValues();
                cv.put("pid", episode.getPodcastId());
                cv.put("title", episode.getTitle() != null ? episode.getTitle() : "");
                cv.put("description", episode.getDescription());

                if (episode.getMediaUrl() != null)
                    cv.put("mediaurl", episode.getMediaUrl().toString());

                if (episode.getEpisodeUrl() != null)
                    cv.put("url", episode.getEpisodeUrl().toString());
                cv.put("dateAdded", DateUtils.GetDate());
                cv.put("pubDate", episode.getPubDate());
                cv.put("duration", episode.getDuration());

                final long episodeId = db.insert(cv);
                episode.setEpisodeId((int) episodeId);

                if (assignPlaylist) {
                    db.addEpisodeToPlaylist(autoAssignPlaylistId, (int) episodeId);
                }

                if (autoDownload) {
                    startDownload(ctx, episode);
                    downloadCount++;
                }

                newEpisodesCount++;
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
        db.close();
        DBUtilities.TrimEpisodes(ctx, podcast);

        if (downloadCount > 0 && prefs.getBoolean("cleanup_downloads", false) == false)
        {
            //need to detect in download receiver that the download is from a background update and not manual download
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("cleanup_downloads", true);
            editor.apply();
        }

        quantities[0] = newEpisodesCount;
        quantities[1] = downloadCount;

        return quantities;
    }

    static long startDownload(final Context ctx, final PodcastItem episode) {

        if (episode.getMediaUrl() == null) return 0;

        final DownloadManager manager = (DownloadManager) ctx.getSystemService(DOWNLOAD_SERVICE);

        final DownloadManager.Request download = new DownloadManager.Request(Uri.parse(episode.getMediaUrl().toString()));
        download.setTitle(episode.getTitle());
        download.setDestinationInExternalPublicDir("/WearCasts/Episodes", Utilities.GetEpisodeFileName(episode));

        final long downloadId = manager.enqueue(download);

        DBUtilities.SaveEpisodeValue(ctx, episode, "downloadid", downloadId);

        return downloadId;
    }

    static void cancelAllDownloads(final Context ctx) {
        final DownloadManager manager = (DownloadManager) ctx.getSystemService(DOWNLOAD_SERVICE);

        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);

        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT [id],[downloadid] FROM [tbl_podcast_episodes] WHERE [downloadid] <> 0", null);
        final ContentValues cv = new ContentValues();
        cv.put("download", 0);
        cv.put("downloadid", 0);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                manager.remove(cursor.getInt(1));
                db.update(cv, cursor.getInt(0));
                cursor.moveToNext();
            }
        }

        sdb.close();
        db.close();
    }

    static int getDownloadId(final Context ctx, final int episodeId)
    {
        int downloadId = 0;
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT [downloadid] FROM [tbl_podcast_episodes] WHERE [id] = ?", new String[]{String.valueOf(episodeId)});

        if (cursor.moveToFirst())
            downloadId = cursor.getInt(0);

        cursor.close();
        sdb.close();
        db.close();

        return downloadId;
    }

    public static int getThumbMaxWidth(final Context ctx, final String density, final Boolean isRound)
    {
        if (Objects.equals(density, ctx.getString(R.string.hdpi)))
        {
            if (isRound)
                return (int)ctx.getResources().getDimension(R.dimen.thumbs_width_episode_list_title_hdpi_round);
            else
                return (int)ctx.getResources().getDimension(R.dimen.thumbs_width_episode_list_title_hdpi_square);
        }
        else if (Objects.equals(density, ctx.getString(R.string.xhdpi)))
            return  (int)ctx.getResources().getDimension(R.dimen.thumbs_width_episode_list_title_xhdpi);
        else
            return  (int)ctx.getResources().getDimension(R.dimen.thumbs_width_episode_list_title_default);
    }

    public static boolean HeadsetConnected(Context ctx) {
        BluetoothManager bluetoothManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);

        return devices.size() > 0;
    }

    static String GetDownloadErrorReason(final Context ctx, final int reasonId)
    {
        switch(reasonId){
            case DownloadManager.ERROR_CANNOT_RESUME:
                return ctx.getString(R.string.alert_download_error_noresume);
            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                return ctx.getString(R.string.alert_download_error_device_not_found);
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                return ctx.getString(R.string.alert_download_error_file_exists);
            case DownloadManager.ERROR_FILE_ERROR:
                return ctx.getString(R.string.alert_download_error_file_error);
            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                return ctx.getString(R.string.alert_download_error_data_error);
            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                return ctx.getString(R.string.alert_download_error_low_disk);
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                return ctx.getString(R.string.alert_download_error_redirects);
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                return ctx.getString(R.string.alert_download_error_unhandled_http);
            case DownloadManager.ERROR_UNKNOWN:
                return ctx.getString(R.string.alert_download_error_unknown);
            case 404:
                return ctx.getString(R.string.alert_download_error_not_found);
        }

        return ctx.getString(R.string.alert_download_error_default);
    }

     public static void ShowPlayingNotification(final Context ctx, final PodcastItem episode) {
         PodcastItem podcast = DBUtilities.GetPodcast(ctx, episode.getPodcastId());

         Intent notificationIntent = new Intent(ctx, PodcastEpisodeActivity.class);
         notificationIntent.setFlags(Notification.FLAG_ONGOING_EVENT);
         notificationIntent.setFlags(Notification.FLAG_NO_CLEAR);
         notificationIntent.setFlags(Notification.FLAG_FOREGROUND_SERVICE);

         Bundle bundle = new Bundle();
         bundle.putInt("eid", episode.getEpisodeId());
         notificationIntent.putExtras(bundle);

         NotificationCompat.Builder notificationBuilder =
                 new NotificationCompat.Builder(ctx)
                         .setSmallIcon(R.drawable.ic_notification)
                         .setContentTitle(podcast.getTitle())
                         .setContentText(episode.getTitle())
                         .setOngoing(true)
                         .setAutoCancel(false)
                         .setContentIntent(PendingIntent.getActivity(ctx, episode.getEpisodeId(), notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT));

         NotificationManagerCompat.from(ctx).notify(100, notificationBuilder.build());
     }

    static boolean IsNetworkConnected(final Context ctx) {
        final ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm != null && cm.getActiveNetwork() != null;
    }

    static void DeleteMediaFile(final Context ctx, final PodcastItem episode)
    {
        if (episode.getPodcastId() == ctx.getResources().getInteger(R.integer.episode_with_no_podcast_id))
        {
            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
            db.delete(episode.getEpisodeId());
            db.close();
        }
        else
            DBUtilities.SaveEpisodeValue(ctx, episode, "download", 0);

        String fileName = Utilities.GetMediaFile(ctx, episode);

        if (fileName == null) return;

        fileName = fileName.replace("file://","");

        final File file = new File(fileName);

        if (file.exists())
            file.delete();
    }

    public static int downloadsCount()
    {
        final File episodesDirectory = new File(GetMediaDirectory());
        final String[] episodes = episodesDirectory.list();

        return episodes.length;
    }

    public static long getFilesSize(final String path)
    {
        long bytes = 0;
        final File episodesDirectory = new File(path);
        final String[] episodes = episodesDirectory.list();

        if (episodes != null) {
            for (final String episode : episodes) {
                bytes = bytes + new File(episodesDirectory, episode).length();
            }
        }
        return bytes;
    }

    public static int deleteAllDownloadedFiles()
    {
        final File episodesDirectory = new File(GetMediaDirectory());
        final String[] episodes = episodesDirectory.list();

        final int episodesLength = episodes.length;

        for (final String episode : episodes)
            new File(episodesDirectory, episode).delete();

        return episodesLength;
    }

    public static int deleteAllThumbnails()
    {
        final File thumbsDirectory = new File(GetThumbnailDirectory());
        final String[] thumbs = thumbsDirectory.list();

        final int length = thumbs.length;

        for (final String thumb : thumbs)
            new File(thumbsDirectory, thumb).delete();

        return length;
    }

    static void DeleteFiles(final Context ctx, final int podcastId)
    {
        final List<PodcastItem> episodes = DBUtilities.GetEpisodesWithDownloads(ctx, podcastId);

        for(final PodcastItem episode : episodes)
            DeleteMediaFile(ctx, episode);
    }

     static String GetMediaFile(final Context ctx, final PodcastItem episode)
    {
          return GetMediaDirectory().concat(GetEpisodeFileName(episode));
    }

     static String GetMediaFileOLD(final Context ctx, final PodcastItem episode)
    {
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
        final SQLiteDatabase sdb = db.select();

        final Cursor cursor = sdb.rawQuery("SELECT [downloadurl] FROM [tbl_podcast_episodes] WHERE [id] = ?",
                new String[] { String.valueOf(episode.getEpisodeId())
                });

        String value = null;

        if (cursor.moveToFirst())
            value = cursor.getString(0);

        cursor.close();
        db.close();

        return value;
    }

    static String GetMediaDirectory() {
        return CommonUtils.GetDirectory().concat("/Episodes/");
    }

    private static String GetEpisodeFileName(final PodcastItem episode) {
        return episode.getEpisodeId() + ".mp3";
    }
}
