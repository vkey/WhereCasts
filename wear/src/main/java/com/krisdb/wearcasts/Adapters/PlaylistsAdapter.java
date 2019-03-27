package com.krisdb.wearcasts.Adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.krisdb.wearcasts.Activities.EpisodeActivity;
import com.krisdb.wearcasts.Databases.DBPodcastsEpisodes;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.wear.widget.WearableRecyclerView;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.GetEpisode;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.SaveEpisodeValue;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.GetEpisodes;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;


public class PlaylistsAdapter extends WearableRecyclerView.Adapter<PlaylistsAdapter.ViewHolder> {

    private List<PodcastItem> mEpisodes;
    private Activity mContext;
    private int mPlaylistId, mPlaylistDownloads, mPlaylistLocal, mTextColor, mHeaderColor;
    private Resources mResources;
    private boolean isRound, isXHDPI, isHDPI;
    private static WeakReference<Activity> mActivityRef;

    private ConnectivityManager mManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private TimeOutHandler mTimeOutHandler;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(7);

    static class ViewHolder extends WearableRecyclerView.ViewHolder {

        private final TextView title;
        private final ImageView thumbnail,download;
        private final ConstraintLayout layout;
        private final ProgressBar progressEpisode;

        ViewHolder(final View view) {
            super(view);
            title = view.findViewById(R.id.playlist_row_item_title);
            thumbnail = view.findViewById(R.id.playlist_row_item_thumbnail);
            download = view.findViewById(R.id.playlist_row_item_download);
            layout = view.findViewById(R.id.playlist_row_item_layout);
            progressEpisode = view.findViewById(R.id.playlist_row_item_progress);
        }
    }

    public PlaylistsAdapter(final Activity context, final List<PodcastItem> episodes, final int playlistId, final int textColor, final int headerColor) {
        mEpisodes = episodes;
        mContext = context;
        mTextColor = textColor;
        mPlaylistId = playlistId;
        mResources = mContext.getResources();
        mPlaylistDownloads = mResources.getInteger(R.integer.playlist_downloads);
        mPlaylistLocal = mResources.getInteger(R.integer.playlist_local);
        isRound = mResources.getConfiguration().isScreenRound();
        mHeaderColor = headerColor;
        mActivityRef = new WeakReference<>(mContext);
        final String density = CommonUtils.getDensityName(mContext);
        isXHDPI = Objects.equals(density, mContext.getString(R.string.xhdpi));
        isHDPI = Objects.equals(density, mContext.getString(R.string.hdpi));
        mTimeOutHandler = new TimeOutHandler(this);
        mManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int viewType) {
        final View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.playlist_row_item, viewGroup, false);

        final ViewHolder holder = new ViewHolder(view);

        holder.download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initDownload(holder, holder.getAdapterPosition());
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
        holder.thumbnail.setOnLongClickListener(new View.OnLongClickListener() {
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
        //holder.progressEpisodeDownload.setVisibility(View.VISIBLE);

        if (mPlaylistId == mPlaylistLocal && mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
            final AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
            alert.setMessage(mContext.getString(R.string.confirm_delete_file));
            alert.setPositiveButton(mContext.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                int position = holder.getAdapterPosition();

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Utilities.deleteLocal(mContext, mEpisodes.get(position).getTitle());
                    refreshList();
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
        final SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(mContext);

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
                            refreshList();
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
        else if (prefs.getBoolean("initialDownload", true) && Utilities.BluetoothEnabled()) {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
                alert.setMessage(mContext.getString(R.string.confirm_initial_download_message));
                alert.setPositiveButton(mContext.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("pref_disable_bluetooth", true);
                        editor.apply();
                        downloadEpisode(position, episode);
                        dialog.dismiss();
                    }
                });
                alert.setNegativeButton(mContext.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadEpisode(position, episode);
                        dialog.dismiss();
                    }
                }).show();

                final SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("initialDownload", false);
                editor.apply();
            }
        }
        else if (prefs.getBoolean("pref_disable_bluetooth", false) && Utilities.BluetoothEnabled())
        {
            unregisterNetworkCallback();

            Utilities.disableBluetooth(mContext);

            CommonUtils.showToast(mContext, mContext.getString(R.string.alert_episode_network_waiting));

            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(final Network network) {
                    mTimeOutHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);
                    downloadEpisode(position, episode);
                }
            };

            final NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            mManager.requestNetwork(request, mNetworkCallback);

            mTimeOutHandler.sendMessageDelayed(
                    mTimeOutHandler.obtainMessage(MESSAGE_CONNECTIVITY_TIMEOUT),
                    NETWORK_CONNECTIVITY_TIMEOUT_MS);
        }
        else
            downloadEpisode(position, episode);
    }

    private static class TimeOutHandler extends Handler {
        private final WeakReference<PlaylistsAdapter> mActivityWeakReference;

        TimeOutHandler(final PlaylistsAdapter adapter) {
            mActivityWeakReference = new WeakReference<>(adapter);
        }

        @Override
        public void handleMessage(final Message msg) {
            final PlaylistsAdapter adapter = mActivityWeakReference.get();

            if (adapter != null) {
                switch (msg.what) {
                    case MESSAGE_CONNECTIVITY_TIMEOUT:
                        final Activity ctx = mActivityRef.get();

                        if (ctx != null && !ctx.isFinishing()) {
                            final AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
                            alert.setMessage(ctx.getString(R.string.alert_episode_network_notfound));
                            alert.setPositiveButton(ctx.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mActivityRef.get().startActivity(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent));
                                    dialog.dismiss();
                                }
                            });

                            alert.setNegativeButton(ctx.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Utilities.enableBluetooth(ctx);
                                    dialog.dismiss();
                                }
                            }).show();
                        }
                        adapter.unregisterNetworkCallback();
                        break;
                }
            }
        }
    }

    private void unregisterNetworkCallback() {
        if (mNetworkCallback != null) {
            mManager.unregisterNetworkCallback(mNetworkCallback);
            mManager.bindProcessToNetwork(null);
            mNetworkCallback = null;
        }
    }

    private void downloadEpisode(final int position, final PodcastItem episode) {
        showToast(mContext, mContext.getString(R.string.alert_episode_download_start));

        Utilities.startDownload(mContext, episode);
        mEpisodes.get(position).setIsDownloaded(true);
        notifyItemChanged(position);
    }

    private void showContext(final int position)
    {
        if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
            final AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
            if (mPlaylistId == mResources.getInteger(R.integer.playlist_inprogress))
                alert.setMessage(mContext.getString(R.string.confirm_mark_finished));
            else if (mPlaylistId == mPlaylistDownloads)
                alert.setMessage(mContext.getString(R.string.confirm_delete_download));
            else if (mPlaylistId == mPlaylistLocal || mPlaylistId <= mResources.getInteger(R.integer.playlist_playerfm))
                alert.setMessage(mContext.getString(R.string.confirm_remove_local));
            else if (mPlaylistId > -1)
                alert.setMessage(mContext.getString(R.string.confirm_remove_upnext));
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
                    } else if (mPlaylistId > -1)
                        db.deleteEpisodeFromPlaylist(mPlaylistId, mEpisodes.get(position).getEpisodeId());
                    else if (mPlaylistId == mPlaylistLocal)
                        Utilities.deleteLocal(mContext, mEpisodes.get(position).getTitle());
                    else
                        SaveEpisodeValue(mContext, mEpisodes.get(position), "finished", mEpisodes.get(position).getFinished() ? 0 : 1);

                    refreshList();

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
        final Intent intent = new Intent(mContext, EpisodeActivity.class);

        final Bundle bundle = new Bundle();
        bundle.putInt("episodeid", mEpisodes.get(position).getEpisodeId());

        if (mEpisodes.get(position).getIsLocal())
            bundle.putString("local_file", mEpisodes.get(position).getTitle());

        bundle.putInt("playlistid", mPlaylistId);
        bundle.putInt("podcastid", -1);
        intent.putExtras(bundle);

        if (position > 0)
            mContext.startActivity(intent);
    }

    public void refreshList(final List<PodcastItem> episodes)
    {
        mEpisodes = episodes;
        notifyDataSetChanged();
    }

    public void refreshList()
    {
        refreshList(GetEpisodes(mContext, mPlaylistId));
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {

        final PodcastItem episode = mEpisodes.get(position);
        final TextView title = viewHolder.title;
        final ImageView thumb = viewHolder.thumbnail;
        final ImageView download = viewHolder.download;
        final ConstraintLayout layout = viewHolder.layout;
        final ProgressBar episodeProgress = viewHolder.progressEpisode;
        final ViewGroup.MarginLayoutParams paramsLayout = (ViewGroup.MarginLayoutParams)viewHolder.layout.getLayoutParams();

        if ((mPlaylistId == mPlaylistLocal) || episode.getIsDownloaded() || Utilities.getDownloadId(mContext, episode.getEpisodeId()) > 0)
            download.setImageDrawable(mContext.getDrawable(R.drawable.ic_action_episode_row_item_download_delete));
        else
            download.setImageDrawable(mContext.getDrawable(R.drawable.ic_action_episode_row_item_download));

        title.setTextColor(mTextColor);

        if (episode.getIsTitle()) //TITLE
        {
            title.setText(episode.getChannel().getTitle());
            download.setVisibility(View.GONE);
            episodeProgress.setVisibility(View.GONE);
            thumb.setVisibility(View.GONE);

            final SpannableString titleText = new SpannableString(episode.getChannel().getTitle());
            titleText.setSpan(new StyleSpan(Typeface.BOLD), 0, titleText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            title.setText(titleText);

            title.setTextSize(16);
            title.setGravity(Gravity.CENTER_HORIZONTAL);

            if (isHDPI) {
                if (isRound)
                    paramsLayout.setMargins(0, 0, 0, 10);
                else
                    paramsLayout.setMargins(0, 0, 0, 10);
            } else if (isXHDPI) {
                if (isRound)
                    paramsLayout.setMargins(0, 0, 0, 10);
                else
                    paramsLayout.setMargins(0, 0, 0, 20);
            } else
                paramsLayout.setMargins(0, 0, 0, 20);

            layout.setBackgroundColor(mHeaderColor);
            title.setBackgroundColor(mHeaderColor);
        }
        else //EPISODE
        {
            download.setVisibility(View.VISIBLE);

            layout.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
            title.setBackgroundColor(mContext.getColor(R.color.wc_transparent));

            if (episode.getPosition() > 0) {
                episodeProgress.setVisibility(View.VISIBLE);
                episodeProgress.setMax(episode.getDuration());
                episodeProgress.setProgress(episode.getPosition());
            } else
                episodeProgress.setVisibility(View.GONE);

            final int topMarginPlaylists = 20;

            thumb.setVisibility(episode.getIsLocal() ? View.GONE : View.VISIBLE);

            if (isHDPI) {
                if (isRound)
                    paramsLayout.setMargins(35, topMarginPlaylists, 35, 0);
                else
                    paramsLayout.setMargins(15, topMarginPlaylists, 15, 0);
            } else if (isXHDPI)
                paramsLayout.setMargins(45, topMarginPlaylists, 45, 0);
            else
                paramsLayout.setMargins(45, topMarginPlaylists, 45, 0);

            if (episode.getDisplayThumbnail() != null)
                thumb.setImageDrawable(episode.getDisplayThumbnail());
            else
                thumb.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_logo_placeholder));

            title.setText(episode.getTitle());
            title.setVisibility(View.VISIBLE);
            title.setTextSize(14);
            title.setGravity(Gravity.START);
        }
    }

    @Override
    public int getItemCount() {
        return mEpisodes.size();
    }

}