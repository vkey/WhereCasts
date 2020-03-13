package com.krisdb.wearcasts.Adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.wear.widget.WearableRecyclerView;
import androidx.wear.widget.drawer.WearableActionDrawerView;

import com.krisdb.wearcasts.Activities.EpisodeActivity;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Settings.SettingsPodcastActivity;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.SaveEpisodeValue;
import static com.krisdb.wearcasts.Utilities.PodcastUtilities.GetPodcast;
import static com.krisdb.wearcastslibrary.CommonUtils.isCurrentDownload;
import static com.krisdb.wearcastslibrary.CommonUtils.isFinishedDownload;


public class EpisodesAdapter extends WearableRecyclerView.Adapter<EpisodesAdapter.ViewHolder> {

    public List<PodcastItem> mEpisodes;
    public List<PodcastItem> mSelectedEpisodes;
    public List<Integer> mSelectedPositions;
    private Activity mContext;
    private int mTextColor;
    private static int mNoNetworkPosition;
    private String mDensityName;
    private boolean isRound, isXHDPI, isHDPI;
    private static WeakReference<Activity> mActivityRef;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private WearableActionDrawerView mWearableActionDrawer;
    private Handler mHandler = new Handler();

    private ConnectivityManager mManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private TimeOutHandler mTimeOutHandler;
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(7);

    static class ViewHolder extends WearableRecyclerView.ViewHolder {

        private final TextView title, date, duration;
        private final ImageView thumbnailTitle, download;
        private final ConstraintLayout layout;
        private ProgressBar progressDownload, progressDownloadLoading;

        ViewHolder(final View view) {
            super(view);
            title = view.findViewById(R.id.episode_row_item_title);
            date = view.findViewById(R.id.episode_row_item_date);
            duration = view.findViewById(R.id.episode_row_item_duration);
            thumbnailTitle = view.findViewById(R.id.episode_row_item_title_thumbnail);
            download = view.findViewById(R.id.episode_row_item_download);
            layout = view.findViewById(R.id.episode_row_item_layout);
            progressDownload = view.findViewById(R.id.episode_row_item_download_progress);
            progressDownloadLoading = view.findViewById(R.id.episode_row_item_download_progress_loading);
        }
    }

    public EpisodesAdapter(final Activity context, final List<PodcastItem> episodes, final int textColor, final SwipeRefreshLayout refreshLayout, final WearableActionDrawerView menu) {
        mEpisodes = episodes;
        mContext = context;
        mTextColor = textColor;
        mDensityName = CommonUtils.getDensityName(mContext);
        isRound = mContext.getResources().getConfiguration().isScreenRound();
        mActivityRef = new WeakReference<>(mContext);
        mSelectedEpisodes = new ArrayList<>();
        isXHDPI = Objects.equals(mDensityName, mContext.getString(R.string.xhdpi));
        isHDPI = Objects.equals(mDensityName, mContext.getString(R.string.hdpi));
        mSwipeRefreshLayout = refreshLayout;
        mWearableActionDrawer = menu;
        mSelectedPositions = new ArrayList<>();
        mTimeOutHandler = new TimeOutHandler(this);
        mManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        //mHandler.postDelayed(downloadsProgress, 1000);
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int viewType) {
        final View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.episode_row_item, viewGroup, false);

        final ViewHolder holder = new ViewHolder(view);

        holder.thumbnailTitle.setOnLongClickListener(view17 -> {

            final PodcastItem podcast = GetPodcast(viewGroup.getContext(), mEpisodes.get(holder.getAdapterPosition()).getPodcastId());

            final Intent intent = new Intent(mContext, SettingsPodcastActivity.class);
            final Bundle bundle = new Bundle();
            bundle.putInt("podcastId", podcast.getPodcastId());
            intent.putExtras(bundle);

            if (podcast.getPodcastId() > 0)
                mContext.startActivityForResult(intent, 134);

            return false;
        });

        holder.download.setOnClickListener(view1 -> initDownload(holder, holder.getAdapterPosition()));

        holder.duration.setOnClickListener(view12 -> initDownload(holder, holder.getAdapterPosition()));

        holder.date.setOnLongClickListener(view13 -> {
            showContext(holder.getAdapterPosition());
            return false;
        });

        holder.title.setOnLongClickListener(view14 -> {
            showContext(holder.getAdapterPosition());
            return true;
        });

        holder.title.setOnClickListener(view15 -> openEpisode(holder.getAdapterPosition()));

        holder.date.setOnClickListener(view16 -> openEpisode(holder.getAdapterPosition()));

        return holder;
    }

    private void initDownload(final ViewHolder holder, final int position)
    {
        final PodcastItem episode = mEpisodes.get(position);

        final int downloadId = episode.getDownloadId();
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
                        mEpisodes.get(position).setDownloadId(0);
                        //((Interfaces.EpisodeDownload)mContext).refresh(null, position, 0, true);
                        notifyItemChanged(position);
                    }
                });
                alert.setNegativeButton(mContext.getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss());
                alert.setNegativeButton(mContext.getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss());
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
                alert.setNegativeButton(mContext.getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss());
                alert.show();
            }
        }
        else if (!CommonUtils.isNetworkAvailable(mContext))
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
                alert.setMessage(mContext.getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(mContext.getString(R.string.confirm_yes), (dialog, which) -> {
                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("no_network_position", holder.getAdapterPosition());
                    editor.apply();

                    mContext.startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), 102);
                    dialog.dismiss();
                });

                alert.setNegativeButton(mContext.getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss()).show();
            }
        }
        else if (prefs.getBoolean("initialDownload", true) && Utilities.BluetoothEnabled()) {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
                alert.setMessage(mContext.getString(R.string.confirm_initial_download_message));
                alert.setPositiveButton(mContext.getString(R.string.confirm_yes), (dialog, which) -> {
                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("pref_disable_bluetooth", true);
                    editor.apply();
                    initDownload(holder, position);
                    dialog.dismiss();
                });
                alert.setNegativeButton(mContext.getString(R.string.confirm_no), (dialog, which) -> {
                    downloadEpisode(position, episode);
                    dialog.dismiss();
                }).show();

                final SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("initialDownload", false);
                editor.apply();
            }
        }
        else if (prefs.getBoolean("pref_disable_bluetooth", false) && Utilities.BluetoothEnabled() && Utilities.disableBluetooth(mContext)) {
            unregisterNetworkCallback();

            if (!CommonUtils.isNetworkAvailable(mContext, true))
                CommonUtils.showToast(mContext, mContext.getString(R.string.alert_episode_network_waiting));

            mNoNetworkPosition = holder.getAdapterPosition();
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

    public void downloadEpisode(final int position, final PodcastItem episode) {
        final long downloadID = Utilities.startDownload(mContext, episode);
        mEpisodes.get(position).setDownloadId((int)downloadID);
        notifyItemChanged(position);
        //mHandler.postDelayed(downloadsProgress, 1000);
    }

    public void downloadAllEpisodes() {
        CommonUtils.showToast(mContext, mContext.getString(R.string.alert_episode_download_start));

        int count = mEpisodes.size()-1;
        for(int i = 1; i <= count; i++)
        {
            final long downloadID = Utilities.startDownload(mContext, mEpisodes.get(i), false);
            mEpisodes.get(i).setDownloadId((int)downloadID);
       }
        notifyDataSetChanged();
        //mHandler.postDelayed(downloadsProgress, 1000);
    }

    public void downloadSelectedEpisodes() {
        CommonUtils.showToast(mContext, mContext.getString(R.string.alert_episode_download_start));
        for(final Integer position : mSelectedPositions)
        {
            final long downloadID = Utilities.startDownload(mContext, mEpisodes.get(position), false);
            mEpisodes.get(position).setDownloadId((int)downloadID);
            mEpisodes.get(position).setIsSelected(false);
            notifyItemChanged(position);
        }
        mSelectedPositions = new ArrayList<>();
        mSelectedEpisodes = new ArrayList<>();
        //mHandler.postDelayed(downloadsProgress, 1000);
    }

    private Runnable downloadsProgress = new Runnable() {
        @Override
        public void run() {

            int position = 0;
            boolean hasDownloads = false;
            for (final PodcastItem episode : mEpisodes) {
                if (episode.getDownloadId() > 0 && isCurrentDownload(mContext, episode.getDownloadId())) {
                    notifyItemChanged(position);
                    hasDownloads = true;
                }
                else if(isFinishedDownload(mContext, episode.getDownloadId()))
                {
                    mEpisodes.get(position).setDownloadId(0);
                    mEpisodes.get(position).setIsDownloaded(true);
                    notifyItemChanged(position);
                }
                position++;
            }

            if (hasDownloads)
                mHandler.postDelayed(downloadsProgress, 1000);
            else
                mHandler.removeCallbacks(this);
        }
    };

    private void showContext(final int position) {
        final PodcastItem episode = mEpisodes.get(position);
        if (episode.getIsSelected()) {
            episode.setIsSelected(false);
            mSelectedEpisodes.remove(episode);
            mSelectedPositions.removeAll(Arrays.asList(position));
        } else {
            episode.setIsSelected(true);
            mSelectedEpisodes.add(episode);
            mSelectedPositions.add(position);
        }

        final Menu menu = mWearableActionDrawer.getMenu();
        menu.clear();
        SystemClock.sleep(125);
        int menuId = R.menu.menu_drawer_episode_list;

        if (mSelectedEpisodes.size() > 0) {
            mWearableActionDrawer.getController().peekDrawer();
            if (mSelectedEpisodes.size() == 1)
                menuId = R.menu.menu_drawer_episode_list_selected_single;
            else
                menuId = R.menu.menu_drawer_episode_list_selected;
        }
        else
            mWearableActionDrawer.getController().closeDrawer();

        mActivityRef.get().getMenuInflater().inflate(menuId, menu);

        mSwipeRefreshLayout.setEnabled(mSelectedEpisodes.size() == 0);
        notifyItemChanged(position);
    }

    private static class TimeOutHandler extends Handler {
        private final WeakReference<EpisodesAdapter> mActivityWeakReference;

        TimeOutHandler(final EpisodesAdapter adapter) {
            mActivityWeakReference = new WeakReference<>(adapter);
        }

        @Override
        public void handleMessage(final Message msg) {
            final EpisodesAdapter adapter = mActivityWeakReference.get();

            if (adapter != null) {
                switch (msg.what) {
                    case MESSAGE_CONNECTIVITY_TIMEOUT:
                        final Activity ctx = mActivityRef.get();

                        if (ctx != null && !ctx.isFinishing()) {
                            final AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
                            alert.setMessage(ctx.getString(R.string.alert_episode_network_notfound));
                            alert.setPositiveButton(ctx.getString(R.string.confirm_yes), (dialog, which) -> {
                                final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
                                editor.putInt("no_network_position", mNoNetworkPosition);
                                editor.apply();

                                ctx.startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), 102);
                                dialog.dismiss();
                            });

                            alert.setNegativeButton(ctx.getString(R.string.confirm_no), (dialog, which) -> {
                                Utilities.enableBluetooth(ctx);
                                dialog.dismiss();
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

    private void openEpisode(final int position)
    {
        final SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(mContext);

        if (Integer.valueOf(prefs.getString("pref_display_home_screen", String.valueOf(mContext.getResources().getInteger(R.integer.default_home_screen)))) == mContext.getResources().getInteger(R.integer.home_screen_option_playing_Screen))
        {
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("default_home_screen_playing_screen_playlistid", mContext.getResources().getInteger(R.integer.playlist_episodes));
            editor.apply();
        }

        final Intent intent = new Intent(mContext, EpisodeActivity.class);

        final Bundle bundle = new Bundle();
        bundle.putInt("episodeid", mEpisodes.get(position).getEpisodeId());
        bundle.putInt("playlistid", mContext.getResources().getInteger(R.integer.playlist_episodes));

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

    public void refreshItem2(final List<PodcastItem> episodes, final int position)
    {
        mEpisodes = episodes;
        notifyItemRemoved(position);
        notifyItemInserted(position);
    }


    public void refreshItem(final int position)
    {
        notifyItemChanged(position);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, final int position) {
        final PodcastItem episode = mEpisodes.get(position);
        final TextView title = viewHolder.title;
        final TextView date = viewHolder.date;
        final TextView duration = viewHolder.duration;
        final ImageView thumbTitle = viewHolder.thumbnailTitle;
        final ImageView download = viewHolder.download;
        final ConstraintLayout layout = viewHolder.layout;
        final ProgressBar progressDownload = viewHolder.progressDownload;
        final ProgressBar progressDownloadLoading = viewHolder.progressDownloadLoading;
        final ViewGroup.MarginLayoutParams paramsLayout = (ViewGroup.MarginLayoutParams) viewHolder.layout.getLayoutParams();

        title.setTextColor(mTextColor);

        if (episode.getIsTitle()) //TITLE
        {
            title.setVisibility(View.GONE);
            date.setVisibility(View.GONE);
            download.setVisibility(View.GONE);
            duration.setVisibility(View.GONE);
            thumbTitle.setVisibility(View.VISIBLE);
            thumbTitle.setMaxWidth(Utilities.getThumbMaxWidth(mContext, mDensityName, isRound));
            thumbTitle.setImageDrawable(episode.getDisplayThumbnail());

            layout.setBackgroundColor(mContext.getColor(R.color.wc_transparent));
            title.setBackgroundColor(mContext.getColor(R.color.wc_transparent));

            if (isHDPI) {
                if (isRound)
                    paramsLayout.setMargins(0, 5, 0, 30);
                else
                    paramsLayout.setMargins(0, 4, 0, 10);
            } else if (isXHDPI)
                paramsLayout.setMargins(0, 5, 0, 20);
            else
                paramsLayout.setMargins(0, 10, 0, 10);
        }
        else //EPISODE
        {
            title.setVisibility(View.VISIBLE);
            date.setVisibility(View.VISIBLE);
            download.setVisibility(View.VISIBLE);
            thumbTitle.setVisibility(View.GONE);

            progressDownloadLoading.setVisibility(View.GONE);
            progressDownload.setVisibility(View.GONE);

            if (episode.getDownloadId() > 0) {
                download.setImageDrawable(mContext.getDrawable(R.drawable.ic_action_episode_row_item_download_cancel));

 /*               final int downloadBytes = Utilities.getDownloadProgress(mContext, episode.getDownloadId());
                if (downloadBytes > 0) {
                    progressDownload.setVisibility(View.VISIBLE);
                    progressDownload.setMax(Utilities.getDownloadTotal(mContext, episode.getDownloadId()));
                    progressDownload.setProgress(downloadBytes);
                }
                else
                    progressDownloadLoading.setVisibility(View.VISIBLE);*/
            }
            else if (episode.getIsDownloaded())
                download.setImageDrawable(mContext.getDrawable(R.drawable.ic_action_episode_row_item_download_delete));
            else
                download.setImageDrawable(mContext.getDrawable(R.drawable.ic_action_episode_row_item_download));

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
                title.setText(CommonUtils.boldText(episode.getTitle()));
            } else {
                final SpannableString titleText = new SpannableString(episode.getTitle());
                titleText.setSpan(new StyleSpan(Typeface.NORMAL), 0, titleText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                title.setText(titleText);
            }

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

            if (isHDPI) {
                if (isRound)
                    paramsLayout.setMargins(40, 0, 40, 30);
                else
                    paramsLayout.setMargins(15, 0, 15, 30);
            }
            else if (isXHDPI)
                paramsLayout.setMargins(30, 0, 30, 30);
            else
                paramsLayout.setMargins(15, 0, 15, 30);
        }
    }

    @Override
    public int getItemCount() {
        return mEpisodes.size();
    }
}