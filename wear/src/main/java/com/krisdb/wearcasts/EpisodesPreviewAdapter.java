package com.krisdb.wearcasts;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

public class EpisodesPreviewAdapter extends RecyclerView.Adapter<EpisodesPreviewAdapter.ViewHolder> {

    private List<PodcastItem> mEpisodes;
    private Context mContext;

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView title, description, date;

        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.episode_preview_row_item_title);
            date = view.findViewById(R.id.episode_preview_row_item_date);
            description = view.findViewById(R.id.episode_preview_row_item_description);
        }
    }

    EpisodesPreviewAdapter(final Context ctx, final List<PodcastItem> episodes) {
        mContext = ctx;
        mEpisodes = episodes;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.episode_preview_row_item, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {

        final PodcastItem episode = mEpisodes.get(position);

        final SpannableString title = new SpannableString(episode.getTitle());
        title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        viewHolder.title.setText(title);
        viewHolder.date.setText(DateUtils.GetDisplayDate(mContext, episode.getPubDate()));

        /*
        if (episode.getDescription() != null){
            String description = CommonUtils.CleanDescription(episode.getDescription());
            if (description.length() > 150)
                description = description.substring(0, 100).concat("...");

            viewHolder.description.setText(description);
        }
        */
    }

    @Override
    public int getItemCount() {
        return mEpisodes.size();
    }

}