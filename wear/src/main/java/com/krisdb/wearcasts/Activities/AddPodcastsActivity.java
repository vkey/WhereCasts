package com.krisdb.wearcasts.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;

import com.krisdb.wearcasts.Adapters.NavigationAdapter;
import com.krisdb.wearcasts.Fragments.AddPodcastListFragment;
import com.krisdb.wearcasts.Models.NavItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastCategory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class AddPodcastsActivity extends BaseFragmentActivity implements WearableNavigationDrawerView.OnItemSelectedListener {
    //private ViewPager2 mViewPager;
    private ViewPager mViewPager;
    private static int mNumberOfPages;
    private Context mContext;
    private ProgressBar mProgressBar;
    private TextView mProgressBarText;
    private ArrayList<NavItem> mNavItems;
    private WearableNavigationDrawerView mNavDrawer;
    private static int WIFI_RESULT_CODE = 101;
    private Boolean mForceRefresh;
    private static WeakReference<AddPodcastsActivity> mActivityRef;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_podcasts);

        mContext = getApplicationContext();
        mActivityRef = new WeakReference<>(this);

        mViewPager = findViewById(R.id.podcasts_add_pager);
        mProgressBar = findViewById(R.id.add_podcast_progress_bar);
        mProgressBarText = findViewById(R.id.add_podcasts_progress_text);

        mNavDrawer = findViewById(R.id.drawer_nav_directory);

        mNavItems = new ArrayList<>();
        final NavItem navItemSearch = new NavItem();
        navItemSearch.setID(0);
        navItemSearch.setTitle(getString(R.string.search));
        navItemSearch.setIcon("ic_action_search");
        mNavItems.add(navItemSearch);

        final NavItem navItemSync = new NavItem();
        navItemSync.setID(1);
        navItemSync.setTitle(getString(R.string.nav_directory_resync));
        navItemSync.setIcon("ic_action_resync");
        mNavItems.add(navItemSync);

        mNavDrawer.setAdapter(new NavigationAdapter(this, mNavItems));
        mNavDrawer.addOnItemSelectedListener(this);

        mForceRefresh = getIntent().getExtras() != null && getIntent().getExtras().getBoolean("refresh", false);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
            public void onPageSelected(int position) {
                mNavDrawer.getController().closeDrawer();
            }
        });

        /*
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback()
        {
            public void onPageScrollStateChanged(int state) {
                mNavDrawer.getController().closeDrawer();
            }
        });
         */

        if (!CommonUtils.isNetworkAvailable(mContext) && mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
            final AlertDialog.Builder alert = new AlertDialog.Builder(AddPodcastsActivity.this);
            alert.setMessage(getString(R.string.alert_episode_network_notfound));
            alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), WIFI_RESULT_CODE);
                    dialog.dismiss();
                }
            });

            alert.setNegativeButton(getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            }).show();
        }
        else
            SetDirectory();
    }

    private void SetDirectory() {
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBarText.setVisibility(View.VISIBLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        new AsyncTasks.GetDirectory(this, mForceRefresh, false, mProgressBar,
                new Interfaces.DirectoryResponse() {
                    @Override
                    public void processFinish(final List<PodcastCategory> categories) {

                        if (categories.size() > 0) {
                            mNumberOfPages = categories.size();

                            //final AddPodcastsPagerAdapter2 adapter = new AddPodcastsPagerAdapter2(mActivityRef.get());
                            final AddPodcastsPagerAdapter adapter = new AddPodcastsPagerAdapter(getSupportFragmentManager());

                            for (final PodcastCategory category : categories)
                                adapter.addFrag(AddPodcastListFragment.newInstance(category.getPodcasts()), category.getName());

                            mViewPager.setAdapter(adapter);

                            mViewPager.setVisibility(View.VISIBLE);
                            mProgressBar.setVisibility(View.GONE);
                            mProgressBarText.setVisibility(View.GONE);
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                        else
                        {
                            CommonUtils.showToast(mContext, getString(R.string.general_error));
                            finish();
                        }
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == WIFI_RESULT_CODE)
                SetDirectory();
        }
    }

    @Override
    public void onItemSelected(int pos) {
        final int id = mNavItems.get(pos).getID();
        switch (id) {
            case 0:
                startActivity(new Intent(mContext, SearchDirectoryActivity.class));
                break;
            case 1:
                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    final AlertDialog.Builder alert = new AlertDialog.Builder(AddPodcastsActivity.this);
                    alert.setMessage(getString(R.string.alert_directory_resync));
                    alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            {
                                final Intent intent = getIntent();
                                final Bundle bundle = new Bundle();
                                bundle.putBoolean("refresh", true);
                                intent.putExtras(bundle);
                                finish();
                                startActivity(intent);
                            }
                        }
                    });
                    alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    alert.show();
                }
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private class AddPodcastsPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        AddPodcastsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int index) {
            return mFragmentList.get(index);
        }

        public void addFrag(Fragment fragment, String title) {
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

    private class AddPodcastsPagerAdapter2 extends FragmentStateAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        AddPodcastsPagerAdapter2(FragmentActivity fa) {
            super(fa);
        }

        public void addFrag(Fragment fragment, String title) {
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
}