package com.krisdb.wearcasts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class PodcastEpisodeListActivity extends BaseFragmentActivity implements MenuItem.OnMenuItemClickListener {

    private WearableActionDrawerView mWearableActionDrawer;
    private Activity mActivity;
    private int mPodcastId, mPlaylistId;
    private static int SEARCH_RESULTS_CODE = 131;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.podcast_episode_list_activity);
        mActivity = this;
        mPodcastId = getIntent().getExtras().getInt("podcastId");
        mPlaylistId = getIntent().getExtras().getInt("playlistId");

        final Fragment fragment = new PodcastEpisodesListFragment().newInstance(getResources().getInteger(R.integer.playlist_default), mPodcastId, null);

        getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).commit();

        mWearableActionDrawer = findViewById(R.id.drawer_action_episode_list);
        mWearableActionDrawer.setOnMenuItemClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            if (requestCode == SEARCH_RESULTS_CODE) {

                final String query = data.getData().toString();
                final Fragment fragment = new PodcastEpisodesListFragment().newInstance(getResources().getInteger(R.integer.playlist_default), mPodcastId, query);

                getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).commitAllowingStateLoss();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        final int itemId = menuItem.getItemId();

        switch (itemId) {
            case R.id.menu_drawer_episode_list_search:
                startActivityForResult(new Intent(this, SearchEpisodesActivity.class), SEARCH_RESULTS_CODE);
                break;
            case R.id.menu_drawer_episode_list_markplayed:
                final AlertDialog.Builder alertRead = new AlertDialog.Builder(PodcastEpisodeListActivity.this);
                alertRead.setMessage(getString(R.string.confirm_mark_all_played));
                alertRead.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final ContentValues cv = new ContentValues();
                        cv.put("finished", 1);
                        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                        db.updateAll(cv, mPodcastId);
                        db.close();

                        final Fragment fragment = new PodcastEpisodesListFragment().newInstance(mPlaylistId, mPodcastId, null);
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
                break;

            case R.id.menu_drawer_episode_list_markunplayed:
                final AlertDialog.Builder alertUnread = new AlertDialog.Builder(PodcastEpisodeListActivity.this);
                alertUnread.setMessage(getString(R.string.confirm_mark_all_unplayed));
                alertUnread.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final ContentValues cv = new ContentValues();
                        cv.put("finished", 0);
                        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                        db.updateAll(cv, mPodcastId);
                        db.close();

                        final Fragment fragment = new PodcastEpisodesListFragment().newInstance(mPlaylistId, mPodcastId, null);
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
                break;
            case R.id.menu_drawer_episode_list_add_playlist:
                final View playlistAddView = getLayoutInflater().inflate(R.layout.episodes_add_playlist, null);

                final List<PlaylistItem> playlistItems = DBUtilities.getPlaylists(mActivity);
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

                            List<PodcastItem> episodes = DBUtilities.GetEpisodesFiltered(mActivity, mPodcastId);
                            episodes = episodes.subList(1, episodes.size());

                            final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                            db.addToPlaylist(playlist.getID(), episodes);
                            db.close();

                            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

                            if (prefs.getBoolean("pref_hide_empty_playlists", false) && DBUtilities.playlistIsEmpty(mActivity, playlist.getID()))
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

                final AlertDialog.Builder builder = new AlertDialog.Builder(PodcastEpisodeListActivity.this);
                builder.setView(playlistAddView);
                builder.create().show();
            break;
        }

        mWearableActionDrawer.getController().closeDrawer();

        return true;
    }
}
