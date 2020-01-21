package com.krisdb.wearcasts.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;
import androidx.wear.widget.drawer.WearableActionDrawerView;

import com.krisdb.wearcasts.Adapters.EpisodesAdapter;
import com.krisdb.wearcasts.Adapters.PlaylistsAssignAdapter;
import com.krisdb.wearcasts.Async.SyncPodcasts;
import com.krisdb.wearcasts.Controllers.EpisodesSwipeController;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.EpisodeUtilities;
import com.krisdb.wearcasts.Utilities.ScrollingLayoutEpisodes;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcasts.ViewModels.EpisodesViewModel;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Enums;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodesFiltered;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.HasNewEpisodes;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.playlistIsEmpty;
import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcast;

public class EpisodeListActivity extends BaseFragmentActivity implements MenuItem.OnMenuItemClickListener {

    private WearableActionDrawerView mWearableActionDrawer;
    private Activity mActivity;
    private int mPodcastId;
    private String mQuery;
    private static int NO_NETWORK_RESULTS_CODE = 130;
    private static int SEARCH_RESULTS_CODE = 131;
    private static int DOWNLOAD_RESULTS_CODE = 132;
    private static int LOW_BANDWIDTH_RESULTS_CODE = 133;
    private static WeakReference<EpisodeListActivity> mActivityRef;
    private WearableRecyclerView mEpisodeList;
    private int mTextColor, mItemID;
    private TextView mStatus, mProgressPlaylistText;
    private ImageView mProgressThumb;
    private LinearLayout mProgressPlaylistLayout;
    private RelativeLayout mEpisodeListLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private EpisodesAdapter mAdapter;
    private ConnectivityManager mManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private TimeOutHandler mTimeOutHandler;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(7);
    private List<PodcastItem> mEpisodes, mDownloadEpisodes;
    private AlertDialog mPlaylistDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.podcast_episode_list_activity);
        mActivity = this;
        mActivityRef = new WeakReference<>(this);

        mTimeOutHandler = new TimeOutHandler(this);
        mManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

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
        ((SimpleItemAnimator)mEpisodeList.getItemAnimator()).setSupportsChangeAnimations(false);

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
        if (mQuery != null)
        {
            mQuery = null;
            RefreshContent();
            mSwipeRefreshLayout.setRefreshing(false);
        }
        else if (!CommonUtils.isNetworkAvailable(mActivity))
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                alert.setMessage(getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(getString(R.string.confirm_yes), (dialog, which) -> {
                    startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), NO_NETWORK_RESULTS_CODE);
                    dialog.dismiss();
                });

                alert.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss()).show();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }
         else {
            mStatus.setVisibility(View.GONE);
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mSwipeRefreshLayout.setRefreshing(true);

            CommonUtils.executeSingleThreadAsync(new SyncPodcasts(mActivity, mPodcastId), (response) -> {
                mSwipeRefreshLayout.setRefreshing(false);
                if (response.getDownloadEpisodes().size() > 0)
                    downloadEpisodes(response.getDownloadEpisodes());
                else
                    runOnUiThread(this::RefreshContent);

                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }
        });

        mSwipeRefreshLayout.setEnabled(true);

        mTextColor = ContextCompat.getColor(mActivity, R.color.wc_white);

        RefreshContent();
    }

    private void downloadEpisodes(final List<PodcastItem> episodes) {
        mDownloadEpisodes = episodes;
        downloadEpisodes();
    }

    private void downloadEpisodes() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        if (prefs.getBoolean("pref_disable_bluetooth", false) && Utilities.BluetoothEnabled() && Utilities.disableBluetooth(mActivity, true)) {

            unregisterNetworkCallback();

            if (!CommonUtils.isNetworkAvailable(mActivity, true))
                CommonUtils.showToast(mActivity, getString(R.string.alert_episode_network_waiting));

            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(final Network network) {
                    mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);

                    CommonUtils.showToast(mActivity, mActivity.getString(R.string.alert_episode_download_start));

                    for (final PodcastItem episode : mDownloadEpisodes)
                        Utilities.startDownload(mActivity, episode, false);

                    runOnUiThread(() -> RefreshContent());
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
        } else {
            CommonUtils.showToast(mActivity, mActivity.getString(R.string.alert_episode_download_start));

            for (final PodcastItem episode : mDownloadEpisodes)
                Utilities.startDownload(mActivity, episode, false);

            RefreshContent();
        }
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
        final ViewGroup.MarginLayoutParams paramsThumb = (ViewGroup.MarginLayoutParams) mProgressThumb.getLayoutParams();

        if (Objects.equals(densityName, getString(R.string.hdpi))) {
            if (isRound) {
                mProgressPlaylistLayout.setPadding(0, 8, 0, 8);
                paramsLayout.setMargins(0, 0, 0, 40);
            } else {
                mProgressPlaylistLayout.setPadding(0, 7, 0, 7);
                paramsLayout.setMargins(0, 0, 0, 20);
                paramsThumb.setMargins(0, 20, 0, 0);
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

        final EpisodesViewModel model = ViewModelProviders.of(this).get(EpisodesViewModel.class);
        model.getEpisodes(mPodcastId, mQuery).observe(this, episodes -> {
            mEpisodes = episodes;
            mAdapter = new EpisodesAdapter(mActivity, episodes, mTextColor, mSwipeRefreshLayout, mWearableActionDrawer);
            mEpisodeList.setAdapter(mAdapter);

            final ItemTouchHelper itemTouchhelper = new ItemTouchHelper(new EpisodesSwipeController(mActivity, mAdapter, mQuery, episodes));
            itemTouchhelper.attachToRecyclerView(mEpisodeList);

            if (episodes.size() == 1) {
                mStatus.setText(mQuery != null ? mActivity.getString(R.string.empty_episode_list_search_results) : mActivity.getString(R.string.empty_episode_list));
                mStatus.setVisibility(View.VISIBLE);
            } else {
                if (HasNewEpisodes(mActivity, mPodcastId)) {
                    Utilities.SetPodcstRefresh(mActivity);
                    final ContentValues cv = new ContentValues();
                    cv.put("new", 0);
                    final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                    db.updateAll(cv, mPodcastId);
                    db.close();
                }

                mStatus.setVisibility(TextView.GONE);

                mProgressThumb.setVisibility(View.GONE);
                mEpisodeList.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == NO_NETWORK_RESULTS_CODE) {
                mSwipeRefreshLayout.setRefreshing(true);
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                CommonUtils.executeSingleThreadAsync(new SyncPodcasts(mActivity, mPodcastId), (response) -> {
                    mSwipeRefreshLayout.setRefreshing(false);
                    if (response.getDownloadEpisodes().size() > 0)
                        downloadEpisodes(response.getDownloadEpisodes());
                    else {
                        runOnUiThread(this::RefreshContent);
                    }
                    mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                });
            } else if (requestCode == SEARCH_RESULTS_CODE) {
                mQuery = data.getData().toString();

                final Intent intent = getIntent();
                final Bundle bundle = new Bundle();
                bundle.putString("query", mQuery);
                bundle.putInt("podcastId", mPodcastId);
                intent.putExtras(bundle);
                finish();
                startActivity(intent);

                //RefreshContent();
            } else if (requestCode == 102) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

                final int position = prefs.getInt("no_network_position", 0);

                if (position > 0) {
                    mAdapter.downloadEpisode(position, mEpisodes.get(position));

                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("no_network_position", 0);
                    editor.apply();
                }
            } else if (requestCode == DOWNLOAD_RESULTS_CODE)
                downloadEpisodes(mItemID);
            else if (requestCode == LOW_BANDWIDTH_RESULTS_CODE) {

                CommonUtils.showToast(mActivity, mActivity.getString(R.string.alert_episode_download_start));

                for (final PodcastItem episode : mDownloadEpisodes)
                    Utilities.startDownload(mActivity, episode, false);

                runOnUiThread(() -> RefreshContent());
            }
        }
    }

    private void resetMenu()
    {
        runOnUiThread(() -> {
            final Menu menu = mWearableActionDrawer.getMenu();
            menu.clear();
            getMenuInflater().inflate(R.menu.menu_drawer_episode_list, menu);
            mSwipeRefreshLayout.setEnabled(true);
        });
    }

    @Override
    public boolean onMenuItemClick(final MenuItem menuItem) {

        final int itemId = menuItem.getItemId();
        final List<PodcastItem> episodes = mAdapter.mEpisodes;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        final NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        final int autoDeleteID = Integer.valueOf(prefs.getString("pref_downloads_auto_delete", "1"));
        final boolean autoDelete = autoDeleteID == Enums.AutoDelete.PLAYED.getAutoDeleteID();
        final boolean hidePlayed = prefs.getBoolean("pref_" + mPodcastId + "_hide_played", false);

        switch (itemId) {
            case R.id.menu_drawer_episode_list_selected_markplayed: //Mark selected played
                final DBPodcastsEpisodes dbMarkPlayed = new DBPodcastsEpisodes(mActivity);
                dbMarkPlayed.updateEpisodes(mAdapter.mSelectedEpisodes, "finished", 1);
                dbMarkPlayed.close();

                for (final Integer position : mAdapter.mSelectedPositions) {
                    episodes.get(position).setFinished(true);
                    episodes.get(position).setIsSelected(false);

                    if (autoDelete) {
                        Utilities.DeleteMediaFile(mActivity, episodes.get(position));
                        episodes.get(position).setIsDownloaded(false);
                    }
                    if (hidePlayed)
                        mAdapter.notifyDataSetChanged();
                    else
                        mAdapter.notifyItemChanged(position);
                }

                mAdapter.mSelectedPositions = new ArrayList<>();
                mAdapter.mSelectedEpisodes = new ArrayList<>();
                resetMenu();
                break;
            case R.id.menu_drawer_episode_list_selected_markunplayed: //Mark selected unplayed
                final DBPodcastsEpisodes dbMarkUnplayed = new DBPodcastsEpisodes(mActivity);
                dbMarkUnplayed.updateEpisodes(mAdapter.mSelectedEpisodes, "finished", 0);
                dbMarkUnplayed.close();

                for (final Integer position : mAdapter.mSelectedPositions) {
                    episodes.get(position).setFinished(false);
                    episodes.get(position).setIsSelected(false);
                    if (autoDelete) {
                        Utilities.DeleteMediaFile(mActivity, episodes.get(position));
                        episodes.get(position).setIsDownloaded(false);
                    }
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

                for (final PodcastItem episodeAfter : episodesAfter) {
                    for (final PodcastItem episode : episodes) {
                        if (episodeAfter.getEpisodeId() == episode.getEpisodeId()) {
                            episode.setFinished(true);
                            episode.setIsSelected(false);
                            if (autoDeleteID == Enums.AutoDelete.PLAYED.getAutoDeleteID()) {
                                Utilities.DeleteMediaFile(mActivity, episode);
                                episode.setIsDownloaded(false);
                            }
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                }
                resetMenu();
                break;
            case R.id.menu_drawer_episode_list_markplayed: //Mark all played
                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    final AlertDialog.Builder alertRead = new AlertDialog.Builder(EpisodeListActivity.this);
                    alertRead.setMessage(getString(R.string.confirm_mark_all_played));
                    alertRead.setPositiveButton(getString(R.string.confirm_yes), (dialog, which) -> {
                        final ContentValues cv = new ContentValues();
                        cv.put("finished", 1);
                        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                        db.updateAll(cv, mPodcastId);
                        db.close();

                        for (final PodcastItem episode : episodes) {
                            episode.setFinished(true);
                            if (autoDelete) {
                                Utilities.DeleteMediaFile(mActivity, episode);
                                episode.setIsDownloaded(false);
                            }
                        }

                        mAdapter.notifyDataSetChanged();
                        resetMenu();
                    });
                    alertRead.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss());
                    alertRead.show();
                }
                break;
            case R.id.menu_drawer_episode_list_markunplayed: //Mark all unplayed
                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    final AlertDialog.Builder alertUnread = new AlertDialog.Builder(EpisodeListActivity.this);
                    alertUnread.setMessage(getString(R.string.confirm_mark_all_unplayed));
                    alertUnread.setPositiveButton(getString(R.string.confirm_yes), (dialog, which) -> {
                        final ContentValues cv = new ContentValues();
                        cv.put("finished", 0);
                        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                        db.updateAll(cv, mPodcastId);
                        db.close();
                        for (final PodcastItem episode : episodes)
                            episode.setFinished(false);

                        mAdapter.notifyDataSetChanged();
                        resetMenu();
                    });
                    alertUnread.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss());
                    alertUnread.show();
                }
                break;
            case R.id.menu_drawer_episode_list_download:
            case R.id.menu_drawer_episode_list_selected_downloaad:
                mItemID = itemId;
                if (!CommonUtils.isNetworkAvailable(mActivity))
                {
                    if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                        final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                        alert.setMessage(getString(R.string.alert_episode_network_notfound));
                        alert.setPositiveButton(getString(R.string.confirm_yes), (dialog, which) -> {
                            startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), DOWNLOAD_RESULTS_CODE);
                            dialog.dismiss();
                        });

                        alert.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss()).show();
                    }
                }
                else if (prefs.getBoolean("initialDownload", true) && Utilities.BluetoothEnabled()) {
                    if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                        final AlertDialog.Builder alert = new AlertDialog.Builder(EpisodeListActivity.this);
                        alert.setMessage(getString(R.string.confirm_initial_download_message));
                        alert.setPositiveButton(getString(R.string.confirm_yes), (dialog, which) -> {
                            final SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("pref_disable_bluetooth", true);
                            editor.apply();
                            onMenuItemClick(menuItem);
                            dialog.dismiss();
                        });
                        alert.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> {
                            downloadEpisodes(itemId);
                            dialog.dismiss();
                        }).show();

                        final SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("initialDownload", false);
                        editor.apply();
                    }
                }
                else if (prefs.getBoolean("pref_disable_bluetooth", false) && Utilities.BluetoothEnabled() && Utilities.disableBluetooth(mActivity)) {

                    unregisterNetworkCallback();

                    if (!CommonUtils.isNetworkAvailable(mActivity, true))
                        CommonUtils.showToast(mActivity, getString(R.string.alert_episode_network_waiting));

                    mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(final Network network) {
                            mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);
                            downloadEpisodes(itemId);
                        }
                    };

                    mManager.requestNetwork(request, mNetworkCallback);

                    mTimeOutHandler.sendMessageDelayed(
                            mTimeOutHandler.obtainMessage(MESSAGE_CONNECTIVITY_TIMEOUT),
                            NETWORK_CONNECTIVITY_TIMEOUT_MS);
                }
                else
                    downloadEpisodes(itemId);
                break;
            case R.id.menu_drawer_episode_list_search: //Search
                startActivityForResult(new Intent(this, SearchEpisodesActivity.class), SEARCH_RESULTS_CODE);
                break;
            case R.id.menu_drawer_episode_list_add_playlist: //Add all to playlist
                final View playlistAddView = getLayoutInflater().inflate(R.layout.episodes_add_playlist, null);
                final AlertDialog.Builder builder = new AlertDialog.Builder(EpisodeListActivity.this);
                builder.setView(playlistAddView);

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

                            Utilities.ShowConfirmationActivity(mActivity, mActivity.getString(R.string.alert_episodes_playlist_added, episodes.size(), playlist.getName()));
                            mPlaylistDialog.dismiss();

                            //showToast(mActivity, mActivity.getString(R.string.alert_episodes_playlist_added, episodes.size(), playlist.getName()));
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {}
                });

                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    mPlaylistDialog = builder.show();
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

    private void downloadEpisodes(final int itemId) {
        if (itemId == R.id.menu_drawer_episode_list_selected_downloaad)
            mAdapter.downloadSelectedEpisodes();
        else
            mAdapter.downloadAllEpisodes();

        resetMenu();
    }

    @Override
    public void onPause() {
        super.onPause();
        //mHandler.removeCallbacks(downloadsProgress);
    }

    private static class TimeOutHandler extends Handler {
        private final WeakReference<EpisodeListActivity> mActivityWeakReference;

        TimeOutHandler(final EpisodeListActivity activity) {
            mActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(final Message msg) {
            final EpisodeListActivity activity = mActivityWeakReference.get();

            if (activity != null) {
                switch (msg.what) {
                    case MESSAGE_CONNECTIVITY_TIMEOUT:
                        if (!activity.isFinishing()) {
                            final AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                            alert.setMessage(activity.getString(R.string.alert_episode_network_notfound));
                            alert.setPositiveButton(activity.getString(R.string.confirm_yes), (dialog, which) -> {
                                activity.startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), LOW_BANDWIDTH_RESULTS_CODE);
                                dialog.dismiss();
                            });

                            alert.setNegativeButton(activity.getString(R.string.confirm_no), (dialog, which) -> {
                                Utilities.enableBluetooth(activity);
                                dialog.dismiss();
                            }).show();
                        }
                        activity.unregisterNetworkCallback();
                        break;
                }
            }
        }
    }

    private void unregisterNetworkCallback() {
        if (mNetworkCallback != null) {
            mManager.unregisterNetworkCallback(mNetworkCallback);
            mNetworkCallback = null;
        }
    }
}
