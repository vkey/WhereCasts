package com.krisdb.wearcastslibrary;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.util.Pair;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.DOWNLOAD_SERVICE;
import static android.os.Environment.getExternalStorageDirectory;

public class CommonUtils {

    public static void showToast(final Context ctx, final String message) {

        showToast(ctx, message, Toast.LENGTH_SHORT);
    }

    public static Network getActiveNetwork(final Context ctx) {
        final ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetwork();
    }


    public static Boolean HighBandwidthNetwork(final Context ctx)
    {
        if (PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("pref_high_bandwidth", true) == false)
            return true;

        final Network activeNetwork = getActiveNetwork(ctx);

        if (activeNetwork != null) {
            final ConnectivityManager manager = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

            final int bandwidth = manager.getNetworkCapabilities(activeNetwork).getLinkDownstreamBandwidthKbps();

            return bandwidth > ctx.getResources().getInteger(R.integer.minimum_bandwidth);
        }

        return false;
    }

    public static void showToast(final Context ctx, final String message, final int length)
    {
        if (ctx instanceof Activity && ((Activity)ctx).isFinishing()) return;

        try {
            final Toast toast = Toast.makeText(ctx, message, length);
            final View view = toast.getView();
            final TextView text = view.findViewById(android.R.id.message);
            text.setBackgroundColor(Color.TRANSPARENT);
            toast.show();
        }catch (Exception ignored){}
    }

    public static String getRedirectUrl(final String url) {
        String output = null;
        HttpURLConnection ucon = null;
        try {
            final URL url2 = new URL(url);
            ucon = (HttpURLConnection) url2.openConnection();
            ucon.setInstanceFollowRedirects(false);
            final URL secondURL = new URL(ucon.getHeaderField("Location"));
            output = secondURL.toString();
            ucon.disconnect();
        } catch (Exception ex) {
            if (ucon != null)
                ucon.disconnect();
            ex.printStackTrace();
        }

        return output;
    }

    /*
    * Returns true/false if there is an active download
    */
    public static boolean isCurrentDownload(final Context ctx) {
        final DownloadManager.Query q = new DownloadManager.Query();
        q.setFilterByStatus(DownloadManager.STATUS_RUNNING | DownloadManager.STATUS_PENDING);

        final DownloadManager m = (DownloadManager) ctx.getSystemService(DOWNLOAD_SERVICE);

        return m != null && m.query(q).moveToFirst();
    }

    public static void DeviceSync(final Context ctx, final PutDataMapRequest dataMap)
    {
        DeviceSync(ctx, dataMap, null, Toast.LENGTH_SHORT);
    }

    public static int getCurrentPosition(final MediaPlayer mp)
    {
        int output = 0;
        try
        {
            output = mp.getCurrentPosition();
        }
        catch (IllegalStateException ex)
        {
            mp.reset();
        }

        return output;
    }

    public static InputStream getRemoteStream(final String url) {
        try {
            final HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "Wear Casts");
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(true);

            final int statusCode = conn.getResponseCode();

            if (statusCode == 301 || statusCode == 302) {
                final String redirectUrl = getRedirectUrl(url);

                if (redirectUrl != null)
                    return getRemoteStream(redirectUrl);
            }

            if (conn.getResponseCode() == 200)
                return conn.getInputStream();

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return null;
    }

    public static String getRemoteContent(final String url) {
        try {

            final InputStream stream = getRemoteStream(url);

            if (stream != null) {
                final BufferedReader r = new BufferedReader(new InputStreamReader(stream));
                final StringBuilder response = new StringBuilder();
                String line;

                while ((line = r.readLine()) != null)
                    response.append(line);

                return response.toString();
            } else
                return null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static void DeviceSync(final Context ctx, final PutDataMapRequest dataMap, final String message, final int toastLength)
    {
        dataMap.getDataMap().putLong("time", new Date().getTime());
        final PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();

        final Task<DataItem> task = Wearable.getDataClient(ctx).putDataItem(request);

        task.addOnSuccessListener(new OnSuccessListener<DataItem>() {
            @Override
            public void onSuccess(DataItem dataItem) {
                if (message != null)
                    showToast(ctx, message, toastLength);
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showToast(ctx, message, Toast.LENGTH_LONG);
            }
        });
    }

   public static String getDensityName(final Context context) {
        final float density = context.getResources().getDisplayMetrics().density;
        if (density >= 4.0)
            return context.getString(R.string.xxxhdpi);

        if (density >= 3.0)
            return context.getString(R.string.xxhdpi);

        if (density >= 2.0)
            return context.getString(R.string.xhdpi);

        if (density >= 1.5)
            return context.getString(R.string.hdpi);

        if (density >= 1.0)
            return context.getString(R.string.mdpi);

        return context.getString(R.string.ldpi);
    }

    public static float GetFloatResource(Resources res, int resid)
    {
        final TypedValue tv = new TypedValue();
        res.getValue(resid, tv, true);

        return tv.getFloat();
    }

    public static boolean IsCharging(final Context context)
    {
        final Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        final int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }

    synchronized public static PowerManager.WakeLock getLock(final Context context, PowerManager.WakeLock wl)
    {
        if (wl == null) {

            final PowerManager mgr = (PowerManager)context.getSystemService(Context.POWER_SERVICE);

            wl = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, context.getPackageName());
            wl.setReferenceCounted(true);
        }

        return(wl);
    }

    public static String CleanString(String str)
    {
        if (str == null) return "";
        return android.text.Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY).toString();
    }

    public static String stripHTML(String str)
    {
        if (str == null) return "";
        //str = str.replace(" ","");
        str = str.replaceAll("[ ':.–!&|{}_-]","");
        return str;
    }

    public static String CleanDescription(String str)
    {
        str = str.replaceAll("\\<[^>]*>","");
        str = str.replace("&#8217;","'");
        str = str.replace("[&#8230;]","");
        str = str.replace("&quot;","\"");
        str = str.replace("&#039;","'");
        str = str.replace("&nbsp;"," ");
        str = str.replace("&amp;","&");
        return str;
    }

    public static int GetDuration(String url)
    {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(url, new HashMap<String, String>());
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

        return Integer.parseInt(durationStr);
    }

    public static Uri GetLogoUri(PodcastItem podcast, String path)
    {
        String fileName = podcast.getChannel().getThumbnailName();
        File thumbnail = new File(path, fileName);

        if (thumbnail.exists())
            return Uri.fromFile(thumbnail);

        return null;
    }

    public static Pair<Integer, Integer> GetBackgroundColor(final PodcastItem podcast)
    {
        final Bitmap bitmap = BitmapFactory.decodeFile(CommonUtils.GetThumbnailDirectory() + podcast.getChannel().getThumbnailName());

        if (bitmap == null)
            return new Pair<>(0, -3355444);

        final Palette palette = Palette.from(bitmap).generate();

        Pair<Integer, Integer> p = null;

        if (palette.getDarkVibrantSwatch() != null) {
            p = new Pair<>(palette.getDarkVibrantSwatch().getRgb(), palette.getDarkVibrantSwatch().getBodyTextColor());
        } else if (palette.getDarkMutedSwatch() != null) {
            p = new Pair<>(palette.getDarkMutedSwatch().getRgb(), palette.getDarkMutedSwatch().getBodyTextColor());
        } else if (palette.getVibrantSwatch() != null) {
            p = new Pair<>(palette.getVibrantSwatch().getRgb(), palette.getVibrantSwatch().getBodyTextColor());
        }

        return p;
    }

    public static RoundedBitmapDrawable GetRoundedLogo(final Context ctx, final ChannelItem channelItem) {
        return GetRoundedLogo(ctx, channelItem, 0);
    }

    public static RoundedBitmapDrawable GetRoundedLogo(final Context ctx, final ChannelItem channelItem, int defaultResource) {
        RoundedBitmapDrawable rdb;

        if (channelItem != null && channelItem.getThumbnailUrl() != null)
            rdb = RoundedBitmapDrawableFactory.create(ctx.getResources(), GetThumbnailDirectory() + channelItem.getThumbnailName());
        else
            rdb = RoundedBitmapDrawableFactory.create(ctx.getResources(), BitmapFactory.decodeResource(ctx.getResources(), defaultResource));
            //rbd.setCornerRadius(Math.max(src.getWidth(), src.getHeight()) / 2.0f);

        if (rdb.getBitmap() == null)
            rdb = RoundedBitmapDrawableFactory.create(ctx.getResources(), BitmapFactory.decodeResource(ctx.getResources(), defaultResource));

        rdb.setCircular(true);

        return rdb;
    }

    public static Bitmap resizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    public static Bitmap GetLogo(final Context ctx, final PodcastItem podcast) {
        Bitmap output;

        if (podcast.getChannel() == null || podcast.getChannel().getThumbnailName() == null)
            output = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_thumb_default);
        else
            output = BitmapFactory.decodeFile(GetThumbnailDirectory() + podcast.getChannel().getThumbnailName());

        return output;
    }

    public static File GetLogoFile(final PodcastItem podcast, final String path)
    {
        return new File(path, podcast.getChannel().getThumbnailName());
    }

    public static String GetDirectory() {
        return getExternalStorageDirectory() + "/WearCasts";
    }

    public static String GetThumbnailDirectory() {
        return GetDirectory() + "/Images/Thumbnails/";
    }

    public static String GetLocalDirectory() {
        return GetDirectory() + "/Local/";
    }

    public static String GetThumbnailName(final ChannelItem channel) {
        return CleanString(stripHTML(channel.getTitle())) + ".png";
    }

    public static String GetThumbnailFullPath(ChannelItem channel) {
        return GetThumbnailDirectory() + GetThumbnailName(channel);
    }

    public static boolean isValidUrl(final String url) {
        final Pattern p = Patterns.WEB_URL;
        final Matcher m = p.matcher(url.toLowerCase());

        return m.matches();
    }

    public static void SavePodcastLogo(final Context ctx, final String url, final String path, final String filename, final int width)
    {
        SavePodcastLogo(ctx, url, path, filename, width, false);
    }

    public static void SavePodcastLogo(final Context ctx, final String url, final String path, final String filename, final int width, final Boolean force)
    {
        try {
            if (filename == null) return;

            final File file = new File(path, filename);

            if (force || file.exists() == false) {

                //final URLConnection ucon = new URL(url).openConnection();
                //ucon.setConnectTimeout(500);
                //ucon.setReadTimeout(500);

                //final InputStream is = ucon.getInputStream();

                //final BitmapFactory.Options options = new BitmapFactory.Options();
                //options.inJustDecodeBounds = true;
                //options.inSampleSize = 2;
                //final Bitmap b = BitmapFactory.decodeStream(new BufferedInputStream(is), null, options);
                //int height = b.getHeight();

                //Bitmap b = BitmapFactory.decodeStream(is);

                final Bitmap b = Glide.with(ctx)
                            .asBitmap()
                            .load(url)
                            .submit()
                            .get();

                final int origWidth = b.getWidth();
                final int origHeight = b.getHeight();
                final float scaleWidth = ((float) width) / origWidth;
                final float scaleHeight = ((float) width) / origHeight;

                final Matrix matrix = new Matrix();
                matrix.postScale(scaleWidth, scaleHeight);

                final Bitmap b2 = Bitmap.createBitmap(b, 0, 0, origWidth, origHeight, matrix, false);

                final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                b2.compress(Bitmap.CompressFormat.PNG, 100, outStream);

                final FileOutputStream fo = new FileOutputStream(file);
                fo.write(outStream.toByteArray());
                fo.flush();
                fo.close();
                outStream.flush();
                outStream.close();
                if (!b.isRecycled())
                    b.recycle();

                if (!b2.isRecycled())
                    b2.recycle();
                //is.reset();
                //is.close();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
