package com.krisdb.wearcastslibrary;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.text.HtmlCompat;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.material.snackbar.Snackbar;
import com.krisdb.wearcastslibrary.Async.WatchConnected;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.DOWNLOAD_SERVICE;
import static android.content.Context.MODE_APPEND;
import static android.os.Environment.getExternalStorageDirectory;

public class CommonUtils {

    public static void Log(final Context context, final String text)
    {
        if (inDebugMode(context))
            Log.d(context.getPackageName(), "(WearCasts Log) " + text);
    }

    public static boolean isNightModeActive(final Context context) {
        final int defaultNightMode = AppCompatDelegate.getDefaultNightMode();
        if (defaultNightMode == AppCompatDelegate.MODE_NIGHT_YES)
            return true;

        if (defaultNightMode == AppCompatDelegate.MODE_NIGHT_NO)
            return false;

        final int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
            case Configuration.UI_MODE_NIGHT_NO:
                return false;
            case Configuration.UI_MODE_NIGHT_YES:
                return true;
        }

        return false;
    }
    public static <R> void executeSingleThreadAsync(Callable<R> callable, Interfaces.Callback<R> callback) {
        executeAsync(callable, Executors.newSingleThreadExecutor(), callback);
    }

    public static <R> void executeMultiThreadAsync(Callable<R> callable, int corePoolSize, Interfaces.Callback<R> callback) {
        executeAsync(callable, new ThreadPoolExecutor(corePoolSize, corePoolSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>()), callback);
    }

    public static <R> void executeCachedAsync(Callable<R> callable, Interfaces.Callback<R> callback) {
        executeAsync(callable, Executors.newCachedThreadPool(), callback);
    }

    public static <R> void executeAsync(Callable<R> callable, Interfaces.Callback<R> callback) {
        final int cores = Runtime.getRuntime().availableProcessors();

        executeAsync(callable, new ThreadPoolExecutor(cores, cores, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>()), callback);
    }

    public static <R> void executeAsync(Callable<R> callable, Executor executor, Interfaces.Callback<R> callback) {

        final Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                final R result = callable.call();
                handler.post(() -> callback.onComplete(result));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public static void showToast(final Context ctx, final String message) {
        showToast(ctx, message, Toast.LENGTH_SHORT);
    }

    public static void showToast(final Context ctx, final String message, final int length)
    {
        if (ctx instanceof Activity && ((Activity)ctx).isFinishing()) return;

        try {
            final Toast toast = Toast.makeText(ctx, message, length);

            /*
            final View view = toast.getView();
            final TextView text = view.findViewById(android.R.id.message);

            text.setBackgroundColor(ctx.getColor(ctx.getResources().getIdentifier("wc_toast_bg", "color", ctx.getPackageName())));
            text.setTextColor(ctx.getColor(ctx.getResources().getIdentifier("wc_toast_text", "color", ctx.getPackageName())));
             */
            toast.show();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public static void showSnackbar(final View view, final String message) {
        showSnackbar(view, message, Snackbar.LENGTH_LONG);
    }

    public static void showSnackbar(final View view, final String message, final int length) {
        Snackbar.make(view, message, length).show();
    }

    public static void cancelNotification(final Context ctx, final int id)
    {
        ((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id);
    }

    public static Boolean isNetworkAvailable(final Context ctx) {
        return isNetworkAvailable(ctx, false);
    }

    public static Boolean isNetworkAvailable(final Context ctx, final Boolean excludeBluetooth) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        final Network network = connectivityManager.getActiveNetwork();
        final NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);

        if (excludeBluetooth)
            return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        else
            return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
    }

    public static Network getActiveNetwork(final Context ctx) {
        final ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetwork();
    }

    public static String truncateWords(final String s, final int n) {
        if (s == null) return null;
        if (n <= 0) return "";

        final Pattern WB_PATTERN = Pattern.compile("(?<=\\w)\\b");
        final Matcher m = WB_PATTERN.matcher(s);
        for (int i=0; i<n && m.find(); i++);
        if (m.hitEnd())
            return s;
        else
            return s.substring(0, m.end()).concat("...");
    }

    public static String truncateLength(final String s, final int n) {
        if (s.length() > n)
            return s.substring(0, n).concat("...");

        return s;
    }


    public static URL getRedirectUrl(final String url) {
        URL output = null;
        HttpURLConnection ucon = null;
        try {
            final URL url2 = new URL(url);
            ucon = (HttpURLConnection) url2.openConnection();
            ucon.setInstanceFollowRedirects(false);
            final URL secondURL = new URL(ucon.getHeaderField("Location"));
            output = secondURL;
            ucon.disconnect();
        } catch (Exception ex) {
            if (ucon != null)
                ucon.disconnect();
            ex.printStackTrace();
        }

        return output;
    }

    public static Boolean inDebugMode(final Context ctx)
    {
        return ((0 != (ctx.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)));
    }

   public static void writeToFile(final Context ctx, String data) {
        if (!inDebugMode(ctx)) return;
        try {
            final FileOutputStream fOut = ctx.openFileOutput("log.txt", MODE_APPEND);

            final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fOut);
            //final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(new File(Environment.getExternalStorageDirectory(), fileName)));
            outputStreamWriter.append("\n");

            if (!data.contains("\n"))
                outputStreamWriter.append(DateUtils.FormatDate(new Date(), "M/d/Y h:m:s a")).append(": ").append(data);
            else
                outputStreamWriter.append(data);

            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    /*
    * Returns true/false if there is an active download
    */
    public static boolean isCurrentDownload(final Context ctx) {
        final DownloadManager.Query q = new DownloadManager.Query();
        q.setFilterByStatus(DownloadManager.STATUS_RUNNING | DownloadManager.STATUS_PENDING | DownloadManager.STATUS_PAUSED);

        final DownloadManager m = (DownloadManager) ctx.getSystemService(DOWNLOAD_SERVICE);

        return m != null && m.query(q).moveToFirst();
    }

    public static boolean isCurrentDownload(final Context ctx, final int downloadId) {
        final DownloadManager.Query q = new DownloadManager.Query();
        q.setFilterById(downloadId);

        final DownloadManager m = (DownloadManager) ctx.getSystemService(DOWNLOAD_SERVICE);

        final Cursor cursor = m.query(q);

        if (cursor.moveToFirst()) {
            final int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));

            if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING)
                return true;
        }

        return false;
    }

    public static boolean isFinishedDownload(final Context ctx, final int downloadId) {
        final DownloadManager.Query q = new DownloadManager.Query();
        q.setFilterById(downloadId);

        final DownloadManager m = (DownloadManager) ctx.getSystemService(DOWNLOAD_SERVICE);

        final Cursor cursor = m.query(q);

        if (cursor.moveToFirst()) {
            final int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));

            if (status == DownloadManager.STATUS_SUCCESSFUL)
                return true;
        }

        return false;
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
                final URL redirectUrl = getRedirectUrl(url);

                if (redirectUrl != null)
                    return getRemoteStream(redirectUrl.toString());
            }

            if (statusCode == 200)
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

    public static void DeviceSync(final Context ctx, final PutDataMapRequest dataMap, final String message, final int toastLength) {

        CommonUtils.executeCachedAsync(new WatchConnected(ctx), (connected) -> {
            dataMap.getDataMap().putLong("time", new Date().getTime());
            final PutDataRequest request = dataMap.asPutDataRequest();
            request.setUrgent();

            final Task<DataItem> task = Wearable.getDataClient(ctx).putDataItem(request);

            task.addOnSuccessListener(dataItem -> {
                if (message != null)
                    showToast(ctx, message, toastLength);
            });

            task.addOnFailureListener(e -> showToast(ctx, message, Toast.LENGTH_LONG));
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
        if (str == null || str.length() == 0) return "";

        return str.replaceAll("[ ':,.–!&)(|{}_-]","");
    }

    public static SpannableString boldText(final String text)
    {
        final SpannableString titleText = new SpannableString(text);
        titleText.setSpan(new StyleSpan(Typeface.BOLD), 0, titleText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return titleText;
    }

    public static String stripHTML(String str)
    {
        if (str == null) return "";
        //str = str.replace(" ","");
        return HtmlCompat.fromHtml(str, HtmlCompat.FROM_HTML_MODE_LEGACY).toString();

        //str = str.replaceAll("[ ':.–!&|{}_-]","");
        //turn str;
    }

    public static String CleanDescription(String str)
    {
        //str = stripHTML(str);
        //str = str.replaceAll("\\<[^>]*>","");
        //str = str.replace("&#8217;","'");
        //str = str.replace("[&#8230;]","");
        //str = str.replace("&quot;","\"");
        //str = str.replace("&#039;","'");
        //str = str.replace("&nbsp;"," ");
        //str = str.replace("&amp;","&");
        str = stripHTML(str);
        str = str.replace("￼","");
        //return str;
        return str;
    }

    public static boolean IsDark(final int color)
    {
        return ColorUtils.calculateLuminance(color) < 0.3;
    }

    public static Pair<Integer, Integer> GetBackgroundColor(final Context ctx, final PodcastItem podcast) {
        final Bitmap bitmap = BitmapFactory.decodeFile(CommonUtils.GetPodcastsThumbnailDirectory(ctx).concat(CommonUtils.GetThumbnailName(podcast.getPodcastId())));

        if (bitmap == null)
            return new Pair<>(0, -3355444);

        final Palette palette = Palette.from(bitmap).generate();

        Pair<Integer, Integer> p = null;

        if (palette.getDarkMutedSwatch() != null)
            p = new Pair<>(palette.getDarkMutedSwatch().getRgb(), palette.getDarkMutedSwatch().getBodyTextColor());

        if (palette.getMutedSwatch() != null)
            p = new Pair<>(palette.getMutedSwatch().getRgb(), palette.getMutedSwatch().getBodyTextColor());

        if (palette.getLightMutedSwatch() != null)
            p = new Pair<>(palette.getLightMutedSwatch().getRgb(), palette.getLightMutedSwatch().getBodyTextColor());

        if (palette.getLightVibrantSwatch() != null)
            p = new Pair<>(palette.getLightVibrantSwatch().getRgb(), palette.getLightVibrantSwatch().getBodyTextColor());

        if (palette.getDarkVibrantSwatch() != null)
            p = new Pair<>(palette.getDarkVibrantSwatch().getRgb(), palette.getDarkVibrantSwatch().getBodyTextColor());

        return p;
    }

    public static Drawable GetBackgroundLogo(final Context ctx, final PodcastItem episode) {
        return GetBackgroundLogo(ctx, episode, R.drawable.ic_logo_placeholder);
    }

    public static Drawable GetBackgroundLogo(final Context ctx, final PodcastItem episode, final int defaultResource) {
        Bitmap bitmap;

        if (episode.getChannel() != null && episode.getThumbnailUrl() != null)
            bitmap = BitmapFactory.decodeFile(GetEpisodesThumbnailDirectory(ctx).concat(CommonUtils.GetThumbnailName(episode.getEpisodeId())));
        else if (episode.getChannel() != null && episode.getChannel().getThumbnailUrl() != null)
            bitmap = BitmapFactory.decodeFile(GetPodcastsThumbnailDirectory(ctx).concat(CommonUtils.GetThumbnailName(episode.getPodcastId())));
        else
            bitmap = BitmapFactory.decodeResource(ctx.getResources(), defaultResource);

        if (bitmap == null)
            bitmap = BitmapFactory.decodeResource(ctx.getResources(), defaultResource);

        bitmap = blur(ctx, bitmap);

        final Canvas canvas = new Canvas(bitmap);

        final Paint p = new Paint(Color.RED);
        p.setColorFilter(new LightingColorFilter(0x5f606060, 0x00000000));

        canvas.drawBitmap(bitmap, new Matrix(), p);

        return new BitmapDrawable(ctx.getResources(), bitmap);
    }

    public static RoundedBitmapDrawable GetRoundedPlaceholderLogo(final Context ctx) {
        return GetRoundedLogo(ctx, null, R.drawable.ic_logo_placeholder);
    }

    public static RoundedBitmapDrawable GetRoundedLogo(final Context ctx, final String file) {
        return GetRoundedLogo(ctx, file, R.drawable.ic_logo_placeholder);
    }

    public static RoundedBitmapDrawable GetRoundedLogo(final Context ctx, final String file, int defaultResource) {

        Bitmap bitmap;
        int borderWidthHalfImage = 5, borderWidth = 3;

        if (file != null && new File(file).exists())
            bitmap = BitmapFactory.decodeFile(file);
        else {
            bitmap = BitmapFactory.decodeResource(ctx.getResources(), defaultResource);
            borderWidthHalfImage = 6;
            borderWidth = 5;
        }

        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(ctx.getResources(), defaultResource);
            borderWidthHalfImage = 6;
            borderWidth = 5;
        }


        final int bitmapWidthImage = bitmap.getWidth();
        final int bitmapHeightImage = bitmap.getHeight();

        final int bitmapRadiusImage = Math.min(bitmapWidthImage,bitmapHeightImage);
        final int bitmapSquareWidthImage = Math.min(bitmapWidthImage,bitmapHeightImage);
        final int newBitmapSquareWidthImage = bitmapSquareWidthImage+borderWidthHalfImage;

        final Bitmap roundedImageBitmap = Bitmap.createBitmap(newBitmapSquareWidthImage,newBitmapSquareWidthImage,Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(roundedImageBitmap);

        final int i = borderWidthHalfImage + bitmapSquareWidthImage - bitmapWidthImage;
        final int j = borderWidthHalfImage + bitmapSquareWidthImage - bitmapHeightImage;

        canvas.drawBitmap(bitmap, i, j, null);

        final Paint borderImagePaint = new Paint();
        borderImagePaint.setStyle(Paint.Style.STROKE);
        borderImagePaint.setStrokeWidth(borderWidthHalfImage * borderWidth);
        borderImagePaint.setColor(ContextCompat.getColor(ctx, R.color.wc_logo_border));
        canvas.drawCircle(canvas.getWidth() >> 1, canvas.getWidth() >> 1, newBitmapSquareWidthImage >> 1, borderImagePaint);

        final RoundedBitmapDrawable roundedImageBitmapDrawable = RoundedBitmapDrawableFactory.create(ctx.getResources(),roundedImageBitmap);
        roundedImageBitmapDrawable.setCornerRadius(bitmapRadiusImage);
        roundedImageBitmapDrawable.setAntiAlias(true);

        return roundedImageBitmapDrawable;
    }

    public static Bitmap blur(Context context, Bitmap image) {
        final float BITMAP_SCALE = 0.9f;
        final float BLUR_RADIUS = 7.5f;

        int width = Math.round(image.getWidth() * BITMAP_SCALE);
        int height = Math.round(image.getHeight() * BITMAP_SCALE);

        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
        theIntrinsic.setRadius(BLUR_RADIUS);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        return outputBitmap;
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
        if (podcast == null || podcast.getChannel() == null || podcast.getChannel().getThumbnailUrl() == null)
            return BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_thumb_default);
        else
            return BitmapFactory.decodeFile(GetPodcastsThumbnailDirectory(ctx).concat(CommonUtils.GetThumbnailName(podcast.getPodcastId())));
    }

    public static String GetPodcastsThumbnailDirectory(final Context ctx) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
            return getExternalStorageDirectory() + "/WearCasts/Images/Thumbnails/Podcasts/"; //legacy
        else
            return ctx.getExternalFilesDir(Environment.DIRECTORY_PODCASTS) + ctx.getString(R.string.directory_thumbnails_podcasts);
    }

    public static String GetEpisodesThumbnailDirectory(final Context ctx) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
            return getExternalStorageDirectory() + "/WearCasts/Images/Thumbnails/Episodes/"; //legacy
        else
            return ctx.getExternalFilesDir(Environment.DIRECTORY_PODCASTS) + ctx.getString(R.string.directory_thumbnails_episodes);
    }

    public static String GetLocalDirectory(final Context ctx) {
        return getExternalStorageDirectory() + ctx.getString(R.string.directory_local);
    }

    public static String GetMediaDirectory(final Context ctx) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
            return getExternalStorageDirectory() + ctx.getString(R.string.directory_episodes);
        else
            return ctx.getExternalFilesDir(Environment.DIRECTORY_PODCASTS) + ctx.getString(R.string.directory_episodes);
    }

    public static String GetThumbnailName(final int id)
    {
        return id + ".png";
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
