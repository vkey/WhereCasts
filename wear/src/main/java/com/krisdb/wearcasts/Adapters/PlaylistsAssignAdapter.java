package com.krisdb.wearcasts.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;

import java.util.List;


public class PlaylistsAssignAdapter extends ArrayAdapter<PlaylistItem> {

    private final List<PlaylistItem> mPlaylists;
    private final Context mContext;
    public PlaylistsAssignAdapter(@NonNull Context context, @NonNull List<PlaylistItem> playlists) {
        super(context, 0, playlists);
        mContext = context;
        mPlaylists = playlists;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }


    @Override
    public @NonNull View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    private View createItemView(int position, View convertView, ViewGroup parent){
        final View view = LayoutInflater.from(mContext).inflate(R.layout.playlist_assign_row_item, parent, false);

        ((TextView)view.findViewById(R.id.playlist_assign_row_item_name)).setText(mPlaylists.get(position).getName());

        return view;
    }
}