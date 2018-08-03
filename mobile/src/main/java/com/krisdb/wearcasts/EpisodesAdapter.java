package com.krisdb.wearcasts;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.Date;
import java.util.List;

public class EpisodesAdapter extends RecyclerView.Adapter<EpisodesAdapter.ViewHolder> {

    private List<PodcastItem> mEpisodes;
    private Context mContext;
    private Boolean isConnected;

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView title, description, date;
        private final ImageView sendEpisode;

        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.episode_row_item_title);
            description = view.findViewById(R.id.episode_row_item_description);
            date = view.findViewById(R.id.episode_row_item_date);
            sendEpisode = view.findViewById(R.id.episode_row_item_send);
        }
    }

    EpisodesAdapter(final Context ctx, final List<PodcastItem> episodes, final Boolean connected) {
        mContext = ctx;
        mEpisodes = episodes;
        isConnected = connected;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.episode_row_item, viewGroup, false);
        final ViewHolder holder = new ViewHolder(view);

        holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.description.getVisibility() == View.VISIBLE)
                    holder.description.setVisibility(View.GONE);
                else
                    holder.description.setVisibility(View.VISIBLE);
            }
        });

        holder.sendEpisode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected) {
                    final PodcastItem episode = mEpisodes.get(holder.getAdapterPosition());

                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

                    if (prefs.getInt("episode_import", 0) == 0) {
                        final AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
                        alert.setMessage(mContext.getString(R.string.alert_episode_import_first));
                        alert.setPositiveButton(mContext.getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sendEpisode(holder.getAdapterPosition());
                                dialog.dismiss();
                            }
                        });

                        alert.setNegativeButton(mContext.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
                        final SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt("episode_import", 1);
                        editor.apply();
                    } else
                        sendEpisode(holder.getAdapterPosition());
                }
                else
                    CommonUtils.showToast(mContext, mContext.getString(R.string.button_text_no_device));
            }
        });

        return holder;
    }

    private void sendEpisode(final int position)
    {
        final PodcastItem episode = mEpisodes.get(position);

        final PutDataMapRequest dataMap = PutDataMapRequest.create("/episodeimport");
        dataMap.getDataMap().putString("title", episode.getTitle());
        dataMap.getDataMap().putString("description", episode.getDescription());
        if (episode.getMediaUrl() != null)
            dataMap.getDataMap().putString("mediaurl", episode.getMediaUrl().toString());
        if (episode.getEpisodeUrl() != null)
            dataMap.getDataMap().putString("url", episode.getEpisodeUrl().toString());
        dataMap.getDataMap().putString("pubDate", episode.getPubDate());
        dataMap.getDataMap().putInt("duration", episode.getDuration());
        dataMap.getDataMap().putLong("time", new Date().getTime());

        CommonUtils.DeviceSync(mContext, dataMap, mContext.getString(R.string.alert_episode_added), Toast.LENGTH_SHORT);

        mEpisodes.get(position).setIsSenttoWatch(true);
        notifyItemChanged(position);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {

        final PodcastItem episode = mEpisodes.get(position);

        final SpannableString title = new SpannableString(episode.getTitle());
        title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        viewHolder.title.setText(title);
        viewHolder.date.setText(DateUtils.GetDisplayDate(mContext, episode.getPubDate()));

        if (episode.getDescription() != null)
            viewHolder.description.setText(CommonUtils.CleanDescription(episode.getDescription()));

        if (episode.getIsSenttoWatch())
            viewHolder.sendEpisode.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_podcast_add_confirm));
        else
            viewHolder.sendEpisode.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_action_send_episode_to_phone));
    }

    @Override
    public int getItemCount() {
        return mEpisodes.size();
    }

}