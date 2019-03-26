package com.krisdb.wearcasts.Utilities;


import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.krisdb.wearcasts.Activities.MainActivity;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Models.NavItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Services.BackgroundService;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodesWithDownloads;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.SaveEpisodeValue;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.assignedToPlaylist;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.GetMediaDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.GetThumbnailDirectory;

public class Utilities {

    public static List<NavItem> getNavItems(final Context ctx)
    {
        List<NavItem> items = new ArrayList<>();
        final NavItem navItemDiscover = new NavItem();
        navItemDiscover.setID(0);
        navItemDiscover.setTitle(ctx.getString(R.string.nav_main_discover));
        navItemDiscover.setIcon("ic_action_add_podcast");
        items.add(navItemDiscover);

        /*
        final NavItem navItemUpdate = new NavItem();
        navItemUpdate.setID(1);
        navItemUpdate.setTitle(ctx.getString(R.string.nav_main_update));
        navItemUpdate.setIcon("ic_action_menu_main_refresh");
        items.add(navItemUpdate);
        */

        final NavItem navItemSettings = new NavItem();
        navItemSettings.setID(2);
        navItemSettings.setTitle(ctx.getString(R.string.nav_main_settings));
        navItemSettings.setIcon("ic_action_menu_main_settings");
        items.add(navItemSettings);

        return items;
    }

    public static void showNewEpisodesNotification(final Context ctx, final int episodeCount, final int downloadCount)
    {
        if (episodeCount == 0 || !PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("pref_updates_notification", true)) return;

        String displayMessage = episodeCount > 1 ? ctx.getString(R.string.plurals_multiple_new_episodes, episodeCount) : ctx.getString(R.string.plurals_single_new_episode);

        if (downloadCount > 0)
            displayMessage = displayMessage.concat("\n").concat(downloadCount > 1 ? ctx.getString(R.string.notification_downloads_count, downloadCount) : ctx.getString(R.string.notification_download_count));

        final Intent notificationIntent = new Intent(ctx, MainActivity.class);
        final Bundle bundle = new Bundle();
        bundle.putBoolean("new_episodes", true);
        notificationIntent.putExtras(bundle);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        final PendingIntent intent = PendingIntent.getActivity(ctx, 5, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            final String channelID = ctx.getPackageName().concat(".newepisodes");

            final NotificationManager mNotificationManager = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);

            final NotificationChannel mChannel = new NotificationChannel(channelID, ctx.getString(R.string.notification_channel_newepisodes), NotificationManager.IMPORTANCE_DEFAULT);
            mChannel.setDescription(displayMessage);
            mNotificationManager.createNotificationChannel(mChannel);

            final Notification notification = new NotificationCompat.Builder(ctx, channelID)
                    .setContentIntent(intent)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(ctx.getString(R.string.app_name_wc))
                    .setContentText(displayMessage).build();

            mNotificationManager.notify(102, notification);
        } else {
            final NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(ctx)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(ctx.getString(R.string.app_name_wc))
                            .setContentText(displayMessage)
                            .setContentIntent(intent);

            NotificationManagerCompat.from(ctx).notify(102, notificationBuilder.build());
        }
    }

    public static Boolean hasPremium(final Context ctx)
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

    public static Boolean WifiEnabled(final Context ctx)
    {
        return HasWifi(ctx) && ((WifiManager)ctx.getSystemService(Context.WIFI_SERVICE)).isWifiEnabled();
    }

    public static Boolean HasWifi(final Context ctx)
    {
        return ctx.getSystemService(Context.WIFI_SERVICE) != null;
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

    public static int getBackgroundColor(final Context ctx)
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

    public static void StartJob(final Context ctx) {

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

    public static void CancelJob(final Context ctx)
    {
        final JobScheduler jobScheduler = ctx.getSystemService(JobScheduler.class);

        if (jobScheduler != null)
            jobScheduler.cancelAll();
    }

    public static String GetOrderClause(final int orderId) {
        return  GetOrderClause(orderId, null);
    }

    public static String GetOrderClause(final int orderId, final String db)
    {
        String orderString;

        if (orderId == Enums.SortOrder.NAMEASC.getSorderOrderCode())
            orderString = "title ASC";
        else if (orderId == Enums.SortOrder.NAMEDESC.getSorderOrderCode())
            orderString = "title DESC";
        else if (orderId == Enums.SortOrder.DATEASC.getSorderOrderCode())
            orderString = "pubDate ASC";
        else if (orderId == Enums.SortOrder.DATEDESC.getSorderOrderCode())
            orderString = "pubDate DESC";
        else if (orderId == Enums.SortOrder.DATEDOWNLOADED_DESC.getSorderOrderCode())
            orderString = "dateDownload DESC";
        else if (orderId == Enums.SortOrder.DATEDOWNLOADED_ASC.getSorderOrderCode())
            orderString = "dateDownload ASC";
        else if (orderId == Enums.SortOrder.DATEADDED_ASC.getSorderOrderCode())
            orderString = "dateAdded ASC";
        else if (orderId == Enums.SortOrder.DATEADDED_DESC.getSorderOrderCode())
            orderString = "dateAdded DESC";
        else if (orderId == Enums.SortOrder.PROGRESS.getSorderOrderCode())
            orderString = "position DESC";
        else if (orderId == Enums.SortOrder.NEWEPISODES.getSorderOrderCode())
            orderString = "title ASC";
        else
            orderString = "title ASC";

        if (db != null)
            orderString = db.concat(".").concat(orderString);

        return orderString;
    }

    public static String GetLocalPositionKey(final String name)
    {
        return "local_file_position_".concat(CommonUtils.CleanString(name));
    }

    public static String GetLocalDurationKey(final String name)
    {
        return "local_file_duration_".concat(CommonUtils.CleanString(name));
    }

    public static long startDownload(final Context ctx, final PodcastItem episode) {

        if (episode.getMediaUrl() == null) return 0;

        final DownloadManager manager = (DownloadManager) ctx.getSystemService(DOWNLOAD_SERVICE);

        final DownloadManager.Request download = new DownloadManager.Request(Uri.parse(episode.getMediaUrl().toString()));
        download.setTitle(episode.getTitle());

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
            download.setDestinationInExternalPublicDir(ctx.getString(com.krisdb.wearcastslibrary.R.string.directory_episodes), Utilities.GetEpisodeFileName(episode));
        else
            download.setDestinationInExternalFilesDir(ctx, Environment.DIRECTORY_PODCASTS,ctx.getString(com.krisdb.wearcastslibrary.R.string.directory_episodes).concat(Utilities.GetEpisodeFileName(episode)));

        final long downloadId = manager.enqueue(download);

        SaveEpisodeValue(ctx, episode, "downloadid", downloadId);

        return downloadId;
    }

    public static void cancelAllDownloads(final Context ctx) {
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

    public static int getDownloadId(final Context ctx, final int episodeId)
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

    public static String GetDownloadErrorReason(final Context ctx, final int reasonId)
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


    public static void deleteLocal(final Context ctx, final String title)
    {
        final File localFile = new File(GetLocalDirectory(ctx).concat(title));

        if (localFile.exists())
            localFile.delete();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final SharedPreferences.Editor editor = prefs.edit();

        editor.remove(Utilities.GetLocalDurationKey(title));
        editor.remove(Utilities.GetLocalPositionKey(title));
        editor.apply();
    }

    public static void DeleteMediaFile(final Context ctx, final PodcastItem episode)
    {
        //episodes sent from the phone app shouldn't be assigned to any playlist, so delete those.  This
        //prevent third-party episodes from being deleted
        if (episode.getPodcastId() == ctx.getResources().getInteger(R.integer.episode_with_no_podcast_id) && assignedToPlaylist(ctx, episode.getEpisodeId()) == false)
        {
            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);
            db.delete(episode.getEpisodeId());
            db.close();
        }
        else {
            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(ctx);

            final ContentValues cv = new ContentValues();
            cv.put("download", 0);
            cv.put("downloadid", 0);
            db.update(cv, episode.getEpisodeId());
            //SaveEpisodeValue(ctx, episode, "download", 0);
        }

        String fileName = Utilities.GetMediaFile(ctx, episode);

        fileName = fileName.replace("file://","");

        final File file = new File(fileName);

        if (file.exists())
            file.delete();
    }

    public static int downloadsCount(final Context ctx)
    {
        final File episodesDirectory = new File(GetMediaDirectory(ctx));
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

    public static int deleteAllDownloadedFiles(final Context ctx)
    {
        final File episodesDirectory = new File(GetMediaDirectory(ctx));
        final String[] episodes = episodesDirectory.list();

        final int episodesLength = episodes.length;

        for (final String episode : episodes)
            new File(episodesDirectory, episode).delete();

        return episodesLength;
    }

    public static int deleteAllThumbnails(final Context ctx)
    {
        final File thumbsDirectory = new File(GetThumbnailDirectory(ctx));
        final String[] thumbs = thumbsDirectory.list();

        final int length = thumbs.length;

        for (final String thumb : thumbs)
            new File(thumbsDirectory, thumb).delete();

        return length;
    }

    public static void DeleteFiles(final Context ctx, final int podcastId)
    {
        final List<PodcastItem> episodes = GetEpisodesWithDownloads(ctx, podcastId);

        for(final PodcastItem episode : episodes)
            DeleteMediaFile(ctx, episode);
    }

    public static String GetMediaFile(final Context ctx, final PodcastItem episode)
    {
          return GetMediaDirectory(ctx).concat(GetEpisodeFileName(episode));
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

    private static String GetEpisodeFileName(final PodcastItem episode) {
        return episode.getEpisodeId() + ".mp3";
    }
}
