package com.krisdb.wearcasts;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

public class PodcastsAdapter extends RecyclerView.Adapter<PodcastsAdapter.ViewHolder> {

    private List<PodcastItem> mPodcasts;
    private Context mContext;
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

    PodcastsAdapter(Context ctx, List<PodcastItem> podcasts, boolean connected) {
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
                    new AsyncTasks.GetEpisodes(mPodcasts.get(holder.getAdapterPosition()),
                            new Interfaces.PodcastsResponse() {
                                @Override
                                public void processFinish(List<PodcastItem> episodes) {
                                    if (episodes.size() > 50)
                                        episodes =  episodes.subList(0, 50);

                                    holder.episodes.setLayoutManager(new LinearLayoutManager(mContext));
                                    holder.episodes.setAdapter(new EpisodesAdapter(mContext, episodes));
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

        if (podcast.getIsTitle()) return;
        viewHolder.episodesProgress.setVisibility(View.GONE);
        viewHolder.episodes.setVisibility(View.GONE);
        viewHolder.episodesExpand.setImageDrawable(mContext.getDrawable(R.drawable.ic_podcast_row_item_expand));

        viewHolder.title.setText(podcast.getChannel().getTitle());
        viewHolder.description.setText(CommonUtils.CleanDescription(podcast.getChannel().getDescription()));

        if (podcast.getChannel().getThumbnailUrl() != null)
            Glide.with(mContext).load(podcast.getChannel().getThumbnailUrl().toString()).into(viewHolder.thumbnail);
        else
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
                    }
                    else
                        CommonUtils.showToast(mContext, "No Wear device connected");
                }
            });
    }

    @Override
    public int getItemCount() {
        return mPodcasts.size();
    }

}