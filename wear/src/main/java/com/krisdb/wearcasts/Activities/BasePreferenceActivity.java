package com.krisdb.wearcasts.Activities;

import android.content.res.Resources;
<<<<<<< HEAD

import com.krisdb.wearcasts.Utilities.Utilities;

import androidx.fragment.app.FragmentActivity;

public abstract class BasePreferenceActivity extends FragmentActivity {
=======
import android.preference.PreferenceActivity;

import com.krisdb.wearcasts.Utilities.Utilities;

public abstract class BasePreferenceActivity extends PreferenceActivity {
>>>>>>> parent of 638f5a8... preferences update
    @Override
    public Resources.Theme getTheme() {
        final Resources.Theme theme = super.getTheme();

        theme.applyStyle(Utilities.getTheme(this), true);

        return theme;
    }


}
