package com.krisdb.wearcasts.Fragments;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.krisdb.wearcasts.Adapters.PlaylistsAdapter;
import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.ScrollingLayoutEpisodes;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;
import java.util.Objects;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylistName;

public class PlaylistsListFragment extends Fragment {

    private WearableRecyclerView mPlaylistList;
    private int mPlaylistId, mTextColor, mHeaderColor;
    private Activity mActivity;
    private TextView mStatus, mProgressPlaylistText;
    private LinearLayout mProgressPlaylistLayout;
    private PlaylistsAdapter mAdapter;


    public static PlaylistsListFragment newInstance(final int playlistId) {

        final PlaylistsListFragment elf = new PlaylistsListFragment();

        final Bundle bundle = new Bundle();
        bundle.putInt("playlistId", playlistId);
        elf.setArguments(bundle);

        return elf;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        final View listView = inflater.inflate(R.layout.fragment_playlists, container, false);
        mPlaylistList = listView.findViewById(R.id.playlist_list);
        mProgressPlaylistLayout = listView.findViewById(R.id.playlist_progress_text_playlist_layout);
        mProgressPlaylistText = listView.findViewById(R.id.playlist_progress_text_playlist);
        mStatus = listView.findViewById(R.id.playlist_status);

        mPlaylistList.setEdgeItemsCenteringEnabled(false);
        mPlaylistList.setLayoutManager(new WearableLinearLayoutManager(mActivity, new ScrollingLayoutEpisodes()));

        if (getArguments() != null) {
            mPlaylistId = getArguments().getInt("playlistId");
        }

        mTextColor = ContextCompat.getColor(mActivity, R.color.wc_white);

        RefreshContent();

        return listView;
    }

    public void RefreshContent() {
        if (!isAdded()) return;
        mHeaderColor = Utilities.getHeaderColor(mActivity);

        final Resources resources = mActivity.getResources();
        final String densityName = CommonUtils.getDensityName(mActivity);
        final boolean isRound = resources.getConfiguration().isScreenRound();

        mStatus.setTextColor(mTextColor);

        mProgressPlaylistLayout.setBackgroundColor(mHeaderColor);
        mProgressPlaylistText.setBackgroundColor(mHeaderColor);
        String title = null;

        if (mPlaylistId == resources.getInteger(R.integer.playlist_inprogress))
            title = getString(R.string.playlist_title_inprogress);
        else if (mPlaylistId == resources.getInteger(R.integer.playlist_downloads))
            title = getString(R.string.playlist_title_downloads);
        else if (mPlaylistId == resources.getInteger(R.integer.playlist_playerfm)) //third party: add title
            title = getString(R.string.third_party_title_playerfm);
        else if (mPlaylistId == resources.getInteger(R.integer.playlist_upnext))
            title = getString(R.string.playlist_title_upnext);
        else if (mPlaylistId == resources.getInteger(R.integer.playlist_local))
            title = getString(R.string.playlist_title_local);
        else if (mPlaylistId > -1)
            title = getPlaylistName(mActivity, mPlaylistId);

        final SpannableString titleText = new SpannableString(title);
        titleText.setSpan(new StyleSpan(Typeface.BOLD), 0, titleText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mProgressPlaylistText.setText(titleText);
        mProgressPlaylistText.setTextSize(16);

        final ViewGroup.MarginLayoutParams paramsLayout = (ViewGroup.MarginLayoutParams) mProgressPlaylistLayout.getLayoutParams();

        if (Objects.equals(densityName, getString(R.string.hdpi))) {
            if (isRound) {
                mProgressPlaylistLayout.setPadding(0, 8, 0, 10);
                paramsLayout.setMargins(0, 0, 0, 40);
            } else {
                mProgressPlaylistLayout.setPadding(0, 7, 0, 7);
                paramsLayout.setMargins(0, 0, 0, 20);
            }
        } else if (Objects.equals(densityName, getString(R.string.xhdpi))) {
            mProgressPlaylistLayout.setPadding(0, 8, 0, 10);
            paramsLayout.setMargins(0, 0, 0, 0);
        } else {
            mProgressPlaylistLayout.setPadding(0, 10, 0, 10);
            paramsLayout.setMargins(0, 0, 0, 0);
        }

            mProgressPlaylistLayout.setVisibility(View.VISIBLE);
            mStatus.setVisibility(View.GONE);
        mPlaylistList.setVisibility(View.INVISIBLE);

        new AsyncTasks.DisplayPlaylistEpisodes(mActivity, mPlaylistId,
                new Interfaces.PodcastsResponse() {
                    @Override
                    public void processFinish(final List<PodcastItem> episodes) {
                        if (!isAdded()) return;

                        mAdapter = new PlaylistsAdapter(mActivity, episodes, mPlaylistId, mTextColor, mHeaderColor);
                        mPlaylistList.setAdapter(mAdapter);

                        mStatus.setVisibility(TextView.GONE);

                        mProgressPlaylistLayout.setVisibility(View.GONE);

                        mPlaylistList.setVisibility(View.VISIBLE);
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAdapter != null)
            mAdapter.refreshList();
    }
}