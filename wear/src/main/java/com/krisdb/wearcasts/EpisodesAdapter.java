package com.krisdb.wearcasts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
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

import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;


public class EpisodesAdapter extends WearableRecyclerView.Adapter<EpisodesAdapter.ViewHolder> {

    private List<PodcastItem> mEpisodes;
    private Activity mContext;
    private int mPlaylistId, mPlaylistDefault, mPlaylistDownloads, mPlaylistLocal, mPlaylistRadio, mTextColor, mHeaderColor;
    private String mDensityName;
    private Resources mResources;
    private boolean isRound;
    private WeakReference<Activity> mActivityRef;
    //private Handler mDownloadProgressHandler = new Handler();
    private DownloadManager mDownloadManager;

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
            progressEpisode = view.findViewById(R.id.episode_progress);
        }
    }

    EpisodesAdapter(final Activity context, final List<PodcastItem> episodes, final int playlistId, final int textColor, final int headerColor) {
        mEpisodes = episodes;
        mContext = context;
        mTextColor = textColor;
        mPlaylistId = playlistId;
        mResources = mContext.getResources();
        mPlaylistDefault = mResources.getInteger(R.integer.playlist_default);
        mPlaylistRadio = mResources.getInteger(R.integer.playlist_radio);
        mPlaylistDownloads = mResources.getInteger(R.integer.playlist_downloads);
        mPlaylistLocal = mResources.getInteger(R.integer.playlist_local);
        mDensityName = CommonUtils.getDensityName(mContext);
        isRound = mResources.getConfiguration().isScreenRound();
        mHeaderColor = headerColor;
        mActivityRef = new WeakReference<>(mContext);
        mDownloadManager = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int viewType) {
        final View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.episode_row_item, viewGroup, false);

        final ViewHolder holder = new ViewHolder(view);

        holder.thumbnailTitle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                final PodcastItem podcast = DBUtilities.GetPodcast(mContext, mEpisodes.get(holder.getAdapterPosition()).getPodcastId());

                if (podcast.getChannel().getThumbnailUrl() != null) {
                    CommonUtils.showToast(mContext, mContext.getString(R.string.alert_refreshing_thumb));
                    new AsyncTasks.SaveLogo(mContext, podcast.getChannel().getThumbnailUrl().toString(), podcast.getChannel().getThumbnailName(), true,
                            new Interfaces.AsyncResponse() {
                                @Override
                                public void processFinish() {
                                    notifyItemChanged(0);
                                }
                            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                }

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

        holder.thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openEpisode(holder.getAdapterPosition());
            }
        });

        holder.title.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showContext(holder.getAdapterPosition());
                return false;
            }
        });

        holder.thumbnail.setOnLongClickListener(new View.OnLongClickListener() {
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
                    deleteLocal(position);
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

        final PodcastItem episode = DBUtilities.GetEpisode(mContext, episodeId, mPlaylistId);

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

                        DBUtilities.SaveEpisodeValue(mContext, episode, "downloadid", 0);
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

    private class DownloadProgress implements Runnable
    {
        private PodcastItem episode;
        private int position;
        private long downloadid;

        public DownloadProgress(final PodcastItem episode, final int position, final long downloadId)
        {
            this.episode = episode;
            this.position = position;
            this.downloadid = downloadId;

        }
        @Override
        public void run() {
            final DownloadManager.Query query = new DownloadManager.Query();
            //query.setFilterById(mDownloadId);
            final Cursor cursor = mDownloadManager.query(query);
            final DownloadProgress prog = new DownloadProgress(episode, position, downloadid);

            if (cursor.moveToFirst()) {
                final int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                final int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                final int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));

                //Log.d(mContext.getPackageName(), "Status: " + status);
                //Log.d(mContext.getPackageName(), "Bytes: "  +bytes_total);

                switch (status) {
                    case DownloadManager.STATUS_PAUSED:
                    case DownloadManager.STATUS_RUNNING:
                    case DownloadManager.STATUS_PENDING:
                        final int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        //mDownloadProgressHandler.postDelayed(prog, 1000);
                        break;
                    case DownloadManager.STATUS_FAILED:
                        //mDownloadManager.remove(mDownloadId);
                        //mDownloadProgressHandler.removeCallbacksAndMessages(prog);
                    case DownloadManager.STATUS_SUCCESSFUL:
                        //mDownloadProgressHandler.removeCallbacksAndMessages(prog);
                }
            }
            cursor.close();
        }
    }

    private void showContext(final int position)
    {
        if (mPlaylistId == mResources.getInteger(R.integer.playlist_default))
        {
            final Intent intent = new Intent(mContext, EpisodeContextActivity.class);
            final Bundle bundle = new Bundle();
            bundle.putInt("episodeid", mEpisodes.get(position).getEpisodeId());
            intent.putExtras(bundle);
            mContext.startActivity(intent);
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
                        deleteLocal(position);
                    else
                        DBUtilities.SaveEpisodeValue(mContext, mEpisodes.get(position), "finished", mEpisodes.get(position).getFinished() ? 0 : 1);

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

    private void deleteLocal(final int position)
    {
        final File localFile = new File(GetLocalDirectory().concat(mEpisodes.get(position).getTitle()));

        if (localFile.exists())
            localFile.delete();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final SharedPreferences.Editor editor = prefs.edit();

        editor.remove(Utilities.GetLocalDurationKey(mEpisodes.get(position).getTitle()));
        editor.remove(Utilities.GetLocalPositionKey(mEpisodes.get(position).getTitle()));
        editor.apply();
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
        //List<PodcastItem> episodes = DBUtilities.GetEpisodes(mContext, podcastId, mPlaylistId, hidePlayed, numberOfEpisode, null);
        //DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new EpisodesDiffCallback(this.mEpisodes, episodes));
        //diffResult.dispatchUpdatesTo(this);
    }

    public void refreshList(final int podcastId)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        final Boolean hidePlayed = prefs.getBoolean("pref_" + podcastId + "_hide_played", false);
        final int numberOfEpisode = Integer.valueOf(prefs.getString("pref_episode_limit", mContext.getString(R.string.episode_list_default)));

        refreshList(DBUtilities.GetEpisodes(mContext, podcastId, mPlaylistId, hidePlayed, numberOfEpisode, null));
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
        final ViewGroup.MarginLayoutParams paramsLayout = (ViewGroup.MarginLayoutParams)viewHolder.layout.getLayoutParams();
        final ViewGroup.MarginLayoutParams paramsDate = (ViewGroup.MarginLayoutParams)date.getLayoutParams();

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
            viewHolder.progressEpisode.setVisibility(View.GONE);

            if (mPlaylistId != mPlaylistDefault)
            {
                final SpannableString titleText = new SpannableString(episode.getChannel().getTitle());
                titleText.setSpan(new StyleSpan(Typeface.BOLD), 0, titleText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                title.setText(titleText);

                title.setTextSize(16);
                title.setGravity(Gravity.CENTER_HORIZONTAL);
                title.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                //title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                title.setVisibility(View.VISIBLE);
            }
            else
                title.setVisibility(View.GONE);

            if (mPlaylistId == mPlaylistDefault) //normal episode title image
            {
                if (Objects.equals(mDensityName, mContext.getString(R.string.hdpi))) {
                    if (isRound)
                        paramsLayout.setMargins(0, 20, 0, 0);
                    else
                        paramsLayout.setMargins(0, 10, 0, 0);

                }
                else if (Objects.equals(mDensityName, mContext.getString(R.string.xhdpi))) {
                    paramsLayout.setMargins(0, 20, 0, 0);
                }
                else
                    paramsLayout.setMargins(0, 20, 0, 0);
            }
            else //playlist title
            {
                if (Objects.equals(mDensityName, mContext.getString(R.string.hdpi))) {
                    if (isRound) {
                        layout.setPadding(0, 1, 0, 1);
                        paramsLayout.setMargins(0, 0, 0, 10);
                    }
                    else {
                        layout.setPadding(0, 0, 0, 0);
                        paramsLayout.setMargins(0, 0, 0, 10);
                    }
                }
                else if (Objects.equals(mDensityName, mContext.getString(R.string.xhdpi))) {
                    if (isRound) {
                        layout.setPadding(0, 2, 0, 10);
                        paramsLayout.setMargins(0, 0, 0, 0);
                    }
                    else
                    {
                        layout.setPadding(0, 5, 0, 10);
                        paramsLayout.setMargins(0, 0, 0, 0);
                    }
                }
                else
                {
                    layout.setPadding(0, 0, 0, 0);
                    paramsLayout.setMargins(0, 0, 0, 0);
                }
            }

            thumbTitle.setVisibility(View.VISIBLE);
            thumbTitle.setMaxWidth(Utilities.getThumbMaxWidth(mContext, mDensityName, isRound));
            duration.setVisibility(View.GONE);
            date.setVisibility(View.GONE);
            thumb.setVisibility(View.GONE);

            if (mPlaylistId == mPlaylistDefault) {
                if (episode.getDisplayThumbnail() != null) //thumbnail row
                    thumbTitle.setImageDrawable(episode.getDisplayThumbnail());
                else
                    thumbTitle.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_thumb_title_default));
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

            if (episode.getPosition() > 0)
            {
                viewHolder.progressEpisode.setVisibility(View.GONE);
                viewHolder.progressEpisode.setMax(episode.getDuration());
                viewHolder.progressEpisode.setProgress(episode.getPosition());
            }
            else
                viewHolder.progressEpisode.setVisibility(View.GONE);

            if (mPlaylistId == mPlaylistDefault && episode.getDuration() > 0) {
                duration.setText(episode.getDisplayDuration());
                duration.setVisibility(View.VISIBLE);
            }
            else
                duration.setVisibility(View.GONE);

            layout.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
            title.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
            thumbTitle.setVisibility(View.GONE);
            thumb.setVisibility(episode.getIsLocal() ? View.GONE : View.VISIBLE);

            if (mPlaylistId == mPlaylistDefault)
                viewHolder.progressEpisode.setVisibility(View.GONE);
            else if (episode.getPosition() > 0)
                viewHolder.progressEpisode.setVisibility(View.VISIBLE);

            if (mPlaylistId == mPlaylistDefault) { //episode listing
                if (Objects.equals(mDensityName, mContext.getString(R.string.hdpi))) {
                    if (isRound)
                        paramsLayout.setMargins(45, 0, 45, 20);
                    else
                        paramsLayout.setMargins(20, 0, 10, 20);

                    layout.setPadding(0, 0, 0, 0);
                    paramsDate.setMargins(0, 20, 0, 0);
                    thumb.setMaxWidth((int) mContext.getResources().getDimension(R.dimen.thumb_width_playlist_list_hdpi));
                } else if (Objects.equals(mDensityName, mContext.getString(R.string.xhdpi))) {
                    paramsLayout.setMargins(60, 0, 40, 20);
                    paramsDate.setMargins(0, 30, 0, 10);
                    thumb.setMaxWidth((int) mContext.getResources().getDimension(R.dimen.thumb_width_playlist_list_xhdpi));
                } else {
                    paramsLayout.setMargins(20, 0, 20, 20);
                    paramsDate.setMargins(0, 30, 0, 20);
                    thumb.setMaxWidth((int) mContext.getResources().getDimension(R.dimen.thumb_width_playlist_list_default));
                }
            }
            else //playlist listing
            {
                if (Objects.equals(mDensityName, mContext.getString(R.string.hdpi))) {
                    if (isRound)
                        paramsLayout.setMargins(40, 0, 40, 30);
                    else
                        paramsLayout.setMargins(20, 0, 20, 30);

                    layout.setPadding(0, 0, 0, 0);
                    paramsDate.setMargins(0, 30, 0, 0);
                    thumb.setMaxWidth((int) mContext.getResources().getDimension(R.dimen.thumb_width_playlist_list_hdpi));
                } else if (Objects.equals(mDensityName, mContext.getString(R.string.xhdpi))) {
                    layout.setPadding(0, 0, 0, 0);
                    paramsLayout.setMargins(70, 0, 30, 30);
                    thumb.setMaxWidth((int) mContext.getResources().getDimension(R.dimen.thumb_width_playlist_list_xhdpi));
                    paramsDate.setMargins(0, 20, 0, 0);
                } else {
                    layout.setPadding(0, 0, 0, 0);
                    paramsLayout.setMargins(20, 60, 20, 0);
                    paramsDate.setMargins(0, 20, 0, 0);
                    thumb.setMaxWidth((int) mContext.getResources().getDimension(R.dimen.thumb_width_playlist_list_default));
                }
            }

            if (mPlaylistId != mPlaylistDefault) //thumbnail row
            {
                if (episode.getDisplayThumbnail() != null)
                    thumb.setImageDrawable(episode.getDisplayThumbnail());
                else
                    thumb.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_thumb_playlist_default));

                date.setVisibility(View.GONE);
            } else
                thumb.setVisibility(View.GONE);

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