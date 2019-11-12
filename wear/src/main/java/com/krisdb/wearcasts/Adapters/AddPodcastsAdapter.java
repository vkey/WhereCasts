package com.krisdb.wearcasts.Adapters;

import android.content.ContentValues;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;

import com.krisdb.wearcasts.Async.SaveLogo;
import com.krisdb.wearcasts.Async.SyncPodcasts;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.Async.GetEpisodes;
import com.krisdb.wearcastslibrary.ChannelItem;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.net.URL;
import java.util.List;
import java.util.Objects;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;


public class AddPodcastsAdapter extends WearableRecyclerView.Adapter<AddPodcastsAdapter.ViewHolder> {

    private List<PodcastItem> mPodcasts;
    private Context mContext;
    private int mHeaderColor;
    private String mDensityName;
    private boolean isRound;

    static class ViewHolder extends WearableRecyclerView.ViewHolder {

        private final TextView title, description;
        private final ImageView add, episodesExpand;
        private final ConstraintLayout layout;
        private final RecyclerView episodes;
        private final ProgressBar episodesProgress;

        ViewHolder(final View view) {
            super(view);
            title = view.findViewById(R.id.add_podcast_row_item_title);
            description = view.findViewById(R.id.add_podcast_row_item_description);
            add = view.findViewById(R.id.add_podcast_row_item_add);
            layout = view.findViewById(R.id.add_podcast_row_item_layout);
            episodesExpand = view.findViewById(R.id.add_podcast_row_item_expand);
            episodes = view.findViewById(R.id.add_podcast_row_item_episodes);
            episodesProgress = view.findViewById(R.id.add_podcast_row_item_episodes_progress);
        }
    }

    public AddPodcastsAdapter(final Context ctx, final List<PodcastItem> podcasts, final int headerColor) {
        mPodcasts = podcasts;
        mContext = ctx;
        mDensityName = CommonUtils.getDensityName(mContext);
        isRound = mContext.getResources().getConfiguration().isScreenRound();
        mHeaderColor = headerColor;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int viewType) {
        final View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.add_podcast_row_item, viewGroup, false);

        final ViewHolder holder = new ViewHolder(view);

        holder.episodesExpand.setOnClickListener(v -> {
            if (holder.episodes.getVisibility() == View.GONE) {
                holder.episodesProgress.setVisibility(View.VISIBLE);

                CommonUtils.executeAsync(new GetEpisodes(mPodcasts.get(holder.getAdapterPosition()), 10), (episodes) -> {
                    holder.episodes.setLayoutManager(new LinearLayoutManager(mContext));
                    holder.episodes.setAdapter(new EpisodesPreviewAdapter(mContext, episodes));
                    holder.episodes.setVisibility(View.VISIBLE);
                    holder.episodesProgress.setVisibility(View.GONE);
                    holder.episodesExpand.setImageDrawable(mContext.getDrawable(R.drawable.ic_podcast_preview_row_item_contract));
                });
            } else {
                holder.episodes.setVisibility(View.GONE);
                holder.episodesExpand.setImageDrawable(mContext.getDrawable(R.drawable.ic_podcast_preview_row_item_expand));
            }
        });

        holder.add.setOnClickListener(view1 -> addPodcast(holder));

        holder.title.setOnClickListener(view12 -> addPodcast(holder));

        return holder;
    }

    private void addPodcast(final ViewHolder holder)
    {
        final ChannelItem channel = mPodcasts.get(holder.getAdapterPosition()).getChannel();

        if (channel.getRSSUrl() == null)
        {
            CommonUtils.showToast(mContext, mContext.getString(R.string.alert_no_rss_url));
            return;
        }

        final ContentValues cv = new ContentValues();
        cv.put("title", channel.getTitle() != null ? channel.getTitle() : mContext.getString(R.string.no_rss_title));
        cv.put("url", channel.getRSSUrl().toString());
        cv.put("site_url", channel.getSiteUrl() != null ? channel.getSiteUrl().toString() : null);
        cv.put("dateAdded", DateUtils.GetDate());

        final URL thumbUrl = channel.getThumbnailUrl();

        if (thumbUrl != null) {
            final String thumbName = channel.getThumbnailName();

            cv.put("thumbnail_url", thumbUrl.toString());
            cv.put("thumbnail_name", thumbName);

            CommonUtils.executeSingleThreadAsync(new SaveLogo(mContext, thumbUrl.toString(), thumbName), (response) -> { });
        }

        final int podcastId = (int) new DBPodcastsEpisodes(mContext).insertPodcast(cv);

        CommonUtils.executeSingleThreadAsync(new SyncPodcasts(mContext, podcastId), (response) -> {
            if (response.getNewEpisodeCount() == 0)
                CommonUtils.showToast(mContext, mContext.getString(R.string.alert_add_podcast_no_episodes));
        });

        Utilities.vibrate(mContext);
        Utilities.ShowConfirmationActivity(mContext, mContext.getString(R.string.alert_podcast_added));
        Utilities.SetPodcstRefresh(mContext);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {

        final PodcastItem podcast = mPodcasts.get(position);
        final TextView title = viewHolder.title;
        final ConstraintLayout layout = viewHolder.layout;
        final ViewGroup.MarginLayoutParams paramsLayout = (ViewGroup.MarginLayoutParams)viewHolder.layout.getLayoutParams();

        viewHolder.episodesProgress.setVisibility(View.GONE);
        viewHolder.episodes.setVisibility(View.GONE);
        viewHolder.episodesExpand.setImageDrawable(mContext.getDrawable(R.drawable.ic_podcast_preview_row_item_expand));

        title.setText(CommonUtils.boldText(podcast.getChannel().getTitle()));

        if (podcast.getIsTitle()) //title row
        {
            title.setTextSize(17);

            viewHolder.add.setVisibility(View.GONE);
            viewHolder.description.setVisibility(View.GONE);
            viewHolder.episodesExpand.setVisibility(View.GONE);
            title.setGravity(Gravity.CENTER_HORIZONTAL);
            title.getLayoutParams().width = MATCH_PARENT;

            if (Objects.equals(mDensityName, mContext.getString(R.string.hdpi))) {
                if (isRound)
                    paramsLayout.setMargins(0, 0, 0, 20);
                else
                    paramsLayout.setMargins(0, 0, 0, 10);
            } else if (Objects.equals(mDensityName, mContext.getString(R.string.xhdpi))) {
                if (isRound)
                    paramsLayout.setMargins(0, 0, 0, 20);
                else
                    paramsLayout.setMargins(0, 0, 0, 20);
            } else
                paramsLayout.setMargins(0, 0, 0, 20);

            layout.setBackgroundColor(mHeaderColor);
            title.setBackgroundColor(mHeaderColor);
        }
        else {
            title.setTextSize(16);
            viewHolder.description.setText(CommonUtils.CleanDescription(podcast.getChannel().getDescription()));
            viewHolder.description.setVisibility(View.VISIBLE);
            viewHolder.episodesExpand.setVisibility(View.VISIBLE);

            layout.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
            title.setBackgroundColor(mContext.getColor(R.color.wc_transparent));

            if (Objects.equals(mDensityName, mContext.getString(R.string.hdpi))) {
                if (isRound)
                    paramsLayout.setMargins(40, 0, 40, 20);
                else
                    paramsLayout.setMargins(15, 0, 15, 20);
            } else if (Objects.equals(mDensityName, mContext.getString(R.string.xhdpi))) {
                if (isRound)
                    paramsLayout.setMargins(30, 0, 30, 20);
                else
                    paramsLayout.setMargins(45, 0, 45, 20);
            }
            else
                paramsLayout.setMargins(45, 20, 45, 0);

            viewHolder.add.setVisibility(View.VISIBLE);
            title.setGravity(Gravity.START);
            title.getLayoutParams().width = 0;
        }
    }

    @Override
    public int getItemCount() {
        return mPodcasts.size();
    }

}