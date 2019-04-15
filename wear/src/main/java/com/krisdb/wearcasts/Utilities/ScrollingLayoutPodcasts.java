package com.krisdb.wearcasts.Utilities;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableLinearLayoutManager;

public class ScrollingLayoutPodcasts extends WearableLinearLayoutManager.LayoutCallback {

    @Override
    public void onLayoutFinished(View child, RecyclerView parent) {

        float centerOffset = ((float) child.getHeight() / 2.0f) / (float) parent.getHeight();
        float yRelativeToCenterOffset = (child.getY() / parent.getHeight()) + centerOffset;

        float progresstoCenter = (float) Math.sin(yRelativeToCenterOffset * Math.PI);

        child.setScaleX(progresstoCenter);
        child.setScaleY(progresstoCenter);
    }
}
