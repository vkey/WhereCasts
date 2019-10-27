package com.krisdb.wearcasts.Adapters;

import android.app.Activity;
import android.os.AsyncTask;
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
import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
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

        holder.episodesExpand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.episodes.getVisibility() == View.GONE) {
                    holder.episodesProgress.setVisibility(View.VISIBLE);
                    new AsyncTasks.GetEpisodes(mPodcasts.get(holder.getAdapterPosition()), 50,
                            new Interfaces.PodcastsResponse() {
                                @Override
                                public void processFinish(List<PodcastItem> episodes) {
                                    holder.episodes.setLayoutManager(new LinearLayoutManager(mContext));
                                    holder.episodes.setAdapter(new EpisodesAdapter(mContext, episodes, isConnected));
                                    holder.episodes.setVisibility(View.VISIBLE);
                                    holder.episodesProgress.setVisibility(View.GONE);
                                    holder.episodesExpand.setImageDrawable(mContext.getDrawable(R.drawable.ic_podcast_row_item_contract));
                                }
                            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    holder.episodes.setVisibility(View.GONE);
                    holder.episodesExpand.setImageDrawable(mContext.getDrawable(R.drawable.ic_podcast_row_item_expand));
                }
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
            viewHolder.description.setText(CommonUtils.CleanDescription(podcast.getChannel().getDescription()));

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
                viewHolder.add_image.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_podcast_add));

            viewHolder.add_image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isConnected) {
                        Utilities.SendToWatch(mContext, podcast);
                        mPodcasts.get(position).setIsSenttoWatch(true);
                        notifyItemChanged(position);
                    } else
                        CommonUtils.showSnackbar(viewHolder.add_image, mContext.getString(R.string.button_text_no_device));
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mPodcasts.size();
    }

}