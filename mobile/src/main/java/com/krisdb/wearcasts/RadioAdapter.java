package com.krisdb.wearcasts;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

public class RadioAdapter extends RecyclerView.Adapter<RadioAdapter.ViewHolder> {

    private List<PodcastItem> mStations;
    private Context mContext;
    private Boolean isConnected;

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView title, description;
        private final ImageView sendEpisode;

        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.radio_row_item_title);
            description = view.findViewById(R.id.radio_row_item_description);
            sendEpisode = view.findViewById(R.id.radio_row_item_send);
        }
    }

    RadioAdapter(final Context ctx, final List<PodcastItem> stations, final Boolean connected) {
        mContext = ctx;
        mStations = stations;
        isConnected = connected;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.radio_row_item, viewGroup, false);
        final ViewHolder holder = new ViewHolder(view);

        holder.sendEpisode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected) {
                    final int position = holder.getAdapterPosition();

                    Utilities.sendEpisode(mContext, mStations.get(position));
                    mStations.get(position).setIsSenttoWatch(true);
                    notifyItemChanged(position);
                }
                else
                    CommonUtils.showToast(mContext, mContext.getString(R.string.button_text_no_device));
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {

        final PodcastItem station = mStations.get(position);

        viewHolder.title.setText(station.getTitle());

        if (station.getDescription() != null)
            viewHolder.description.setText(CommonUtils.CleanDescription(station.getDescription()));

        if (station.getIsSenttoWatch())
            viewHolder.sendEpisode.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_podcast_add_confirm));
        else
            viewHolder.sendEpisode.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_action_send_episode_to_phone));
    }

    @Override
    public int getItemCount() {
        return mStations.size();
    }

}