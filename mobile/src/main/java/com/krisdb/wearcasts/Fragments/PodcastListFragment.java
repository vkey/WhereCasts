package com.krisdb.wearcasts.Fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.krisdb.wearcasts.Adapters.PodcastsAdapter;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.Async.WatchConnected;
import com.krisdb.wearcastslibrary.CommonUtils;
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
            CommonUtils.executeSingleThreadAsync(new WatchConnected(mActivity), (connected) -> {
                List<PodcastItem> podcasts = (List<PodcastItem>) getArguments().getSerializable("podcasts");

                podcasts = podcasts.subList(1, podcasts.size());

                final RecyclerView rv = listView.findViewById(R.id.podcasts_list);
                rv.setLayoutManager(new LinearLayoutManager(mActivity));
                rv.setAdapter(new PodcastsAdapter(mActivity, podcasts, connected));
            });
        }
        return listView;
    }

    @Override
    public void onActivityCreated(final Bundle icicle)
    {
        super.onActivityCreated(icicle);
    }
}