package com.krisdb.wearcasts.Adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.wear.widget.WearableRecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.krisdb.wearcasts.Activities.EpisodeActivity;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Settings.SettingsPodcastActivity;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisode;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisodes;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.SaveEpisodeValue;
import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcast;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;


public class EpisodesAdapter extends WearableRecyclerView.Adapter<EpisodesAdapter.ViewHolder> {

    private List<PodcastItem> mEpisodes, mSelectedEpisodes;
    private Activity mContext;
    private int mTextColor;
    private String mDensityName;
    private boolean isRound, isXHDPI, isHDPI;
    private WeakReference<Activity> mActivityRef;
    private Interfaces.OnEpisodeSelectedListener mEpisodeSelectedCallback;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    static class ViewHolder extends WearableRecyclerView.ViewHolder {

        private final TextView title, date, duration;
        private final ImageView thumbnailTitle, download;
        private final RelativeLayout layout;

        ViewHolder(final View view) {
            super(view);
            title = view.findViewById(R.id.episode_row_item_title);
            date = view.findViewById(R.id.episode_row_item_date);
            duration = view.findViewById(R.id.episode_row_item_duration);
            thumbnailTitle = view.findViewById(R.id.episode_row_item_title_thumbnail);
            download = view.findViewById(R.id.episode_row_item_download);
            layout = view.findViewById(R.id.episode_row_item_layout);
        }
    }

    public EpisodesAdapter(final Activity context, final List<PodcastItem> episodes, final int textColor, final SwipeRefreshLayout refreshLayout, final Interfaces.OnEpisodeSelectedListener episodeSelectedCallback) {
        mEpisodes = episodes;
        mContext = context;
        mTextColor = textColor;
        mDensityName = CommonUtils.getDensityName(mContext);
        isRound = mContext.getResources().getConfiguration().isScreenRound();
        mActivityRef = new WeakReference<>(mContext);
        mEpisodeSelectedCallback = episodeSelectedCallback;
        mSelectedEpisodes = new ArrayList<>();
        isXHDPI = Objects.equals(mDensityName, mContext.getString(R.string.xhdpi));
        isHDPI = Objects.equals(mDensityName, mContext.getString(R.string.hdpi));
        mSwipeRefreshLayout = refreshLayout;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int viewType) {
        final View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.episode_row_item, viewGroup, false);

        final ViewHolder holder = new ViewHolder(view);

        if (holder.thumbnailTitle != null) {
            holder.thumbnailTitle.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    final PodcastItem podcast = GetPodcast(mContext, mEpisodes.get(holder.getAdapterPosition()).getPodcastId());

                    final Intent intent = new Intent(mContext, SettingsPodcastActivity.class);
                    final Bundle bundle = new Bundle();
                    bundle.putInt("podcastId", podcast.getPodcastId());
                    intent.putExtras(bundle);

                    if (podcast.getPodcastId() > 0)
                        mContext.startActivity(intent);

                    return false;
                }
            });
        }

        holder.download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initDownload(holder, holder.getAdapterPosition());
            }
        });

            holder.duration.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    initDownload(holder, holder.getAdapterPosition());
                }
            });

            /*
            holder.date.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openEpisode(holder.getAdapterPosition());
                }
            });

            holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openEpisode(holder.getAdapterPosition());
                }
        });
        */

            holder.date.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    showContext(holder.getAdapterPosition());
                    return false;
                }
            });


        holder.title.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showContext(holder.getAdapterPosition());
                return false;
            }
        });

        return holder;
    }

    private void initDownload(final ViewHolder holder, final int position)
    {
        int episodeId = mEpisodes.get(position).getEpisodeId();

        final PodcastItem episode = GetEpisode(mContext, episodeId);

        final int downloadId = Utilities.getDownloadId(mContext, episodeId);

        if (downloadId > 0) {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
                alert.setMessage(mContext.getString(R.string.confirm_cancel_download));
                alert.setPositiveButton(mContext.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    int position = holder.getAdapterPosition();

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final DownloadManager manager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
                        if (manager != null)
                            manager.remove(downloadId);

                        SaveEpisodeValue(mContext, episode, "downloadid", 0);
                        Utilities.DeleteMediaFile(mContext, mEpisodes.get(position));
                        mEpisodes.get(position).setIsDownloaded(false);
                        notifyItemChanged(position);
                    }
                });
                alert.setNegativeButton(mContext.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alert.show();
            }

        } else if (episode.getIsDownloaded()) {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
                alert.setMessage(mContext.getString(R.string.confirm_delete_download));
                alert.setPositiveButton(mContext.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    int position = holder.getAdapterPosition();

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utilities.DeleteMediaFile(mContext, mEpisodes.get(position));

                            mEpisodes.get(position).setIsDownloaded(false);
                            notifyItemChanged(position);
                            dialog.dismiss();
                    }
                });
                alert.setNegativeButton(mContext.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alert.show();
            }
        }
        else if (CommonUtils.getActiveNetwork(mContext) == null)
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
                alert.setMessage(mContext.getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(mContext.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mContext.startActivity(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"));
                        dialog.dismiss();
                    }
                });

                alert.setNegativeButton(mContext.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        }
        else if (CommonUtils.HighBandwidthNetwork(mContext) == false) {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
                alert.setMessage(mContext.getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(mContext.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mContext.startActivity(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"));
                        dialog.dismiss();
                    }
                });

                alert.setNegativeButton(mContext.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        } else
            downloadEpisode(position, episode);
    }

    private void downloadEpisode(final int position, final PodcastItem episode)
    {
            showToast(mContext, mContext.getString(R.string.alert_episode_download_start));

            long downloadId = Utilities.startDownload(mContext, episode);
            mEpisodes.get(position).setIsDownloaded(true);
            notifyItemChanged(position);
            //final DownloadProgress prog = new DownloadProgress(episode, position, downloadId);
            //mDownloadProgressHandler.postDelayed(prog, 1000);
    }

    private void showContext(final int position) {
        final PodcastItem episode = mEpisodes.get(position);
        if (episode.getIsSelected()) {
            episode.setIsSelected(false);
            mSelectedEpisodes.remove(episode);
        } else {
            episode.setIsSelected(true);
            mSelectedEpisodes.add(episode);
        }
        mEpisodeSelectedCallback.onEpisodeSelected(mSelectedEpisodes);
        mSwipeRefreshLayout.setEnabled(mSelectedEpisodes.size() == 0);
        notifyItemChanged(position);
    }

    private void openEpisode(final int position)
    {
        final Intent intent = new Intent(mContext, EpisodeActivity.class);

        final Bundle bundle = new Bundle();
        bundle.putInt("episodeid", mEpisodes.get(position).getEpisodeId());
        bundle.putInt("podcastid", mEpisodes.get(position).getPodcastId());

        if (mEpisodes.get(position).getIsLocal())
            bundle.putString("local_file", mEpisodes.get(position).getTitle());

        intent.putExtras(bundle);

        if (position > 0)
            mContext.startActivity(intent);
    }

    public void refreshList(final List<PodcastItem> episodes)
    {
        mEpisodes = episodes;
        notifyDataSetChanged();
        //List<PodcastItem> episodes = GetEpisodes(mContext, podcastId, mPlaylistId, hidePlayed, numberOfEpisode, null);
        //DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new EpisodesDiffCallback(this.mEpisodes, episodes));
        //diffResult.dispatchUpdatesTo(this);
    }

    public void refreshItem(final List<PodcastItem> episodes, final int position)
    {
        mEpisodes = episodes;
        notifyItemChanged(position);
    }

    public void refreshItem(final int position)
    {
        notifyItemChanged(position);
    }

    public void refreshList(final int podcastId)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        final boolean hidePlayed = prefs.getBoolean("pref_" + podcastId + "_hide_played", false);
        final int numberOfEpisode = Integer.valueOf(prefs.getString("pref_episode_limit", mContext.getString(R.string.episode_list_default)));

        refreshList(GetEpisodes(mContext, podcastId, 0, hidePlayed, numberOfEpisode, null));
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {

        final PodcastItem episode = mEpisodes.get(position);
        final TextView title = viewHolder.title;
        final TextView date = viewHolder.date;
        final TextView duration = viewHolder.duration;
        final ImageView thumbTitle = viewHolder.thumbnailTitle;
        final ImageView download = viewHolder.download;
        final RelativeLayout layout = viewHolder.layout;
        final ViewGroup.MarginLayoutParams paramsLayout = (ViewGroup.MarginLayoutParams) viewHolder.layout.getLayoutParams();

        if (episode.getIsDownloaded() || Utilities.getDownloadId(mContext, episode.getEpisodeId()) > 0)
            download.setImageDrawable(mContext.getDrawable(R.drawable.ic_action_episode_row_item_download_delete));
        else
            download.setImageDrawable(mContext.getDrawable(R.drawable.ic_action_episode_row_item_download));

        //download.setVisibility((mPlaylistId == mPlaylistDefault || mPlaylistId == mPlaylistDownloads) ? View.VISIBLE : View.GONE);
        title.setTextColor(mTextColor);

        if (episode.getIsTitle()) //TITLE
        {
            title.setVisibility(View.GONE);
            date.setVisibility(View.GONE);
            download.setVisibility(View.GONE);
            duration.setVisibility(View.GONE);
            thumbTitle.setVisibility(View.VISIBLE);
            thumbTitle.setMaxWidth(Utilities.getThumbMaxWidth(mContext, mDensityName, isRound));

            if (episode.getDisplayThumbnail() != null) //thumbnail row
                thumbTitle.setImageDrawable(episode.getDisplayThumbnail());
            else
                thumbTitle.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_logo_placeholder));

            title.setPadding(0, 0, 0, 0);

            layout.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
            title.setBackgroundColor(mContext.getColor(R.color.wc_transparent));

            if (isHDPI) {
                if (isRound)
                    paramsLayout.setMargins(0, 5, 0, 0);
                else
                    paramsLayout.setMargins(0, 10, 0, 0);
            } else if (isXHDPI)
                paramsLayout.setMargins(0, 5, 0, 0);
            else
                paramsLayout.setMargins(0, 10, 0, 0);

        } else //EPISODE
        {
            title.setVisibility(View.VISIBLE);
            date.setVisibility(View.VISIBLE);
            download.setVisibility(View.VISIBLE);
            thumbTitle.setVisibility(View.GONE);

            if (episode.getDuration() > 0) {
                duration.setText(episode.getDisplayDuration());
                duration.setVisibility(View.VISIBLE);
                duration.setTextColor(mTextColor);
            }
            else
                duration.setVisibility(View.GONE);

            title.setTextColor(mTextColor);
            title.setTextSize(14);
            title.setGravity(Gravity.START);

            date.setTextColor(mTextColor);
            date.setText(episode.getDisplayDate());

            if (!episode.getFinished()) {
                final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
                final SpannableString spannable = new SpannableString(episode.getTitle());
                spannable.setSpan(boldSpan, 0, episode.getTitle().length(), 0);
                title.setText(spannable);
            } else {
                final SpannableString titleText = new SpannableString(episode.getTitle());
                titleText.setSpan(new StyleSpan(Typeface.NORMAL), 0, titleText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                title.setText(titleText);
            }

            title.setPadding(0, 0, 40, 0);

            if (episode.getIsSelected()) {
                layout.setBackgroundColor(mContext.getColor(R.color.wc_episode_selected));
                title.setBackgroundColor(mContext.getColor(R.color.wc_episode_selected));
                duration.setBackgroundColor(mContext.getColor(R.color.wc_episode_selected));
                date.setBackgroundColor(mContext.getColor(R.color.wc_episode_selected));
                download.setBackgroundColor(mContext.getColor(R.color.wc_episode_selected));
            } else {
                layout.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
                title.setBackgroundColor(mContext.getColor(R.color.wc_transparent));

                duration.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
                date.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
                download.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
            }

            final int topMarginEpisodes = isRound ? 50 : 30;

            if (isHDPI) {
                if (isRound)
                    paramsLayout.setMargins(30, topMarginEpisodes, 40, 0);
                else
                    paramsLayout.setMargins(15, topMarginEpisodes, 15, 0);

            } else if (isXHDPI) {
                paramsLayout.setMargins(30, topMarginEpisodes, 30, 0);
            } else {
                paramsLayout.setMargins(15, topMarginEpisodes, 15, 0);
            }

            if (mSelectedEpisodes.size() > 0)
            {
                title.setOnClickListener(null);
                date.setOnClickListener(null);
            }
            else
            {
                title.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openEpisode(position);
                    }
                });

                date.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openEpisode(position);
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return mEpisodes.size();
    }
}