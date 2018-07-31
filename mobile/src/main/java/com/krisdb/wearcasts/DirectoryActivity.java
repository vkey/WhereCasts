package com.krisdb.wearcasts;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;

import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastCategory;

import java.util.ArrayList;
import java.util.List;

public class DirectoryActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    private static int mNumberOfPages = 0;
    private ProgressBar mProgressBar;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_directory);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.menu_text_directory));

        mViewPager = findViewById(R.id.main_pager);
        mProgressBar = findViewById(R.id.main_progress_bar);

        ((TabLayout) findViewById(R.id.podcasts_tabs)).setupWithViewPager(mViewPager);

        findViewById(R.id.main_progress_text).setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);

        new AsyncTasks.GetDirectory(this,
                new Interfaces.DirectoryResponse() {
                    @Override
                    public void processFinish(final List<PodcastCategory> categories, final Boolean connected) {
                        SetDirectory(categories, connected);
                        mProgressBar.setVisibility(View.GONE);
                        findViewById(R.id.main_progress_text).setVisibility(View.GONE);
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void SetDirectory(final List<PodcastCategory> categories, final Boolean connected)
    {
        mNumberOfPages = categories.size();
        final MainPagerAdapter adapter = new MainPagerAdapter(getSupportFragmentManager());

        for (final PodcastCategory category : categories)
            adapter.addFrag(PodcastListFragment.newInstance(category.getPodcasts(), connected), category.getName());

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
        if (item.getItemId() == R.id.menu_about)
            startActivity(new Intent(this, AboutActivity.class));

        if (item.getItemId() == android.R.id.home)
            finish();

        return super.onOptionsItemSelected(item);
    }
}