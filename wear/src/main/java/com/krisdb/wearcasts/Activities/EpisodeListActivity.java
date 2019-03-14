package com.krisdb.wearcasts.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.krisdb.wearcasts.Adapters.PlaylistsAssignAdapter;
import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Fragments.EpisodesListFragment;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.EpisodeUtilities;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodesFiltered;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.playlistIsEmpty;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class EpisodeListActivity extends BaseFragmentActivity implements MenuItem.OnMenuItemClickListener, Interfaces.OnEpisodeSelectedListener {

    private WearableActionDrawerView mWearableActionDrawer;
    private Activity mActivity;
    private int mPodcastId;
    private String mQuery;
    private static int SEARCH_RESULTS_CODE = 131;
    private static WeakReference<EpisodeListActivity> mActivityRef;
    private List<PodcastItem> mSelectedEpisodes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.podcast_episode_list_activity);
        mActivity = this;
        mActivityRef = new WeakReference<>(this);

        mPodcastId = getIntent().getExtras().getInt("podcastId");

        final Fragment fragment = new EpisodesListFragment().newInstance(mPodcastId, null);

        getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).commit();

        mWearableActionDrawer = findViewById(R.id.drawer_action_episode_list);
        mWearableActionDrawer.setOnMenuItemClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            if (requestCode == SEARCH_RESULTS_CODE) {

                final String query = data.getData().toString();
                final Fragment fragment = new EpisodesListFragment().newInstance(mPodcastId, query);

                getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).commitAllowingStateLoss();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        final int itemId = menuItem.getItemId();
        final Fragment fragment = new EpisodesListFragment().newInstance(mPodcastId, mQuery);

        switch (itemId) {
            case R.id.menu_drawer_episode_list_selected_markplayed:
                final DBPodcastsEpisodes dbMarkPlayed = new DBPodcastsEpisodes(mActivity);
                dbMarkPlayed.updateEpisodes(mSelectedEpisodes, "finished", 1);
                dbMarkPlayed.close();
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).commit();
                break;
            case R.id.menu_drawer_episode_list_selected_markplayed_single:
                final List<PodcastItem> episodes = EpisodeUtilities.GetEpisodesAfter(mActivity, mSelectedEpisodes.get(0));
                final DBPodcastsEpisodes dbMarkPlayedSingle = new DBPodcastsEpisodes(mActivity);
                dbMarkPlayedSingle.updateEpisodes(episodes, "finished", 1);
                dbMarkPlayedSingle.close();
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).commit();
                break;
            case R.id.menu_drawer_episode_list_selected_markunplayed:
                final DBPodcastsEpisodes dbMarkUnplayed = new DBPodcastsEpisodes(mActivity);
                dbMarkUnplayed.updateEpisodes(mSelectedEpisodes, "finished", 0);
                dbMarkUnplayed.close();
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).commit();
                break;
            case R.id.menu_drawer_episode_list_selected_downloaad:
                new AsyncTasks.DownloadMultipleEpisodes(mActivity, mSelectedEpisodes,
                        new Interfaces.AsyncResponse() {
                            @Override
                            public void processFinish() {}
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).commit();
                break;
            case R.id.menu_drawer_episode_list_selected_add_playlist:
                final Intent intent = new Intent(mActivity, EpisodeContextActivity.class);
                final Bundle bundle = new Bundle();
                final ArrayList<Integer> ids = new ArrayList<>();
                for (final PodcastItem episode : mSelectedEpisodes)
                    ids.add(episode.getEpisodeId());
                bundle.putIntegerArrayList("episodeids", ids);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.menu_drawer_episode_list_search:
                startActivityForResult(new Intent(this, SearchEpisodesActivity.class), SEARCH_RESULTS_CODE);
                break;
            case R.id.menu_drawer_episode_list_markplayed:
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
                            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).commit();
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

            case R.id.menu_drawer_episode_list_markunplayed:
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
                            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).commit();
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
            case R.id.menu_drawer_episode_list_add_playlist:
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
        }

        mWearableActionDrawer.getController().closeDrawer();

        return true;
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof EpisodesListFragment) {
            EpisodesListFragment headlinesFragment = (EpisodesListFragment) fragment;
            headlinesFragment.setOnEpisodeSelectedListener(this);
        }
    }

    @Override
    public void onEpisodeSelected(final List<PodcastItem> episodes, final String query) {

        mSelectedEpisodes = episodes;
        mQuery = query;
        final Menu menu = mWearableActionDrawer.getMenu();
        menu.clear();

        int menuId = R.menu.menu_drawer_episode_list;

        if (episodes.size() > 0) {
            if (episodes.size() == 1)
                menuId = R.menu.menu_drawer_episode_list_selected_single;
            else
                menuId = R.menu.menu_drawer_episode_list_selected;
        }

        getMenuInflater().inflate(menuId, menu);
        mSelectedEpisodes = episodes;
    }

}
