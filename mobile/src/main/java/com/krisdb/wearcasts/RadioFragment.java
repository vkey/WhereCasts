package com.krisdb.wearcasts;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastCategory;

import java.util.List;


public class RadioFragment extends Fragment  {

    private Activity mActivity;
    private Boolean mWatchConnected = false, mPremiumUnlocked = false;

    public static RadioFragment newInstance(Boolean connected) {

        final RadioFragment f = new RadioFragment();
        final Bundle bundle = new Bundle();
        bundle.putBoolean("connected", connected);
        f.setArguments(bundle);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        setRetainInstance(true);

        final View view = inflater.inflate(R.layout.fragment_radio, container, false);

        mWatchConnected = getArguments().getBoolean("connected");

        new com.krisdb.wearcasts.AsyncTasks.GetRadioList(mActivity, true, (ProgressBar)view.findViewById(R.id.radio_list_progress),
                new Interfaces.DirectoryResponse() {
                    @Override
                    public void processFinish(final List<PodcastCategory> stations) {
                        final RecyclerView rv = view.findViewById(R.id.radio_list);
                        rv.setLayoutManager(new LinearLayoutManager(mActivity));
                        rv.setAdapter(new RadioAdapter(mActivity, stations.get(0).getPodcasts(), mWatchConnected));
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return view;
    }

    @Override
    public void onActivityCreated(final Bundle icicle)
    {
        super.onActivityCreated(icicle);
    }

    @Override
    public void onResume() {
        super.onResume();

    }
}