package com.krisdb.wearcasts.Adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;
import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcasts;


public class PlaylistsSettingsAdapter extends RecyclerView.Adapter<PlaylistsSettingsAdapter.ViewHolder> {

    private List<PlaylistItem> mPlaylists;
    private Activity mContext;
    private WeakReference<Activity> mActivityRef;

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final EditText name;
        private final ImageView delete;

        ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.playlist_row_item_name);
            delete = view.findViewById(R.id.playlist_row_item_button_delete);
        }
    }

    PlaylistsSettingsAdapter(final Activity context, final List<PlaylistItem> playlists) {
        mPlaylists = playlists;
        mContext = context;
        mActivityRef = new WeakReference<>(mContext);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        final View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.playlist_settings_row_item, viewGroup, false);

        final ViewHolder holder = new ViewHolder(view);
        final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mContext);

        holder.name.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE){
                    final String text = holder.name.getText().toString();

                    if (text.length() == 0) {
                        CommonUtils.showToast(mContext, mContext.getString(R.string.validation_podcast_rename_title));
                        return true;
                    }

                    final int position = holder.getAdapterPosition();
                    final int playlistId = mPlaylists.get(position).getID();
                    db.updatePlaylist(holder.name.getText().toString(), playlistId);
                    mPlaylists = getPlaylists(mContext, false);
                    notifyItemChanged(position);

                    holder.name.setText(v.getText().toString());
                    final InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    final AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
                    alert.setMessage(mContext.getString(R.string.alert_playlist_delete));
                    alert.setPositiveButton(mContext.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final int position = holder.getAdapterPosition();
                            final int playlistId = mPlaylists.get(position).getID();

                            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

                            if (Integer.valueOf(prefs.getString("pref_display_home_screen", "0")) == playlistId)
                                Utilities.resetHomeScreen(mContext);

                            db.deletePlaylist(playlistId);
                            mPlaylists = getPlaylists(mContext, false);

                            final List<PodcastItem> podcasts = GetPodcasts(mContext);
                            final SharedPreferences.Editor editor = prefs.edit();
                            final int autoAssignDefaultPlaylistId = mContext.getResources().getInteger(R.integer.default_playlist_select);

                            for (final PodcastItem podcast : podcasts) {
                                final int autoAssignId = Integer.valueOf(prefs.getString("pref_" + podcast.getPodcastId() + "_auto_assign_playlist", String.valueOf(autoAssignDefaultPlaylistId)));

                                if (autoAssignId == playlistId)
                                    editor.putString("pref_" + podcast.getPodcastId() + "_auto_assign_playlist", String.valueOf(autoAssignDefaultPlaylistId));
                            }

                            editor.putBoolean("refresh_vp", true);
                            editor.apply();

                            notifyDataSetChanged();
                        }
                    });

                    alert.setNegativeButton(mContext.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    alert.show();
                }
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {

        final PlaylistItem playlist = mPlaylists.get(position);

        //final ViewGroup.MarginLayoutParams paramsLayout = (ViewGroup.MarginLayoutParams)viewHolder.layout.getLayoutParams();

        //if (position == 0)
        //paramsLayout.setMargins(0, 50, 0, 15);

        //if (position+1 == mPlaylistSize)
        //paramsLayout.setMargins(0, 0, 0, 70);

        viewHolder.name.setText(playlist.getName());
    }

    @Override
    public int getItemCount() {
        return mPlaylists.size();
    }

}