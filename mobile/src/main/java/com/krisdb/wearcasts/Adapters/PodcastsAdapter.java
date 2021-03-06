package com.krisdb.wearcasts.Adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities;
import com.krisdb.wearcastslibrary.Async.FetchPodcast;
import com.krisdb.wearcastslibrary.Async.GetEpisodes;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

public class PodcastsAdapter extends RecyclerView.Adapter<PodcastsAdapter.ViewHolder> {

    private List<PodcastItem> mPodcasts;
    private Activity mContext;
    private Boolean isConnected;

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView title, description;
        private final ImageView add_image, thumbnail, episodesExpand;
        private final RecyclerView episodes;
        private final ProgressBar episodesProgress;

        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.podcast_row_item_title);
            description = view.findViewById(R.id.podcast_row_item_description);
            add_image = view.findViewById(R.id.podcast_row_item_add);
            thumbnail = view.findViewById(R.id.podcast_row_item_thumbnail);
            episodesExpand = view.findViewById(R.id.podcast_row_item_expand);
            episodes = view.findViewById(R.id.podcast_row_item_episodes);
            episodesProgress = view.findViewById(R.id.podcast_row_item_episodes_progress);
        }
    }

    public PodcastsAdapter(final Activity ctx, final List<PodcastItem> podcasts, final Boolean connected) {
        mPodcasts = podcasts;
        mContext = ctx;
        isConnected = connected;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.podcast_row_item, viewGroup, false);

        final ViewHolder holder = new ViewHolder(view);

        holder.episodesExpand.setOnClickListener(v -> {
            if (holder.episodes.getVisibility() == View.GONE) {
                holder.episodesProgress.setVisibility(View.VISIBLE);

                CommonUtils.executeAsync(new GetEpisodes(mPodcasts.get(holder.getAdapterPosition()), 50), (episodes) -> {
                    holder.episodes.setLayoutManager(new LinearLayoutManager(mContext));
                    holder.episodes.setAdapter(new EpisodesAdapter(mContext, episodes, isConnected));
                    holder.episodes.setVisibility(View.VISIBLE);
                    holder.episodesProgress.setVisibility(View.GONE);
                    holder.episodesExpand.setImageDrawable(mContext.getDrawable(R.drawable.ic_podcast_row_item_contract));
                });
            } else {
                holder.episodes.setVisibility(View.GONE);
                holder.episodesExpand.setImageDrawable(mContext.getDrawable(R.drawable.ic_podcast_row_item_expand));
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {

        final PodcastItem podcast = mPodcasts.get(position);

        if (!podcast.getIsTitle()) {
            viewHolder.episodesProgress.setVisibility(View.GONE);
            viewHolder.episodes.setVisibility(View.GONE);
            viewHolder.episodesExpand.setImageDrawable(mContext.getDrawable(R.drawable.ic_podcast_row_item_expand));

            viewHolder.title.setText(podcast.getChannel().getTitle());

            if (podcast.getChannel().getDescription() == null) {
                CommonUtils.executeAsync(new FetchPodcast(podcast.getChannel().getRSSUrl().toString()), (result) -> {
                    String description = result.getChannel().getDescription();
                    if (description != null) {
                        if (description.length() > 130)
                            description = description.substring(0, 130).concat("...");

                        viewHolder.description.setText(description);
                        viewHolder.description.setVisibility(View.VISIBLE);
                    } else
                        viewHolder.description.setVisibility(View.INVISIBLE);
                });
            }
            else
            {
                viewHolder.description.setText(podcast.getChannel().getDescription());
                viewHolder.description.setVisibility(View.VISIBLE);
            }

            if (podcast.getChannel().getThumbnailUrl() != null) {
                try {
                    Glide.with(mContext).load(podcast.getChannel().getThumbnailUrl().toString()).into(viewHolder.thumbnail);
                } catch (IllegalArgumentException ex) {
                    viewHolder.thumbnail.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_thumb_default));
                }
            } else
                viewHolder.thumbnail.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_thumb_default));

            if (podcast.getIsSenttoWatch())
                viewHolder.add_image.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_podcast_add_confirm));
            else
                viewHolder.add_image.setImageDrawable(ContextCompat.getDrawable(mContext, CommonUtils.isNightModeActive(mContext) ? R.drawable.ic_podcast_add_light : R.drawable.ic_podcast_add));

            viewHolder.add_image.setOnClickListener(view -> {
                if (isConnected) {
                    Utilities.SendToWatch(mContext, podcast);
                    CommonUtils.showSnackbar(viewHolder.add_image, mContext.getString(R.string.alert_podcast_added));

                    mPodcasts.get(position).setIsSenttoWatch(true);
                    notifyItemChanged(position);
                } else
                    CommonUtils.showSnackbar(viewHolder.add_image, mContext.getString(R.string.button_text_no_device));
            });
        }
    }

    @Override
    public int getItemCount() {
        return mPodcasts.size();
    }

}