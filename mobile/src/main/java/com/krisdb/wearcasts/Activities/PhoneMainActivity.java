package com.krisdb.wearcasts.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.Async.NodesConnected;
import com.krisdb.wearcastslibrary.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhoneMainActivity extends AppCompatActivity {
    private static int mNumberOfPages = 2;
    private ViewPager mViewPager;
    private Context mContext;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mViewPager = findViewById(R.id.main_pager);
        mContext = getApplicationContext();

        ((TabLayout)findViewById(R.id.podcasts_tabs)).setupWithViewPager(mViewPager);
        SetContent();

        if (PreferenceManager.getDefaultSharedPreferences(this).getInt("visits", 0) == 0)
        {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            final SharedPreferences.Editor editor = prefs.edit();

            String json = CommonUtils.getRemoteContent(getString(com.krisdb.wearcastslibrary.R.string.podcast_json_url, Locale.getDefault().getLanguage()));

            if(json == null)
                json = CommonUtils.getRemoteContent(getString(com.krisdb.wearcastslibrary.R.string.podcast_json_url_default));

            editor.putString("directory_json", json);
            editor.apply();
        }
    }

    private void SetContent() {

        CommonUtils.executeSingleThreadAsync(new NodesConnected(this), (connected) -> {
            final MainPagerAdapter adapter = new MainPagerAdapter(getSupportFragmentManager());

            //adapter.addFrag(UserAddActivity.newInstance(connected), getString(R.string.tab_add));
            //adapter.addFrag(PremiumActivity.newInstance(connected), getString(R.string.tab_premium));

            mViewPager.setAdapter(adapter);

            if (Intent.ACTION_SEND.equals(getIntent().getAction()) && getIntent().getType() != null)
                mViewPager.setCurrentItem(mNumberOfPages == 3 ? 1 : 0);
        });
    }

    private class MainPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        MainPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override

        public Fragment getItem(int index) {
            return mFragmentList.get(index);
        }

        void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

        @Override
        public int getCount() {
            return mNumberOfPages;
        }
    }

    @Override
        public boolean onCreateOptionsMenu(Menu menu) {
        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        if (item.getItemId() == R.id.menu_about)
            startActivity(new Intent(this, AboutActivity.class));

        if (item.getItemId() == android.R.id.home)
            finish();

        return super.onOptionsItemSelected(item);
    }
}