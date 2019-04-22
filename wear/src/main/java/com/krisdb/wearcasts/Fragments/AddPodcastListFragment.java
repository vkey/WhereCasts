package com.krisdb.wearcasts.Fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.krisdb.wearcasts.Adapters.AddPodcastsAdapter;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.Serializable;
import java.util.List;

public class AddPodcastListFragment extends Fragment {

    private Activity mActivity;

    public static AddPodcastListFragment newInstance(final List<PodcastItem> podcasts) {

        final AddPodcastListFragment plf = new AddPodcastListFragment();

        final Bundle bundle = new Bundle();
        bundle.putSerializable("podcasts", (Serializable)podcasts);

        plf.setArguments(bundle);

        return plf;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        setRetainInstance(true);

        final View listView = inflater.inflate(R.layout.fragment_add_podcast_list, container, false);

        final List<PodcastItem> podcasts = (List<PodcastItem>) getArguments().getSerializable("podcasts");

        final WearableRecyclerView rv = listView.findViewById(R.id.podcasts_list);
        rv.setLayoutManager(new LinearLayoutManager(mActivity));

        //for (final PodcastItem podcast : podcasts)
        //podcast.setDisplayThumbnail(GetRoundedLogo(mActivity, podcast.getChannel(), R.drawable.ic_thumb_default_add_podcasts));

        final int headerColor = Utilities.getHeaderColor(mActivity);
        final AddPodcastsAdapter adapter = new AddPodcastsAdapter(mActivity, podcasts, headerColor);
        rv.setAdapter(adapter);

        return listView;
    }

    @Override
    public void onActivityCreated(final Bundle icicle)
    {
        super.onActivityCreated(icicle);
    }
}