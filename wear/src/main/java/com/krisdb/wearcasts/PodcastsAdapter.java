package com.krisdb.wearcasts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wear.widget.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;


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

    PodcastsAdapter(final Activity activity, final List<PodcastItem> podcasts, final int playlistId) {
        mPodcasts = podcasts;
        mContext = activity;
        mPlaylistId = playlistId;
    }

    public int refreshContent()
    {
        mPodcasts = DBUtilities.GetPodcasts(mContext);
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

        viewHolder.thumbnail.setImageDrawable(podcast.getDisplayThumbnail());
        viewHolder.title.setText(podcast.getChannel().getTitle());

        /*
        long fontSize = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(mContext).getString("pref_font_size", String.valueOf(mContext.getResources().getInteger(R.integer.default_font_size))));

        if (fontSize == Enums.FontSize.SMALL.getFontSizeID()) {
            viewHolder.title.setTextSize(10);
            viewHolder.count.setTextSize(6);
        }
        else if (fontSize == Enums.FontSize.LARGE.getFontSizeID()) {
            viewHolder.title.setTextSize(18);
            viewHolder.count.setTextSize(14);
        }
        else {
            viewHolder.title.setTextSize(14);
            viewHolder.count.setTextSize(10);
        }
        */
        if (podcast.getNewCount() > 0) {
            viewHolder.count.setText(String.valueOf(podcast.getNewCount()));
            viewHolder.count.setVisibility(View.VISIBLE);
        } else
            viewHolder.count.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return mPodcasts.size();
    }

}