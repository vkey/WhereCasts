package com.krisdb.wearcasts;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;

import java.util.List;

public class NavigationAdapter extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter  {
    private final Activity mActivity;
    private List<NavItem> mNavItems;

    NavigationAdapter(final Activity activity, final List<NavItem> items) {
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
