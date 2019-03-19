package com.krisdb.wearcasts.Activities;

import android.content.res.Resources;

import com.krisdb.wearcasts.Utilities.Utilities;

import androidx.fragment.app.FragmentActivity;

public abstract class BasePreferenceActivity extends FragmentActivity {
    @Override
    public Resources.Theme getTheme() {
        final Resources.Theme theme = super.getTheme();

        theme.applyStyle(Utilities.getTheme(this), true);

        return theme;
    }


}
