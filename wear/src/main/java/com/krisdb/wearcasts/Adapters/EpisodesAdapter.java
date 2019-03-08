package com.krisdb.wearcasts.Adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.krisdb.wearcasts.Activities.PodcastEpisodeActivity;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Settings.SettingsPodcastActivity;
import com.krisdb.wearcasts.Utilities.DBUtilities;
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
    private int mPlaylistId, mPlaylistDefault, mPlaylistDownloads, mPlaylistLocal, mTextColor, mHeaderColor;
    private String mDensityName;
    private Resources mResources;
    private boolean isRound, isXHDPI, isHDPI;
    private WeakReference<Activity> mActivityRef;
    private Interfaces.OnEpisodeSelectedListener mEpisodeSelectedCallback;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    static class ViewHolder extends WearableRecyclerView.ViewHolder {

        private final TextView title, date, duration;
        private final ImageView thumbnail, thumbnailTitle, download;
        private final RelativeLayout layout;
        private final ProgressBar progressEpisode;

        ViewHolder(final View view) {
            super(view);
            title = view.findViewById(R.id.episode_row_item_title);
            date = view.findViewById(R.id.episode_row_item_date);
            duration = view.findViewById(R.id.episode_row_item_duration);
            thumbnail = view.findViewById(R.id.episode_row_item_thumbnail);
            thumbnailTitle = view.findViewById(R.id.episode_row_item_title_thumbnail);
            download = view.findViewById(R.id.episode_row_item_download);
            layout = view.findViewById(R.id.episode_row_item_layout);
            progressEpisode = view.findViewById(R.id.episode_row_item_progress);
        }
    }

    public EpisodesAdapter(final Activity context, final List<PodcastItem> episodes, final int playlistId, final int textColor, final int headerColor, final SwipeRefreshLayout refreshLayout, final Interfaces.OnEpisodeSelectedListener episodeSelectedCallback) {
        mEpisodes = episodes;
        mContext = context;
        mTextColor = textColor;
        mPlaylistId = playlistId;
        mResources = mContext.getResources();
        mPlaylistDefault = mResources.getInteger(R.integer.playlist_default);
        mPlaylistDownloads = mResources.getInteger(R.integer.playlist_downloads);
        mPlaylistLocal = mResources.getInteger(R.integer.playlist_local);
        mDensityName = CommonUtils.getDensityName(mContext);
        isRound = mResources.getConfiguration().isScreenRound();
        mHeaderColor = headerColor;
        mActivityRef = new WeakReference<>(mContext);
        mEpisodeSelectedCallback = episodeSelectedCallback;
        mSelectedEpisodes = new ArrayList<>();
        isXHDPI = Objects.equals(mDensityName, mContext.getString(R.string.xhdpi));
        isHDPI = Objects.equals(mDensityName, mContext.getString(R.string.hdpi));
        mSwipeRefreshLayout = refreshLayout;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int viewType) {
        final View view = LayoutInflater.from(viewGroup.getContext()).inflate(mPlaylistId == mPlaylistDefault ? R.layout.episode_row_item : R.layout.playlist_row_item, viewGroup, false);

        final ViewHolder holder = new ViewHolder(view);

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

        if (holder.thumbnail!= null) {
            holder.thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openEpisode(holder.getAdapterPosition());
                }
            });
            holder.thumbnail.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    showContext(holder.getAdapterPosition());
                    return false;
                }
            });
        }

        holder.title.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showContext(holder.getAdapterPosition());
                return false;
            }
        });

        holder.date.setOnLongClickListener(new View.OnLongClickListener() {
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
        //holder.progressEpisodeDownload.setVisibility(View.VISIBLE);

        if (mPlaylistId == mPlaylistLocal && mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
            final AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
            alert.setMessage(mContext.getString(R.string.confirm_delete_file));
            alert.setPositiveButton(mContext.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                int position = holder.getAdapterPosition();

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Utilities.deleteLocal(mContext, mEpisodes.get(position).getTitle());
                    refreshList(mEpisodes.get(position).getPodcastId());
                }
            });
            alert.setNegativeButton(mContext.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alert.show();
            return;
        }

        int episodeId = mEpisodes.get(position).getEpisodeId();

        final PodcastItem episode = GetEpisode(mContext, episodeId, mPlaylistId);

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

                        if (mPlaylistId == mPlaylistDownloads)
                            refreshList(mEpisodes.get(position).getPodcastId());
                        else {
                            mEpisodes.get(position).setIsDownloaded(false);
                            notifyItemChanged(position);
                            dialog.dismiss();
                        }
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

    private void showContext(final int position)
    {
        if (mPlaylistId == mResources.getInteger(R.integer.playlist_default))
        {
            final PodcastItem episode = mEpisodes.get(position);
            if (episode.getIsSelected())
            {
                episode.setIsSelected(false);
                mSelectedEpisodes.remove(episode);
            }
            else
            {
                episode.setIsSelected(true);
                mSelectedEpisodes.add(episode);
            }

            notifyItemChanged(position);

            mSwipeRefreshLayout.setEnabled(mSelectedEpisodes.size() == 0);
            mEpisodeSelectedCallback.onEpisodeSelected(mSelectedEpisodes);
            /*
            final Intent intent = new Intent(mContext, EpisodeContextActivity.class);
            final Bundle bundle = new Bundle();
            bundle.putInt("episodeid", mEpisodes.get(position).getEpisodeId());
            intent.putExtras(bundle);
            mContext.startActivity(intent);
            */
            return;
        }

        if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
            final AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
            if (mPlaylistId == mResources.getInteger(R.integer.playlist_inprogress))
                alert.setMessage(mContext.getString(R.string.confirm_mark_finished));
            else if (mPlaylistId == mPlaylistDownloads)
                alert.setMessage(mContext.getString(R.string.confirm_delete_download));
            else if (mPlaylistId > mResources.getInteger(R.integer.playlist_default))
                alert.setMessage(mContext.getString(R.string.confirm_remove_upnext));
            else if (mPlaylistId == mPlaylistLocal || mPlaylistId <= mResources.getInteger(R.integer.playlist_playerfm))
                alert.setMessage(mContext.getString(R.string.confirm_remove_local));
            else
                alert.setMessage(mEpisodes.get(position).getFinished() ? mContext.getString(R.string.confirm_mark_unplayed) : mContext.getString(R.string.confirm_mark_played));

            alert.setPositiveButton(mContext.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                    final DBPodcastsEpisodes db = new DBPodcastsEpisodes(mContext);

                    if (mPlaylistId == mResources.getInteger(R.integer.playlist_inprogress)) //in progress
                    {
                        final ContentValues cv = new ContentValues();
                        cv.put("position", 0);
                        cv.put("finished", 1);

                        db.update(cv, mEpisodes.get(position).getEpisodeId());
                    } else if (mPlaylistId == mPlaylistDownloads)
                        Utilities.DeleteMediaFile(mContext, mEpisodes.get(position));
                    else if (mPlaylistId == mResources.getInteger(R.integer.playlist_radio))
                        db.delete(mEpisodes.get(position).getEpisodeId());
                    else if (mPlaylistId <= mResources.getInteger(R.integer.playlist_playerfm)) {
                        db.deleteEpisodeFromPlaylist(mPlaylistId, mEpisodes.get(position).getEpisodeId());
                        db.delete(mEpisodes.get(position).getEpisodeId());
                    } else if (mPlaylistId > mResources.getInteger(R.integer.playlist_default))
                        db.deleteEpisodeFromPlaylist(mPlaylistId, mEpisodes.get(position).getEpisodeId());
                    else if (mPlaylistId == mPlaylistLocal)
                        Utilities.deleteLocal(mContext, mEpisodes.get(position).getTitle());
                    else
                        SaveEpisodeValue(mContext, mEpisodes.get(position), "finished", mEpisodes.get(position).getFinished() ? 0 : 1);

                    if (mPlaylistId == mResources.getInteger(R.integer.playlist_default)) {
                        mEpisodes.get(position).setFinished(mEpisodes.get(position).getFinished() ? false : true);
                        notifyItemChanged(position);
                    } else
                        refreshList(mEpisodes.get(position).getPodcastId());

                    db.close();
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

    private void openEpisode(final int position)
    {
        final Intent intent = new Intent(mContext, PodcastEpisodeActivity.class);

        final Bundle bundle = new Bundle();
        bundle.putInt("eid", mEpisodes.get(position).getEpisodeId());

        if (mEpisodes.get(position).getIsLocal())
            bundle.putString("local_file", mEpisodes.get(position).getTitle());

        bundle.putInt("playlistid", mPlaylistId);
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

        refreshList(GetEpisodes(mContext, podcastId, mPlaylistId, hidePlayed, numberOfEpisode, null));
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {

        final PodcastItem episode = mEpisodes.get(position);
        final TextView title = viewHolder.title;
        final TextView date = viewHolder.date;
        final TextView duration = viewHolder.duration;
        final ImageView thumbTitle = viewHolder.thumbnailTitle;
        final ImageView thumb = viewHolder.thumbnail;
        final ImageView download = viewHolder.download;
        final RelativeLayout layout = viewHolder.layout;
        final ProgressBar episodeProgress = viewHolder.progressEpisode;
        final ViewGroup.MarginLayoutParams paramsLayout = (ViewGroup.MarginLayoutParams)viewHolder.layout.getLayoutParams();

        if ((mPlaylistId == mPlaylistLocal) || episode.getIsDownloaded() || Utilities.getDownloadId(mContext, episode.getEpisodeId()) > 0)
            download.setImageDrawable(mContext.getDrawable(R.drawable.ic_action_episode_row_item_download_delete));
        else
            download.setImageDrawable(mContext.getDrawable(R.drawable.ic_action_episode_row_item_download));

        //download.setVisibility((mPlaylistId == mPlaylistDefault || mPlaylistId == mPlaylistDownloads) ? View.VISIBLE : View.GONE);
        title.setTextColor(mTextColor);

        if (episode.getIsTitle()) //TITLE
        {
            title.setPadding(0, 0, 0 ,0 );
            download.setVisibility(View.GONE);

            if (episodeProgress != null)
                episodeProgress.setVisibility(View.GONE);

            if (mPlaylistId != mPlaylistDefault)
            {
                final SpannableString titleText = new SpannableString(episode.getChannel().getTitle());
                titleText.setSpan(new StyleSpan(Typeface.BOLD), 0, titleText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                title.setText(titleText);

                title.setTextSize(16);
                title.setGravity(Gravity.CENTER_HORIZONTAL);
                title.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                title.setVisibility(View.VISIBLE);
            }
            else
                title.setVisibility(View.GONE);

            if (mPlaylistId == mPlaylistDefault) //normal episode title image
            {
                if (isHDPI) {
                    if (isRound)
                        paramsLayout.setMargins(0, 5, 0, 0);
                    else
                        paramsLayout.setMargins(0, 10, 0, 0);
                }
                else if (isXHDPI)
                    paramsLayout.setMargins(0, 5, 0, 0);
                else
                    paramsLayout.setMargins(0, 10, 0, 0);
            }
            else //playlist title
            {
                if (isHDPI) {
                    if (isRound)
                        paramsLayout.setMargins(0, 0, 0, 70);
                    else
                        paramsLayout.setMargins(0, 0, 0, 60);
                }
                else if (isXHDPI) {
                    if (isRound)
                        paramsLayout.setMargins(0, 0, 0, 35);
                    else
                        paramsLayout.setMargins(0, 0, 0, 35);
                }
                else
                    paramsLayout.setMargins(0, 0, 0, 35);
            }

            thumbTitle.setVisibility(View.VISIBLE);
            thumbTitle.setMaxWidth(Utilities.getThumbMaxWidth(mContext, mDensityName, isRound));
            duration.setVisibility(View.GONE);
            date.setVisibility(View.GONE);

            if (thumb != null)
                thumb.setVisibility(View.GONE);

            if (mPlaylistId == mPlaylistDefault) {
                if (episode.getDisplayThumbnail() != null) //thumbnail row
                    thumbTitle.setImageDrawable(episode.getDisplayThumbnail());
                else
                    thumbTitle.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_thumb_title_default));

                layout.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
                title.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
            }
            else {
                layout.setBackgroundColor(mHeaderColor);
                title.setBackgroundColor(mHeaderColor);
            }
        }
        else //EPISODE
        {
            date.setTextColor(mTextColor);
            duration.setTextColor(mTextColor);
            title.setPadding(0, 0, 40 ,0 );
            download.setVisibility(View.VISIBLE);

            if (episode.getIsSelected()) {
                layout.setBackgroundColor(mContext.getColor(R.color.wc_episode_selected));
                title.setBackgroundColor(mContext.getColor(R.color.wc_episode_selected));
                duration.setBackgroundColor(mContext.getColor(R.color.wc_episode_selected));
                date.setBackgroundColor(mContext.getColor(R.color.wc_episode_selected));
                download.setBackgroundColor(mContext.getColor(R.color.wc_episode_selected));
            }
            else {
                layout.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
                title.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
                duration.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
                date.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
                download.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
            }

            final int topMarginPlaylists = isXHDPI ? -85 : -100;
            if (episodeProgress != null) {
                if (episode.getPosition() > 0) {
                    episodeProgress.setVisibility(View.VISIBLE);
                    episodeProgress.setMax(episode.getDuration());
                    episodeProgress.setProgress(episode.getPosition());
                } else {
                    episodeProgress.setVisibility(View.INVISIBLE);
                }
            }

            if (mPlaylistId == mPlaylistDefault && episode.getDuration() > 0) {
                duration.setText(episode.getDisplayDuration());
                duration.setVisibility(View.VISIBLE);
            }
            else
                duration.setVisibility(View.GONE);

            thumbTitle.setVisibility(View.GONE);
            if (thumb != null)
                thumb.setVisibility(episode.getIsLocal() ? View.GONE : View.VISIBLE);

            if (episodeProgress != null) {
                if (mPlaylistId == mPlaylistDefault)
                    episodeProgress.setVisibility(View.GONE);
                else if (episode.getPosition() > 0)
                    episodeProgress.setVisibility(View.VISIBLE);
            }

            if (mPlaylistId == mPlaylistDefault) { //episode listing
                final int topMarginEpisodes = isRound ? 50 : 30;

                if (isHDPI) {
                    if (isRound)
                        paramsLayout.setMargins(30, topMarginEpisodes, 40, 0);
                    else
                        paramsLayout.setMargins(15, topMarginEpisodes, 15, 0);

                    if (thumb != null)
                        thumb.setMaxWidth((int) mContext.getResources().getDimension(R.dimen.thumb_width_playlist_list_hdpi));
                } else if (isXHDPI) {
                    paramsLayout.setMargins(30, topMarginEpisodes, 30, 0);
                    if (thumb != null)
                        thumb.setMaxWidth((int) mContext.getResources().getDimension(R.dimen.thumb_width_playlist_list_xhdpi));
                } else {
                    paramsLayout.setMargins(15, topMarginEpisodes, 15, 0);
                    if (thumb != null)
                        thumb.setMaxWidth((int) mContext.getResources().getDimension(R.dimen.thumb_width_playlist_list_default));
                }
            }
            else //playlist listing
            {
                if (isHDPI) {
                    if (isRound)
                        paramsLayout.setMargins(35, topMarginPlaylists, 35, 0);
                    else
                        paramsLayout.setMargins(15, topMarginPlaylists, 15, 0);

                    if (thumb != null)
                        thumb.setMaxWidth((int) mContext.getResources().getDimension(R.dimen.thumb_width_playlist_list_hdpi));
                } else if (isXHDPI) {
                    paramsLayout.setMargins(45, topMarginPlaylists, 45, 0);
                    if (thumb != null)
                        thumb.setMaxWidth((int) mContext.getResources().getDimension(R.dimen.thumb_width_playlist_list_xhdpi));
                } else {
                    paramsLayout.setMargins(45, topMarginPlaylists, 45, 0);
                    if (thumb != null)
                        thumb.setMaxWidth((int) mContext.getResources().getDimension(R.dimen.thumb_width_playlist_list_default));
                }
            }

            if (thumb != null) {
                if (mPlaylistId != mPlaylistDefault) //thumbnail row
                {
                    if (episode.getDisplayThumbnail() != null)
                        thumb.setImageDrawable(episode.getDisplayThumbnail());
                    else
                        thumb.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_thumb_playlist_default));

                    date.setVisibility(View.GONE);
                } else
                    thumb.setVisibility(View.GONE);
            }
            if (mPlaylistId == mPlaylistDefault) {
                date.setText(episode.getDisplayDate());
                date.setVisibility(View.VISIBLE);
            }

             if (episode.getFinished() == false && mPlaylistId == mPlaylistDefault) {
                 final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
                 final SpannableString spannable = new SpannableString(episode.getTitle());
                 spannable.setSpan(boldSpan, 0, episode.getTitle().length(), 0);
                 title.setText(spannable);
             }
            else {
                 final SpannableString titleText = new SpannableString(episode.getTitle());
                 titleText.setSpan(new StyleSpan(Typeface.NORMAL), 0, titleText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                 title.setText(titleText);
             }

            title.setVisibility(View.VISIBLE);
            title.setTextSize(14);
            title.setGravity(Gravity.START);
            title.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
        }
    }

    @Override
    public int getItemCount() {
        return mEpisodes.size();
    }

}