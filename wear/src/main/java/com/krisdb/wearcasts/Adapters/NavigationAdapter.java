package com.krisdb.wearcasts.Adapters;

import android.app.Activity;
import android.graphics.drawable.Drawable;

import com.krisdb.wearcasts.Models.NavItem;

import java.util.List;

import androidx.wear.widget.drawer.WearableNavigationDrawerView;

public class NavigationAdapter extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter  {
    private final Activity mActivity;
    private List<NavItem> mNavItems;

    public NavigationAdapter(final Activity activity, final List<NavItem> items) {
        mActivity = activity;
        mNavItems = items;
    }

    @Override
    public int getCount() {
        return mNavItems.size();
    }

    @Override
    public String getItemText(final int pos) {
        return mNavItems.get(pos).getTitle();
    }

    @Override
    public Drawable getItemDrawable(final int pos) {
        return mActivity.getDrawable(mActivity.getResources().getIdentifier(mNavItems.get(pos).getIcon(), "drawable", mActivity.getPackageName()));
    }
}
