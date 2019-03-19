package com.krisdb.wearcasts.Activities;

import android.content.res.Resources;
<<<<<<< HEAD
<<<<<<< HEAD

import com.krisdb.wearcasts.Utilities.Utilities;

import androidx.fragment.app.FragmentActivity;

public abstract class BasePreferenceActivity extends FragmentActivity {
=======
import android.preference.PreferenceActivity;

import com.krisdb.wearcasts.Utilities.Utilities;

public abstract class BasePreferenceActivity extends PreferenceActivity {
>>>>>>> parent of 638f5a8... preferences update
=======
import android.support.v4.app.FragmentActivity;

import com.krisdb.wearcasts.Utilities.Utilities;

public abstract class BasePreferenceActivity extends FragmentActivity {
>>>>>>> parent of 16d73e0... revet
    @Override
    public Resources.Theme getTheme() {
        final Resources.Theme theme = super.getTheme();

        theme.applyStyle(Utilities.getPreferenceTheme(this), true);

        return theme;
    }


}
