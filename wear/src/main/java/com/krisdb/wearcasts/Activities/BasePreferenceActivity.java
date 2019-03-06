package com.krisdb.wearcasts.Activities;

import android.content.res.Resources;
import android.preference.PreferenceActivity;

import com.krisdb.wearcasts.Utilities.Utilities;

public abstract class BasePreferenceActivity extends PreferenceActivity {
    @Override
    public Resources.Theme getTheme() {
        final Resources.Theme theme = super.getTheme();

        theme.applyStyle(Utilities.getTheme(this), true);

        return theme;
    }
}
