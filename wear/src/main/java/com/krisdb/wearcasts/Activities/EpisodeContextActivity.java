package com.krisdb.wearcasts.Activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.krisdb.wearcasts.Adapters.PlaylistsAssignAdapter;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.DBUtilities;

import java.util.List;

import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.playlistIsEmpty;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class EpisodeContextActivity extends BaseFragmentActivity {

    private Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.episode_context_activity);

        mActivity = this;

        final List<Integer> episodeIds = getIntent().getExtras().getIntegerArrayList("episodeids");

        final List<PlaylistItem> playlistItems = getPlaylists(this);

        if (playlistItems.size() == 0)
        {
            findViewById(R.id.episode_context_text).setVisibility(View.VISIBLE);
            return;
        }

        final Spinner spinner = findViewById(R.id.episode_context_playlist);
        spinner.setVisibility(View.VISIBLE);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                final PlaylistItem playlist = (PlaylistItem) parent.getSelectedItem();

                if (playlist.getID() != getResources().getInteger(R.integer.default_playlist_select)) {

                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

                    if (prefs.getBoolean("pref_hide_empty_playlists", false) && playlistIsEmpty(mActivity, playlist.getID()))
                    {
                        final SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("refresh_vp", true);
                        editor.apply();
                    }

                    final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                    db.addEpisodesToPlaylist(playlist.getID(), episodeIds);
                    db.close();

                    showToast(mActivity, mActivity.getString(R.string.alert_episode_playlist_added, playlist.getName()));
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (playlistItems.size() == 0) {
            spinner.setVisibility(View.GONE);
        }
        else
        {
            final PlaylistItem playlistEmpty = new PlaylistItem();
            playlistEmpty.setID(getResources().getInteger(R.integer.default_playlist_select));
            playlistEmpty.setName(getString(R.string.dropdown_playlist_select));
            playlistItems.add(0, playlistEmpty);

            spinner.setAdapter(new PlaylistsAssignAdapter(this, playlistItems));
        }
    }
}
