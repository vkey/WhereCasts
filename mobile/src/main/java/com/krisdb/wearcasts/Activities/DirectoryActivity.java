package com.krisdb.wearcasts.Activities;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;

import com.google.android.material.tabs.TabLayout;
import com.krisdb.wearcasts.Fragments.PlayerFragment;
import com.krisdb.wearcasts.Fragments.PodcastListFragment;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastCategory;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class DirectoryActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    private static int mNumberOfPages = 0;
    private ProgressBar mProgressBar;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_directory);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mViewPager = findViewById(R.id.main_pager);
        mProgressBar = findViewById(R.id.main_progress_bar);

        ((TabLayout) findViewById(R.id.podcasts_tabs)).setupWithViewPager(mViewPager);

        findViewById(R.id.main_progress_text).setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);

        new AsyncTasks.GetDirectory(this,
                new Interfaces.DirectoryResponse() {
                    @Override
                    public void processFinish(final List<PodcastCategory> categories) {
                        SetDirectory(categories);
                        mProgressBar.setVisibility(View.GONE);
                        findViewById(R.id.main_progress_text).setVisibility(View.GONE);
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void SetDirectory(final List<PodcastCategory> categories)
    {
        mNumberOfPages = categories.size();
        final MainPagerAdapter adapter = new MainPagerAdapter(getSupportFragmentManager());

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getInt("id", 0) > 0) {
            adapter.addFrag(PlayerFragment.newInstance(), getString(R.string.tab_play));
            mNumberOfPages = mNumberOfPages + 1;
        }

        for (final PodcastCategory category : categories)
            adapter.addFrag(PodcastListFragment.newInstance(category.getPodcasts()), category.getName());

        mViewPager.setAdapter(adapter);
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
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_directory, menu);

        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.search_directory).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(getApplicationContext(), SearchResultsActivity.class)));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        if (item.getItemId() == R.id.menu_directory_import)
            startActivity(new Intent(this, UserAddActivity.class));

        if (item.getItemId() == R.id.menu_directory_premium)
            startActivity(new Intent(this, PremiumActivity.class));

        if (item.getItemId() == R.id.menu_about)
            startActivity(new Intent(this, AboutActivity.class));

        if (item.getItemId() == R.id.menu_tutorial)
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(getString(R.string.tutorial_url))));

        if (item.getItemId() == R.id.menu_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
            sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_body, getString(R.string.google_play_url)));
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}