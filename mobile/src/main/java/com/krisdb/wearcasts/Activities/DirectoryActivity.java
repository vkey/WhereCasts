package com.krisdb.wearcasts.Activities;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import com.krisdb.wearcasts.Fragments.PlayerFragment;
import com.krisdb.wearcasts.Fragments.PodcastListFragment;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.Async.GetDirectory;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.PodcastCategory;

import java.util.ArrayList;
import java.util.List;

public class DirectoryActivity extends AppCompatActivity {
    private ViewPager2 mViewPager;
    private static int mNumberOfPages = 0;
    private ProgressBar mProgressBar;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_directory);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        CommonUtils.cancelNotification(this, 122);

        mViewPager = findViewById(R.id.main_pager);
        mProgressBar = findViewById(R.id.main_progress_bar);

        findViewById(R.id.main_progress_text).setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);

        CommonUtils.executeSingleThreadAsync(new GetDirectory(this), (categories) -> {
            SetDirectory(categories);
            mProgressBar.setVisibility(View.GONE);
            findViewById(R.id.main_progress_text).setVisibility(View.GONE);
        });
    }

    private void SetDirectory(final List<PodcastCategory> categories)
    {
        mNumberOfPages = categories.size();
        final MainPagerAdapter adapter = new MainPagerAdapter(this);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

       if (prefs.getInt("id", 0) > 0) {
            adapter.addFrag(PlayerFragment.newInstance(), getString(R.string.tab_play));
            mNumberOfPages = mNumberOfPages + 1;
        }

        for (final PodcastCategory category : categories)
            adapter.addFrag(PodcastListFragment.newInstance(category.getPodcasts()), category.getName());

        mViewPager.setAdapter(adapter);
        new TabLayoutMediator(findViewById(R.id.podcasts_tabs), mViewPager,
                (tab, position) -> {
                    if (prefs.getInt("id", 0) > 0)
                        tab.setText(position == 0 ? getString(R.string.tab_play) : categories.get(position-1).getName());
                    else
                        tab.setText(categories.get(position).getName());
                }).attach();

    }

    private class MainPagerAdapter extends FragmentStateAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        MainPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

         void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getItemCount() {
            return mNumberOfPages;
        }
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