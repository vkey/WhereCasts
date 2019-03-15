package com.krisdb.wearcasts.Fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.krisdb.wearcasts.Adapters.EpisodesAdapter;
import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.Controllers.EpisodesSwipeController;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.CacheUtils;
import com.krisdb.wearcasts.Utilities.ScrollingLayoutEpisodes;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.HasNewEpisodes;
import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcast;

public class EpisodesListFragment extends Fragment {

    private WearableRecyclerView mEpisodeList;
    private int mPodcastId, mTextColor;
    private Activity mActivity;
    private TextView mStatus, mProgressPlaylistText;
    private ImageView mProgressThumb;
    private LinearLayout mProgressPlaylistLayout;
    private RelativeLayout mEpisodeListLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private EpisodesAdapter mAdapter;
    private String mQuery;
    private WeakReference<Activity> mActivityRef;

    public static EpisodesListFragment newInstance(final int podcastId, final String query) {

        final EpisodesListFragment elf = new EpisodesListFragment();

        final Bundle bundle = new Bundle();
        bundle.putInt("podcastId", podcastId);
        bundle.putString("query", query);
        elf.setArguments(bundle);

        return elf;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mActivityRef = new WeakReference<>(mActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        final View listView = inflater.inflate(R.layout.fragment_podcast_episodes, container, false);
        mEpisodeListLayout = listView.findViewById(R.id.episodes_list_layout);
        mEpisodeList = listView.findViewById(R.id.episode_list);
        mProgressPlaylistLayout = listView.findViewById(R.id.episode_list_progress_text_playlist_layout);
        mProgressPlaylistText = listView.findViewById(R.id.episode_list_progress_text_playlist);
        mStatus = listView.findViewById(R.id.episode_list_status);
        mProgressThumb = listView.findViewById(R.id.episode_list_progress_text_thumbnail);
        mSwipeRefreshLayout = listView.findViewById(R.id.episode_list_swipe_layout);
        mEpisodeList.setEdgeItemsCenteringEnabled(false);
        mEpisodeList.setLayoutManager(new WearableLinearLayoutManager(mActivity, new ScrollingLayoutEpisodes()));

        if (getArguments() != null) {
            mPodcastId = getArguments().getInt("podcastId");
            mQuery = getArguments().getString("query");
        }

        /*
        mEpisodeList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    final Intent intentPlaylist = new Intent();
                    intentPlaylist.setAction("fragment");
                    intentPlaylist.putExtra("hide_paging_indicator", true);
                    LocalBroadcastManager.getInstance(mActivity).sendBroadcast(intentPlaylist);
                }
            }
        });
        */

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (mQuery != null)
                {
                    mQuery = null;
                    RefreshContent();
                    mSwipeRefreshLayout.setRefreshing(false);
                }
                else if (CommonUtils.getActiveNetwork(mActivity) == null)
                {
                    if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                        final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                        alert.setMessage(getString(R.string.alert_episode_network_notfound));
                        alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivityForResult(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"), 1);
                                dialog.dismiss();
                            }
                        });

                        alert.setNegativeButton(getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
                    }
                }
                else if (CommonUtils.HighBandwidthNetwork(mActivity) == false)
                {
                    if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                        final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                        alert.setMessage(getString(R.string.alert_episode_network_no_high_bandwidth));
                        alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivityForResult(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"), 1);
                                dialog.dismiss();
                            }
                        });

                        alert.setNegativeButton(getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
                    }
                    mSwipeRefreshLayout.setRefreshing(false);
                } else {
                    mStatus.setVisibility(View.GONE);
                    mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    new AsyncTasks.SyncPodcasts(mActivity, mPodcastId, true,
                            new Interfaces.BackgroundSyncResponse() {
                                @Override
                                public void processFinish(final int count, final int downloads) {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                    RefreshContent();
                                    mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                }
                            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });

        mSwipeRefreshLayout.setEnabled(true);

        mTextColor = ContextCompat.getColor(mActivity, R.color.wc_white);

        RefreshContent();

        return listView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                mSwipeRefreshLayout.setRefreshing(true);
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                new AsyncTasks.SyncPodcasts(mActivity, mPodcastId, true,
                        new Interfaces.BackgroundSyncResponse() {
                            @Override
                            public void processFinish(final int count, final int downloads) {
                                mSwipeRefreshLayout.setRefreshing(false);
                                RefreshContent();
                                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    public void RefreshContent() {
        if (!isAdded()) return;

        final Resources resources = mActivity.getResources();
        final String densityName = CommonUtils.getDensityName(mActivity);
        final Boolean isRound = resources.getConfiguration().isScreenRound();
        final int themeId = Utilities.getThemeOptionId(mActivity);

        if (themeId == Enums.ThemeOptions.DYNAMIC.getThemeId()) {
            final Pair<Integer, Integer> colors = CommonUtils.GetBackgroundColor(GetPodcast(mActivity, mPodcastId));
            mEpisodeListLayout.setBackgroundColor(colors.first);
            mTextColor = colors.second;
            mProgressPlaylistText.setTextColor(mTextColor);
        }

        mStatus.setTextColor(mTextColor);

        final PodcastItem podcast = GetPodcast(mActivity, mPodcastId);

        mProgressThumb.setImageDrawable(CommonUtils.GetRoundedLogo(mActivity, podcast.getChannel()));
        mProgressThumb.setMaxWidth(Utilities.getThumbMaxWidth(mActivity, densityName, isRound));

        final ViewGroup.MarginLayoutParams paramsLayout = (ViewGroup.MarginLayoutParams) mProgressPlaylistLayout.getLayoutParams();

        if (Objects.equals(densityName, getString(R.string.hdpi))) {
            if (isRound) {
                mProgressPlaylistLayout.setPadding(0, 8, 0, 8);
                paramsLayout.setMargins(0, 0, 0, 40);
            } else {
                mProgressPlaylistLayout.setPadding(0, 7, 0, 7);
                paramsLayout.setMargins(0, 0, 0, 20);
            }
        } else if (Objects.equals(densityName, getString(R.string.xhdpi))) {
            mProgressPlaylistLayout.setPadding(0, 8, 0, 10);
            paramsLayout.setMargins(0, 0, 0, 0);
        } else {
            mProgressPlaylistLayout.setPadding(0, 10, 0, 10);
            paramsLayout.setMargins(0, 0, 0, 0);
        }

        mProgressThumb.setVisibility(View.VISIBLE);
        mStatus.setVisibility(View.VISIBLE);
        mStatus.setText(mActivity.getString(R.string.text_loading_episodes));

        mEpisodeList.setVisibility(View.INVISIBLE);

        new AsyncTasks.DisplayEpisodes(mActivity, mPodcastId, mQuery,
                new Interfaces.PodcastsResponse() {
                    @Override
                    public void processFinish(final List<PodcastItem> episodes) {

                        if (!isAdded()) return;

                        mAdapter = new EpisodesAdapter(mActivity, episodes, mTextColor, mSwipeRefreshLayout, null);
                        mEpisodeList.setAdapter(mAdapter);

                        final ItemTouchHelper itemTouchhelper = new ItemTouchHelper(new EpisodesSwipeController(mActivity, mAdapter, episodes));
                        itemTouchhelper.attachToRecyclerView(mEpisodeList);

                        if (episodes != null && episodes.size() == 1) {
                            mStatus.setText(mQuery != null ? mActivity.getString(R.string.empty_episode_list_search_results) : mActivity.getString(R.string.empty_episode_list));
                            mStatus.setVisibility(View.VISIBLE);
                        } else {
                            if (HasNewEpisodes(mActivity, mPodcastId)) {
                                final ContentValues cv = new ContentValues();
                                cv.put("new", 0);
                                final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                                db.updateAll(cv, mPodcastId);
                                db.close();
                                CacheUtils.deletePodcastsCache(mActivity);
                            }

                            mStatus.setVisibility(TextView.GONE);
                        }

                        mProgressThumb.setVisibility(View.GONE);

                        mEpisodeList.setVisibility(View.VISIBLE);
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}