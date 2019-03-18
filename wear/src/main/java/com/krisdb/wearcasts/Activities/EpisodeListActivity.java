package com.krisdb.wearcasts.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.krisdb.wearcasts.Adapters.EpisodesAdapter;
import com.krisdb.wearcasts.Adapters.PlaylistsAssignAdapter;
import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.Controllers.EpisodesSwipeController;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.CacheUtils;
import com.krisdb.wearcasts.Utilities.EpisodeUtilities;
import com.krisdb.wearcasts.Utilities.ScrollingLayoutEpisodes;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;
import androidx.wear.widget.drawer.WearableActionDrawerView;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodesFiltered;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.HasNewEpisodes;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.playlistIsEmpty;
import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcast;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class EpisodeListActivity extends BaseFragmentActivity implements MenuItem.OnMenuItemClickListener{

    private WearableActionDrawerView mWearableActionDrawer;
    private Activity mActivity;
    private int mPodcastId;
    private String mQuery;
    private static int SEARCH_RESULTS_CODE = 131;
    private static WeakReference<EpisodeListActivity> mActivityRef;
    private WearableRecyclerView mEpisodeList;
    private int mTextColor;
    private TextView mStatus, mProgressPlaylistText;
    private ImageView mProgressThumb;
    private LinearLayout mProgressPlaylistLayout;
    private RelativeLayout mEpisodeListLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private EpisodesAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.podcast_episode_list_activity);
        mActivity = this;
        mActivityRef = new WeakReference<>(this);

        mPodcastId = getIntent().getExtras().getInt("podcastId");
        mQuery = getIntent().getExtras().getString("query");

        mWearableActionDrawer = findViewById(R.id.drawer_action_episode_list);
        mWearableActionDrawer.setOnMenuItemClickListener(this);
        mEpisodeListLayout = findViewById(R.id.episodes_list_layout);
        mEpisodeList = findViewById(R.id.episode_list);
        mProgressPlaylistLayout = findViewById(R.id.episode_list_progress_text_playlist_layout);
        mProgressPlaylistText = findViewById(R.id.episode_list_progress_text_playlist);
        mStatus = findViewById(R.id.episode_list_status);
        mProgressThumb = findViewById(R.id.episode_list_progress_text_thumbnail);
        mSwipeRefreshLayout = findViewById(R.id.episode_list_swipe_layout);
        mEpisodeList.setEdgeItemsCenteringEnabled(false);
        mEpisodeList.setLayoutManager(new WearableLinearLayoutManager(mActivity, new ScrollingLayoutEpisodes()));

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
    }

    public void RefreshContent() {

        final Resources resources = mActivity.getResources();
        final String densityName = CommonUtils.getDensityName(mActivity);
        final Boolean isRound = resources.getConfiguration().isScreenRound();
        final int themeId = Utilities.getThemeOptionId(mActivity);

        if (themeId == Enums.ThemeOptions.DYNAMIC.getThemeId()) {
            final Pair<Integer, Integer> colors = CommonUtils.GetBackgroundColor(mActivity, GetPodcast(mActivity, mPodcastId));
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
        mSwipeRefreshLayout.setEnabled(true);
        mEpisodeList.setVisibility(View.INVISIBLE);

        new AsyncTasks.DisplayEpisodes(mActivity, mPodcastId, mQuery,
                new Interfaces.PodcastsResponse() {
                    @Override
                    public void processFinish(final List<PodcastItem> episodes) {

                        mAdapter = new EpisodesAdapter(mActivity, episodes, mTextColor, mSwipeRefreshLayout, mWearableActionDrawer);
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
            else if (requestCode == SEARCH_RESULTS_CODE) {

                mQuery = data.getData().toString();
                RefreshContent();
            }
        }
    }

    private void resetMenu()
    {
        final Menu menu = mWearableActionDrawer.getMenu();
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_drawer_episode_list, menu);
        mSwipeRefreshLayout.setEnabled(true);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        final int itemId = menuItem.getItemId();
        final List<PodcastItem> episodes = mAdapter.mEpisodes;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        switch (itemId) {
            case R.id.menu_drawer_episode_list_selected_markplayed: //Mark selected played
                final DBPodcastsEpisodes dbMarkPlayed = new DBPodcastsEpisodes(mActivity);
                dbMarkPlayed.updateEpisodes(mAdapter.mSelectedEpisodes, "finished", 1);
                dbMarkPlayed.close();
                if (prefs.getBoolean("pref_" + mPodcastId + "_hide_played", false))
                {
                    RefreshContent();
                }
                else {
                    for (final Integer position : mAdapter.mSelectedPositions) {
                        episodes.get(position).setFinished(true);
                        episodes.get(position).setIsSelected(false);
                        mAdapter.notifyItemChanged(position);
                    }
                    mAdapter.mSelectedPositions = new ArrayList<>();
                    mAdapter.mSelectedEpisodes = new ArrayList<>();
                }
                resetMenu();
                break;
            case R.id.menu_drawer_episode_list_selected_markunplayed: //Mark selected unplayed
                final DBPodcastsEpisodes dbMarkUnplayed = new DBPodcastsEpisodes(mActivity);
                dbMarkUnplayed.updateEpisodes(mAdapter.mSelectedEpisodes, "finished", 0);
                dbMarkUnplayed.close();
                for (final Integer position : mAdapter.mSelectedPositions) {
                    episodes.get(position).setFinished(false);
                    episodes.get(position).setIsSelected(false);
                    mAdapter.notifyItemChanged(position);
                }
                mAdapter.mSelectedPositions = new ArrayList<>();
                mAdapter.mSelectedEpisodes = new ArrayList<>();
                resetMenu();
                break;
            case R.id.menu_drawer_episode_list_selected_markplayed_single: //Mark selected played from here
                final List<PodcastItem> episodesAfter = EpisodeUtilities.GetEpisodesAfter(mActivity, mAdapter.mSelectedEpisodes.get(0));
                final DBPodcastsEpisodes dbMarkPlayedSingle = new DBPodcastsEpisodes(mActivity);
                dbMarkPlayedSingle.updateEpisodes(episodesAfter, "finished", 1);
                dbMarkPlayedSingle.close();
                if (prefs.getBoolean("pref_" + mPodcastId + "_hide_played", false))
                {
                    RefreshContent();
                }
                else {
                for (final PodcastItem episodeAfter : episodesAfter) {
                    for (final PodcastItem episode : episodes) {
                        if (episodeAfter.getEpisodeId() == episode.getEpisodeId()) {
                            episode.setFinished(true);
                            episode.setIsSelected(false);
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                }
                    resetMenu();
                }
                break;
            case R.id.menu_drawer_episode_list_markplayed: //Mark all played
                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    final AlertDialog.Builder alertRead = new AlertDialog.Builder(EpisodeListActivity.this);
                    alertRead.setMessage(getString(R.string.confirm_mark_all_played));
                    alertRead.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final ContentValues cv = new ContentValues();
                            cv.put("finished", 1);
                            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                            db.updateAll(cv, mPodcastId);
                            db.close();

                            if (prefs.getBoolean("pref_" + mPodcastId + "_hide_played", false))
                                RefreshContent();
                            else {
                                for (final PodcastItem episode : episodes)
                                    episode.setFinished(true);

                                mAdapter.notifyDataSetChanged();
                            }
                            resetMenu();
                        }
                    });
                    alertRead.setNegativeButton(getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    alertRead.show();
                }
                break;
            case R.id.menu_drawer_episode_list_markunplayed: //Mark all unplayed
                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    final AlertDialog.Builder alertUnread = new AlertDialog.Builder(EpisodeListActivity.this);
                    alertUnread.setMessage(getString(R.string.confirm_mark_all_unplayed));
                    alertUnread.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final ContentValues cv = new ContentValues();
                            cv.put("finished", 0);
                            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                            db.updateAll(cv, mPodcastId);
                            db.close();
                            if (prefs.getBoolean("pref_" + mPodcastId + "_hide_played", false))
                                RefreshContent();
                            else {
                                for (final PodcastItem episode : episodes)
                                    episode.setFinished(false);

                                mAdapter.notifyDataSetChanged();
                            }
                            resetMenu();
                        }
                    });
                    alertUnread.setNegativeButton(getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    alertUnread.show();
                }
                break;
            case R.id.menu_drawer_episode_list_download: //Download all
                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    final AlertDialog.Builder alertRead = new AlertDialog.Builder(EpisodeListActivity.this);
                    alertRead.setMessage(getString(R.string.confirm_download_all));
                    alertRead.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new AsyncTasks.DownloadMultipleEpisodes(mActivity, episodes.subList(1, episodes.size()),
                                    new Interfaces.AsyncResponse() {
                                        @Override
                                        public void processFinish() {
                                            for (final PodcastItem episode : episodes)
                                                episode.setIsDownloaded(true);

                                            mAdapter.notifyDataSetChanged();
                                            resetMenu();
                                        }
                                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    });
                    alertRead.setNegativeButton(getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    alertRead.show();
                }
                break;
                case R.id.menu_drawer_episode_list_selected_downloaad: //Download selected
                    new AsyncTasks.DownloadMultipleEpisodes(mActivity, mAdapter.mSelectedEpisodes,
                            new Interfaces.AsyncResponse() {
                                @Override
                                public void processFinish() {
                                    for (final Integer position : mAdapter.mSelectedPositions) {
                                        episodes.get(position).setIsDownloaded(true);
                                        episodes.get(position).setIsSelected(false);
                                        mAdapter.notifyItemChanged(position);
                                    }
                                    mAdapter.mSelectedPositions = new ArrayList<>();
                                    mAdapter.mSelectedEpisodes = new ArrayList<>();
                                    resetMenu();
                                }
                            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            case R.id.menu_drawer_episode_list_search: //Search
                startActivityForResult(new Intent(this, SearchEpisodesActivity.class), SEARCH_RESULTS_CODE);
                break;
               case R.id.menu_drawer_episode_list_add_playlist: //Add all to playlist
                final View playlistAddView = getLayoutInflater().inflate(R.layout.episodes_add_playlist, null);

                final List<PlaylistItem> playlistItems = getPlaylists(mActivity);
                final Spinner spinner = playlistAddView.findViewById(R.id.episodes_add_playlist_list);

                if (playlistItems.size() == 0) {
                    playlistAddView.findViewById(R.id.episodes_add_playlist_empty).setVisibility(View.VISIBLE);
                    playlistAddView.findViewById(R.id.episodes_add_playlist_list).setVisibility(View.GONE);
                }
                else
                {
                    final PlaylistItem playlistEmpty = new PlaylistItem();
                    playlistEmpty.setID(mActivity.getResources().getInteger(R.integer.default_playlist_select));
                    playlistEmpty.setName(getString(R.string.dropdown_playlist_select));
                    playlistItems.add(0, playlistEmpty);

                    spinner.setAdapter(new PlaylistsAssignAdapter(this, playlistItems));
                }

                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        final PlaylistItem playlist = (PlaylistItem) parent.getSelectedItem();

                        if (playlist.getID() != getResources().getInteger(R.integer.default_playlist_select)) {

                            List<PodcastItem> episodes = GetEpisodesFiltered(mActivity, mPodcastId);
                            episodes = episodes.subList(1, episodes.size());

                            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                            db.addToPlaylist(playlist.getID(), episodes);
                            db.close();

                            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

                            if (prefs.getBoolean("pref_hide_empty_playlists", false) && playlistIsEmpty(mActivity, playlist.getID()))
                            {
                                final SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("refresh_vp", true);
                                editor.apply();
                            }

                            showToast(mActivity, mActivity.getString(R.string.alert_episodes_playlist_added, episodes.size(), playlist.getName()));
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {}
                });

                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(EpisodeListActivity.this);
                    builder.setView(playlistAddView);
                    builder.create().show();
                }
                break;
            case R.id.menu_drawer_episode_list_selected_add_playlist: //Add selected to playlist
                final Intent intent = new Intent(mActivity, EpisodeContextActivity.class);
                final Bundle bundle = new Bundle();
                final ArrayList<Integer> ids = new ArrayList<>();
                for (final PodcastItem episode : mAdapter.mSelectedEpisodes)
                    ids.add(episode.getEpisodeId());
                bundle.putIntegerArrayList("episodeids", ids);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
        }

        mWearableActionDrawer.getController().closeDrawer();

        return true;
    }
}
