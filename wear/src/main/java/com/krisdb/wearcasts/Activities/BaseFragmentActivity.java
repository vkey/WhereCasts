package com.krisdb.wearcasts.Activities;

import android.content.res.Resources;
import android.support.v4.app.FragmentActivity;

import com.krisdb.wearcasts.Utilities.Utilities;

public abstract class BaseFragmentActivity extends FragmentActivity {

    @Override
    public Resources.Theme getTheme() {
        final Resources.Theme theme = super.getTheme();

        theme.applyStyle(Utilities.getTheme(this), true);

        return theme;
    }

}
