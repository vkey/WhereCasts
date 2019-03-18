package com.krisdb.wearcasts.Utilities;

import android.os.Bundle;

import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

public class EpisodesDiffCallback extends DiffUtil.Callback {

    private List<PodcastItem> mOldList;
    private List<PodcastItem> mNewList;

    EpisodesDiffCallback(List<PodcastItem> oldList, List<PodcastItem> newList) {
        this.mOldList = oldList;
        this.mNewList = newList;
    }

    @Override
    public int getOldListSize() {
        return mOldList != null ? mOldList.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return mNewList != null ? mNewList.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return mNewList.get(newItemPosition).getPosition() == mOldList.get(oldItemPosition).getPosition();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return mNewList.get(newItemPosition).equals(mOldList.get(oldItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        PodcastItem newEpisode = mNewList.get(newItemPosition);
        PodcastItem oldEpisode = mOldList.get(oldItemPosition);

        Bundle diff = new Bundle();
        if(!newEpisode.getTitle().equals(oldEpisode.getTitle())){
            diff.putString("title", newEpisode.getTitle());
        }

        if(!(newEpisode.getPosition() == oldEpisode.getPosition())){
            diff.putInt("position", newEpisode.getPosition());
        }
        if (diff.size()==0){
            return null;
        }
        return diff;
    }
}
