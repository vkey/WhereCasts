package com.krisdb.wearcasts.Fragments;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.krisdb.wearcasts.Adapters.PodcastsAdapter;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.Serializable;
import java.util.List;

public class PodcastListFragment extends Fragment {

    private Activity mActivity;

    public static PodcastListFragment newInstance(final List<PodcastItem> podcasts) {

        final PodcastListFragment plf = new PodcastListFragment();

        final Bundle bundle = new Bundle();
        bundle.putSerializable("podcasts", (Serializable)podcasts);

        plf.setArguments(bundle);

        return plf;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        final View listView = inflater.inflate(R.layout.fragment_podcast_list, container, false);
        if (getArguments() != null)
        {
            new AsyncTasks.WatchConnected(mActivity,
                    new Interfaces.BooleanResponse() {
                        @Override
                        public void processFinish(final Boolean connected) {
                            List<PodcastItem> podcasts = (List<PodcastItem>) getArguments().getSerializable("podcasts");

                            podcasts = podcasts.subList(1, podcasts.size());

                            final RecyclerView rv = listView.findViewById(R.id.podcasts_list);
                            rv.setLayoutManager(new LinearLayoutManager(mActivity));
                            rv.setAdapter(new PodcastsAdapter(mActivity, podcasts, connected));
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        }
        return listView;
    }

    @Override
    public void onActivityCreated(final Bundle icicle)
    {
        super.onActivityCreated(icicle);
    }
}