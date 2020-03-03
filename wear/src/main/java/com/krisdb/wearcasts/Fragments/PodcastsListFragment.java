package com.krisdb.wearcasts.Fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.krisdb.wearcasts.Adapters.PodcastsAdapter;
import com.krisdb.wearcasts.Async.GetPodcasts;
import com.krisdb.wearcasts.Async.SyncPodcasts;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.PodcastUtilities;
import com.krisdb.wearcasts.ViewModels.PodcastsViewModel;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;

import java.lang.ref.WeakReference;

import static android.app.Activity.RESULT_OK;
import static com.krisdb.wearcastslibrary.CommonUtils.GetRoundedLogo;

public class PodcastsListFragment extends Fragment {

    private WearableRecyclerView mPodcastsList;
    private Activity mActivity;
    private TextView mEmptyView;
    private PodcastsAdapter mAdapter;
    private static WeakReference<Activity> mActivityRef;
    private ImageView mLogo;
    private static int SYNC_RESULT_CODE = 111;

    public static PodcastsListFragment newInstance() {
        return new PodcastsListFragment();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mActivity = getActivity();
        mActivityRef = new WeakReference<>(mActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        final View listView = inflater.inflate(R.layout.fragment_podcast_list, container, false);

        mPodcastsList = listView.findViewById(R.id.podcast_list);
        mLogo = listView.findViewById(R.id.podcast_list_empty_logo);

        mPodcastsList.setEdgeItemsCenteringEnabled(true);
        mPodcastsList.setLayoutManager(new WearableLinearLayoutManager(mActivity));
        //final WearableLinearLayoutManager manager = new WearableLinearLayoutManager(mActivity, new com.krisdb.wearcasts.Utilities.ScrollingLayoutPodcasts());
        //mPodcastsList.setLayoutManager(manager);

        if (PreferenceManager.getDefaultSharedPreferences(mActivity).getBoolean("syncOnStart", false))
            handleNetwork();

        RefreshContent();

        return listView;
    }

   private void handleNetwork() {
       if (!CommonUtils.isNetworkAvailable(mActivity)) {
           if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
               final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
               alert.setMessage(getString(R.string.alert_episode_network_notfound));
               alert.setPositiveButton(getString(R.string.confirm_yes), (dialog, which) -> {
                   startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), SYNC_RESULT_CODE);
                   dialog.dismiss();
               });

               alert.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss()).show();
           }
       } else {
           CommonUtils.showToast(mActivity, getString(R.string.alert_sync_started));
           CommonUtils.executeSingleThreadAsync(new SyncPodcasts(mActivity, 0), (response) -> {
               RefreshContent();
               if (isAdded())
                   CommonUtils.showToast(mActivity, getString(R.string.alert_sync_finished));
           });
       }
   }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (resultCode == RESULT_OK) {
            if (requestCode == SYNC_RESULT_CODE)
            {
                CommonUtils.showToast(mActivity, getString(R.string.alert_sync_started));
                CommonUtils.executeSingleThreadAsync(new SyncPodcasts(mActivity, 0), (response) -> {
                    RefreshContent();
                    CommonUtils.showToast(mActivity, getString(R.string.alert_sync_finished));
                });

            }
        }
    }

    private void RefreshContent() {
        if (!isAdded()) return;

        final PodcastsViewModel podcastsModel = ViewModelProviders.of(this).get(PodcastsViewModel.class);

        podcastsModel.getPodcasts().observe(this, podcasts -> {
            mAdapter = new PodcastsAdapter(mActivity, podcasts);
            mPodcastsList.setAdapter(mAdapter);
            if (podcasts.size() == 0)
                showCopy(podcasts.size());
        });
   }

    private void showCopy() {
        showCopy(-1);
    }

    private void showCopy(int podcastCount)
    {
        if (!isAdded()) return;

        if (podcastCount == -1)
            podcastCount = PodcastUtilities.GetPodcastCount(mActivity);

        final ImageView swipeLeftView = mActivity.findViewById(R.id.podcast_list_swipe_left);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        if (podcastCount > 0)
        {
            if (mEmptyView != null)
                mEmptyView.setVisibility(TextView.GONE);

            if (swipeLeftView != null)
                swipeLeftView.setVisibility(View.GONE);

            if (mLogo != null)
                mLogo.setVisibility(TextView.GONE);
        }
        else {
            final int visits = prefs.getInt("visits", 0);

            String emptyText = "";
            final String lastUpdateDate = prefs.getString("last_podcast_sync_date", "");
            mEmptyView = mActivity.findViewById(R.id.empty_podcast_list);

            if (visits < 10) {
                emptyText = emptyText.concat(mActivity.getString(R.string.empty_podcast_list)).concat("\n\n").concat(mActivity.getString(R.string.empty_podcast_list2));
                if (swipeLeftView != null)
                    swipeLeftView.setVisibility(View.VISIBLE);
            }
            else if (lastUpdateDate.length() > 0) {
                emptyText = emptyText.concat(getString(R.string.last_updated)
                        .concat(":\n")
                        .concat(DateUtils.GetDisplayDate(mActivity, lastUpdateDate, "EEE MMM dd H:mm:ss Z yyyy"))
                        .concat(" @ ")
                        .concat(DateUtils.GetTime(DateUtils.ConvertDate(lastUpdateDate, "EEE MMM dd H:mm:ss Z yyyy"))));
            }
            else
                emptyText = mActivity.getString(R.string.empty_podcast_list);

            if (mEmptyView != null) {
                mEmptyView.setText(emptyText);
                mEmptyView.setVisibility(TextView.VISIBLE);
            }

            if (swipeLeftView != null)
                swipeLeftView.setVisibility(visits < 10 ? View.VISIBLE : View.GONE);

            if (mLogo != null) {
                mLogo.setImageDrawable(GetRoundedLogo(mActivity, null));
                mLogo.setVisibility(TextView.VISIBLE);
                mLogo.setOnLongClickListener(view -> {
                    CommonUtils.executeAsync(new GetPodcasts(mActivity, false), (podcasts) -> {
                        mAdapter = new PodcastsAdapter(mActivity, podcasts);
                        mPodcastsList.setAdapter(mAdapter);
                        showCopy(podcasts.size());
                    });

                    return false;
                });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isAdded()) return;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        if (mAdapter != null && prefs.getBoolean("refresh_podcast_list", false)) {
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("refresh_podcast_list", false);
            editor.apply();

            mAdapter.refreshContent();
            showCopy();
        }
    }
}
