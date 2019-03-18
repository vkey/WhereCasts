package com.krisdb.wearcasts.Adapters;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.CacheUtils;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.net.URL;
import java.util.List;
import java.util.Objects;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;

import static com.krisdb.wearcastslibrary.CommonUtils.showToast;


public class AddPodcastsAdapter extends WearableRecyclerView.Adapter<AddPodcastsAdapter.ViewHolder> {

    private List<PodcastItem> mPodcasts;
    private Context mContext;
    private int mHeaderColor;
    private String mDensityName;
    private boolean isRound;

    static class ViewHolder extends WearableRecyclerView.ViewHolder {

        private final TextView title, description;
        private final ImageView add, episodesExpand;
        private final RelativeLayout layout;
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

        holder.episodesExpand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.episodes.getVisibility() == View.GONE) {
                    holder.episodesProgress.setVisibility(View.VISIBLE);
                    new com.krisdb.wearcastslibrary.AsyncTasks.GetEpisodes(mPodcasts.get(holder.getAdapterPosition()), 10,
                            new Interfaces.PodcastsResponse() {
                                @Override
                                public void processFinish(List<PodcastItem> episodes) {
                                    holder.episodes.setLayoutManager(new LinearLayoutManager(mContext));
                                    holder.episodes.setAdapter(new EpisodesPreviewAdapter(mContext, episodes));
                                    holder.episodes.setVisibility(View.VISIBLE);
                                    holder.episodesProgress.setVisibility(View.GONE);
                                    holder.episodesExpand.setImageDrawable(mContext.getDrawable(R.drawable.ic_podcast_preview_row_item_contract));
                                }
                            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    holder.episodes.setVisibility(View.GONE);
                    holder.episodesExpand.setImageDrawable(mContext.getDrawable(R.drawable.ic_podcast_preview_row_item_expand));
                }
            }
        });

        holder.add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addPodcast(holder);
            }
        });

        holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addPodcast(holder);
            }
        });

        return holder;
    }

    private void addPodcast(final ViewHolder holder)
    {
        final ContentValues cv = new ContentValues();
        cv.put("title", mPodcasts.get(holder.getAdapterPosition()).getChannel().getTitle());
        cv.put("url", mPodcasts.get(holder.getAdapterPosition()).getChannel().getRSSUrl().toString());
        cv.put("site_url", mPodcasts.get(holder.getAdapterPosition()).getChannel().getSiteUrl() != null ? mPodcasts.get(holder.getAdapterPosition()).getChannel().getSiteUrl().toString() : null);
        cv.put("dateAdded", DateUtils.GetDate());

        final URL thumbUrl = mPodcasts.get(holder.getAdapterPosition()).getChannel().getThumbnailUrl();

        if (thumbUrl != null) {
            final String thumbName = mPodcasts.get(holder.getAdapterPosition()).getChannel().getThumbnailName();

            cv.put("thumbnail_url", thumbUrl.toString());
            cv.put("thumbnail_name", thumbName);

            new com.krisdb.wearcastslibrary.AsyncTasks.SaveLogo(mContext, thumbUrl.toString(), thumbName,
                    new Interfaces.AsyncResponse() {
                        @Override
                        public void processFinish() {}
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        final int podcastId = (int) new DBPodcastsEpisodes(mContext).insertPodcast(cv);

        new AsyncTasks.GetPodcastEpisodes(mContext, podcastId,
                new Interfaces.IntResponse() {
                    @Override
                    public void processFinish(final int count) {
                        if (count == 0)
                            CommonUtils.showToast(mContext, mContext.getString(R.string.alert_add_podcast_no_episodes));
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        showToast(mContext, mContext.getString(R.string.alert_podcast_added));
        CacheUtils.deletePodcastsCache(mContext);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {

        final PodcastItem podcast = mPodcasts.get(position);
        final TextView title = viewHolder.title;
        final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)viewHolder.layout.getLayoutParams();

        viewHolder.episodesProgress.setVisibility(View.GONE);
        viewHolder.episodes.setVisibility(View.GONE);
        viewHolder.episodesExpand.setImageDrawable(mContext.getDrawable(R.drawable.ic_podcast_preview_row_item_expand));

        final SpannableString titleText = new SpannableString(podcast.getChannel().getTitle());
        titleText.setSpan(new StyleSpan(Typeface.BOLD), 0, titleText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(titleText);

        if (podcast.getIsTitle()) //title row
        {
            title.setTextSize(17);
            final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)title.getLayoutParams();
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            title.setLayoutParams(layoutParams);
            title.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;

            viewHolder.add.setVisibility(View.GONE);
            viewHolder.description.setVisibility(View.GONE);
            viewHolder.episodesExpand.setVisibility(View.GONE);

            if (Objects.equals(mDensityName, mContext.getString(R.string.hdpi))) {
                if (isRound)
                    viewHolder.layout.setPadding(0, 10, 0, 10);
                else
                    viewHolder.layout.setPadding(0, 2, 0, 2);

                params.setMargins(0, 0, 0, 0);
            }
            else if (Objects.equals(mDensityName, mContext.getString(R.string.xhdpi))) {
                if (isRound) {
                    viewHolder.layout.setPadding(0, 10, 0, 10);
                    params.setMargins(0, 0, 0, 0);
                }
                else
                {
                    viewHolder.layout.setPadding(0, 4, 0, 4);
                    params.setMargins(0, 0, 0, 0);
                }
            }
            else {
                viewHolder.layout.setPadding(0, 5, 0, 5);
                params.setMargins(0, 0, 0, 0);
            }

            viewHolder.layout.setBackgroundColor(mHeaderColor);
            title.setBackgroundColor(mHeaderColor);
        }
        else {
            title.setTextSize(16);
            viewHolder.description.setText(CommonUtils.CleanDescription(podcast.getChannel().getDescription()));
            viewHolder.description.setVisibility(View.VISIBLE);
            viewHolder.episodesExpand.setVisibility(View.VISIBLE);
            title.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;

            viewHolder.layout.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
            title.setBackgroundColor(mContext.getColor(R.color.wc_transparent));

            if (Objects.equals(mDensityName, mContext.getString(R.string.hdpi))) {
                viewHolder.layout.setPadding(0, 0, 0, 0);
                if (isRound)
                    params.setMargins(30, 40, 20, 0);
                else
                    params.setMargins(10, 30, 10, 0);
            } else if (Objects.equals(mDensityName, mContext.getString(R.string.xhdpi))) {
                viewHolder.layout.setPadding(0, 0, 0, 0);
                params.setMargins(50, 60, 30, 0);
            } else {
                viewHolder.layout.setPadding(0, 0, 0, 0);
                params.setMargins(20, 60, 20, 0);
            }

            viewHolder.add.setVisibility(View.VISIBLE);
            title.setGravity(Gravity.NO_GRAVITY);
        }
    }

    @Override
    public int getItemCount() {
        return mPodcasts.size();
    }

}