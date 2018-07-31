package com.krisdb.wearcasts;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.ProgressBar;

import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

public class AsyncTasks {
    private static WeakReference<Context> mContext;

     public static class SendToWatch extends AsyncTask<Void, Void, Void> {

         private List<PodcastItem> mPodcasts;
         private Interfaces.PodcastsResponse mResponse;
         private static WeakReference<ProgressBar> mProgressBar;

         SendToWatch(final Context context, final List<PodcastItem> podcasts, final ProgressBar progressBar, final Interfaces.PodcastsResponse response) {
             mContext = new WeakReference<>(context);
             mProgressBar = new WeakReference<>(progressBar);
             mPodcasts = podcasts;
             mResponse = response;
         }

        @Override
        protected final Void doInBackground(Void... params) {

            final ProgressBar pb = mProgressBar.get();

            pb.setMax(mPodcasts.size());

            int count = 0;

            for (final PodcastItem podcast : mPodcasts) {
                pb.setProgress(count++);
                Utilities.SendToWatch(mContext.get(), podcast, false);
                SystemClock.sleep(3500);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            mResponse.processFinish(mPodcasts);
        }
    }
}
