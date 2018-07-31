package com.krisdb.wearcasts;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class EpisodeContextActivity extends BaseFragmentActivity {

    private Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.episode_context_activity);

        mActivity = this;

        final int episodeId = getIntent().getExtras().getInt("episodeid");

        final PodcastItem episode = DBUtilities.GetEpisode(mActivity, episodeId);

        final Button togglePlayButton = findViewById(R.id.episode_context_toggle_played);

        togglePlayButton.setText(episode.getFinished() ? getString(R.string.button_mark_unplayed) : getString(R.string.button_mark_played));

        togglePlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PodcastItem episodeItem = DBUtilities.GetEpisode(mActivity, episodeId);

                DBUtilities.SaveEpisodeValue(mActivity, episodeItem, "finished", episodeItem.getFinished() ? 0 : 1);

                episodeItem = DBUtilities.GetEpisode(mActivity, episodeId);

                togglePlayButton.setText(episodeItem.getFinished() ? getString(R.string.button_mark_unplayed) : getString(R.string.button_mark_played));
            }
        });

        final List<PlaylistItem> playlistItems = DBUtilities.getPlaylists(this);
        final Spinner spinner = findViewById(R.id.episode_context_playlist);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                final PlaylistItem playlist = (PlaylistItem) parent.getSelectedItem();

                if (playlist.getID() != getResources().getInteger(R.integer.default_playlist_select)) {
                    final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mActivity);
                    db.addEpisodeToPlaylist(playlist.getID(), episode.getEpisodeId());
                    db.close();

                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

                    if (prefs.getBoolean("pref_hide_empty_playlists", false))
                    {
                        final SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("refresh_vp", true);
                        editor.apply();
                    }

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
