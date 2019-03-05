package com.krisdb.wearcasts.Adapters;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.wear.widget.WearableRecyclerView;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.krisdb.wearcasts.Activities.PodcastEpisodeListActivity;
import com.krisdb.wearcasts.Databases.DBUtilities;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Settings.SettingsPodcastActivity;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

import static com.krisdb.wearcastslibrary.CommonUtils.showToast;


public class  PodcastsAdapter extends WearableRecyclerView.Adapter<PodcastsAdapter.ViewHolder> {

    private List<PodcastItem> mPodcasts;
    private Activity mContext;
    private int mPlaylistId;

    static class ViewHolder extends WearableRecyclerView.ViewHolder {

        private final TextView title, count;
        private final ImageView thumbnail;
        private final RelativeLayout layout;

        ViewHolder(View view) {
            super(view);
            count = view.findViewById(R.id.podcast_row_item_count);
            title = view.findViewById(R.id.podcast_row_item_title);
            thumbnail = view.findViewById(R.id.podcast_row_item_thumbnail);
            layout = view.findViewById(R.id.podcast_row_item_layout);
        }
    }

    public PodcastsAdapter(final Activity activity, final List<PodcastItem> podcasts, final int playlistId) {
        mPodcasts = podcasts;
        mContext = activity;
        mPlaylistId = playlistId;
    }

    public int refreshContent()
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final Boolean hideEmpty = prefs.getBoolean("pref_hide_empty", false);
        final Boolean showDownloaded = prefs.getBoolean("pref_display_show_downloaded", false);

        if (prefs.getBoolean("long_press_tip_shown", false) == false && DBUtilities.GetPodcasts(mContext).size() > 0)
            showToast(mContext, mContext.getString(R.string.tips_swipe_long_press));

        mPodcasts = DBUtilities.GetPodcasts(mContext, hideEmpty, showDownloaded);
        notifyDataSetChanged();

        return mPodcasts.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        final View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.podcast_row_item, viewGroup, false);

        final ViewHolder holder = new ViewHolder(view);

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(mContext, PodcastEpisodeListActivity.class);
                final Bundle bundle = new Bundle();
                bundle.putInt("podcastId", mPodcasts.get(holder.getAdapterPosition()).getPodcastId());
                bundle.putInt("playlistId", mPlaylistId);
                intent.putExtras(bundle);

                mContext.startActivity(intent);
            }
        });

        holder.layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final Intent intent = new Intent(mContext, SettingsPodcastActivity.class);
                final Bundle bundle = new Bundle();
                bundle.putInt("podcastId", mPodcasts.get(holder.getAdapterPosition()).getPodcastId());
                intent.putExtras(bundle);

                mContext.startActivity(intent);
                return false;
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {

        final PodcastItem podcast = mPodcasts.get(position);

        if (podcast == null || podcast.getChannel() == null) return;

        viewHolder.thumbnail.setImageDrawable(podcast.getDisplayThumbnail());

        if (podcast.getChannel().getTitle() != null)
            viewHolder.title.setText(podcast.getChannel().getTitle());

        if (podcast.getNewCount() > 0) {
            final SpannableString count = new SpannableString(String.valueOf(podcast.getNewCount()));
            count.setSpan(new StyleSpan(Typeface.BOLD), 0, count.length(), 0);
            viewHolder.count.setText(count);
            viewHolder.count.setVisibility(View.VISIBLE);
        } else
            viewHolder.count.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return mPodcasts.size();
    }

}