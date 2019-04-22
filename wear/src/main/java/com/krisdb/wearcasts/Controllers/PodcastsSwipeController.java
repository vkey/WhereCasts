package com.krisdb.wearcasts.Controllers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.krisdb.wearcasts.Adapters.PodcastsAdapter;
import com.krisdb.wearcasts.AsyncTasks;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

public class PodcastsSwipeController extends ItemTouchHelper.Callback {

    private List<PodcastItem> mPodcasts;
    private Context mContext;
    private PodcastsAdapter mAdapter;

    PodcastsSwipeController(final Context ctx, final PodcastsAdapter adapter, final List<PodcastItem> podcasts)
    {
        mContext = ctx;
        mAdapter = adapter;
        mPodcasts = podcasts;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, ItemTouchHelper.RIGHT);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        final int position = viewHolder.getAdapterPosition();

        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        alert.setMessage(mContext.getString(R.string.confirm_delete_podcast));
        alert.setPositiveButton(mContext.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, int which) {

                final PodcastItem podcast = mPodcasts.get(position);

                new AsyncTasks.Unsubscribe(mContext, podcast.getPodcastId(),
                        new Interfaces.BooleanResponse() {
                            @Override
                            public void processFinish(Boolean done) {
                                mAdapter.refreshContent();
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
        alert.setNegativeButton(mContext.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAdapter.notifyItemChanged(position);
                dialog.dismiss();
            }
        });

        alert.show();
    }
}
