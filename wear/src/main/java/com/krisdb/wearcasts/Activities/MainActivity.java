package com.krisdb.wearcasts.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcasts.Adapters.NavigationAdapter;
import com.krisdb.wearcasts.Fragments.PlaylistsListFragment;
import com.krisdb.wearcasts.Fragments.PodcastsListFragment;
import com.krisdb.wearcasts.Models.NavItem;
import com.krisdb.wearcasts.Models.PlaylistItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Settings.SettingsPodcastsActivity;
import com.krisdb.wearcasts.Utilities.DBUtilities;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.HasEpisodes;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.GetThumbnailDirectory;

public class MainActivity extends BaseFragmentActivity implements WearableNavigationDrawerView.OnItemSelectedListener, PurchasesUpdatedListener {
    private static int mNumberOfPages;
    public static List<Integer> mPlayListIds;
    private static List<NavItem> mNavItems;
    private static Boolean mRefresh = false, mShowPodcastList = false, mPremiumInappUnlocked = false, mPremiumSubUnlocked = false;
    private LocalBroadcastManager mBroadcastManger;
    private static int PERMISSIONS_CODE = 121;
    private static WeakReference<WearableNavigationDrawerView> mNavDrawer;
    private static WeakReference<MainActivity> mActivityRef;
    //private static ViewPager2 mViewPager2;
    private static ViewPager mViewPager;
    private BillingClient mBillingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mBroadcastManger = LocalBroadcastManager.getInstance(this);
        mActivityRef = new WeakReference<>(this);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (Integer.valueOf(prefs.getString("pref_display_home_screen", String.valueOf(getResources().getInteger(R.integer.default_home_screen)))) == getResources().getInteger(R.integer.home_screen_option_playing_Screen))
        {

            int lastEpisodePlayedID = prefs.getInt("last_episode_played", 0);

            if (lastEpisodePlayedID > 0) {
                Intent i = new Intent(this, EpisodeActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("episodeid", lastEpisodePlayedID);
                i.putExtras(bundle);

                startActivity(i);
            }
        }

        //mViewPager2 = findViewById(R.id.main_pager);
        mViewPager = findViewById(R.id.main_pager);
        CommonUtils.cancelNotification(this, 102);

        //CommonUtils.showToast(this, CommonUtils.getDensityName(this));

        //final android.database.sqlite.SQLiteDatabase sdb1 = com.krisdb.wearcasts.Databases.DatabaseHelper.select(this);
        //sdb1.execSQL("DELETE FROM [tbl_podcast_episodes] WHERE [new] = 1");

        //final android.database.sqlite.SQLiteDatabase sdb2 = com.krisdb.wearcasts.Databases.DatabaseHelper.select(this);
        //sdb2.execSQL("DELETE FROM [tbl_podcast_episodes] WHERE [id] IN (SELECT [id] FROM [tbl_podcast_episodes] WHERE [pid] = 2 ORDER BY [pubdate] DESC LIMIT 1)"); //CNN

        //final android.database.sqlite.SQLiteDatabase sdb = DatabaseHelper.select(this);
        //sdb.execSQL("DELETE FROM [tbl_playlists_xref] WHERE playlist_id = -7");

        //final android.database.sqlite.SQLiteDatabase sdb = DatabaseHelper.select(this);
        //sdb.execSQL("DELETE FROM [tbl_podcast_episodes] WHERE title = 'NPR'");

        /*
        final android.database.sqlite.SQLiteDatabase sdb = DatabaseHelper.select(this);

       final Cursor cursor = sdb.rawQuery("SELECT pe.id,pe.title FROM tbl_podcast_episodes AS pe RIGHT JOIN tbl_playlists_xref AS pex ON pe.id = pex.episode_id ",null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {

                Integer id = cursor.getInt(0);
                String title = cursor.getString(1);

                String test = "123";
            }
        }

        */


        /*
        final ContentValues cv = new ContentValues();
        cv.put("title", "P3 Dokumentär");
        cv.put("url", "https://api.sr.se/rss/pod/3966");
        cv.put("site_url", "https://api.sr.se/rss/pod/3966");
        cv.put("dateAdded", DateUtils.GetDate());

        final int podcastId = (int) new DBPodcasts(this).insert(cv);
        new AsyncTasks.GetPodcastEpisodes(this, podcastId).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        */

        //new DBPodcastsEpisodes(getApplicationContext()).deletePlaylist(0);
        //new DBPodcastsEpisodes(getApplicationContext()).deletePlaylist(1);
        //new DBPodcastsEpisodes(getApplicationContext()).deletePlaylist(2);
        //new DBPodcastsEpisodes(getApplicationContext()).deletePlaylist(3);

        //new com.krisdb.wearcasts.Databases.DBPodcastsEpisodes(getApplicationContext()).insertPlaylist("Next");
        //new com.krisdb.wearcasts.Databases.DBPodcastsEpisodes(getApplicationContext()).insertPlaylist("Comedy");
        //new DBPodcastsEpisodes(getApplicationContext()).insertPlaylist("Videos");
        //new DBPodcastsEpisodes(getApplicationContext()).insertPlaylist("Business");
        //Utilities.resetHomeScreen(this);

        mShowPodcastList = getIntent().getExtras() != null && getIntent().getExtras().getBoolean("new_episodes");

        if (CommonUtils.isNetworkAvailable(this)) {
            mBillingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener(this).build();
            mBillingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(BillingResult billingResult) {
                    if (billingResult.getResponseCode() == OK) {

                        final Purchase.PurchasesResult purchasesResultInapp = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);

                        if (purchasesResultInapp.getResponseCode() == OK) {
                            final List<Purchase> purchases = purchasesResultInapp.getPurchasesList();
                            for (final Purchase purchase : purchases) {
                                if (purchase.getSku().equals(getString(R.string.inapp_premium_product_id))) {
                                    mPremiumInappUnlocked = true;
                                    break;
                                }
                            }
                        }

                        final Purchase.PurchasesResult purchasesResultSubs = mBillingClient.queryPurchases(BillingClient.SkuType.SUBS);

                        if (purchasesResultSubs.getResponseCode() == OK) {

                            final List<Purchase> purchases = purchasesResultSubs.getPurchasesList();

                            for (final Purchase purchase : purchases) {
                                if (purchase.getSku().equals(getString(R.string.sub_premium_product_id))) {
                                    mPremiumSubUnlocked = true;
                                    break;
                                }
                            }

                        }

                        final SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("premium", mPremiumInappUnlocked || mPremiumSubUnlocked);
                        editor.apply();
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {}
            });
        }

        new Init(this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

        if (!Utilities.sleepTimerEnabled(this))
            setMainMenu();
    }

    private void setMainMenu()
    {
        mNavItems = Utilities.getNavItems(this);
        mNavDrawer = new WeakReference<>((WearableNavigationDrawerView) findViewById(R.id.drawer_nav_main));

        if (mNavDrawer.get() != null) {
            mNavDrawer.get().setAdapter(new NavigationAdapter(this, mNavItems));
            mNavDrawer.get().addOnItemSelectedListener(this);
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {}

    public static class Init extends AsyncTask<Void, Void, Void> {
        private static WeakReference<MainActivity> mActivity;
        private int mHomeScreen = 0;

        Init(final MainActivity context)
        {
            mActivity = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            mActivity.get().findViewById(R.id.main_splash_layout).setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            final MainActivity ctx = mActivity.get();
            final Resources resources = ctx.getResources();

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

            final int homeScreenId = Integer.valueOf(prefs.getString("pref_display_home_screen", String.valueOf(resources.getInteger(R.integer.default_home_screen))));

            final boolean hideEmpty = prefs.getBoolean("pref_hide_empty_playlists", false);
            final boolean localFiles = (ContextCompat.checkSelfPermission(ctx, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && DBUtilities.GetLocalFiles(ctx).size() > 1);
            final boolean hasPremium = Utilities.hasPremium(ctx);

            mPlayListIds = new ArrayList<>();
            mPlayListIds.add(-1);

            //third party: add check for playlist
            if (HasEpisodes(ctx, 0, resources.getInteger(R.integer.playlist_playerfm)))
                mPlayListIds.add(resources.getInteger(R.integer.playlist_playerfm));

            if (hasPremium) {

                final List<PlaylistItem> playlists = getPlaylists(ctx, hideEmpty);

                if (prefs.getBoolean("pref_display_show_downloaded_episodes", false)) {
                    for (final PlaylistItem playlist : playlists)
                        if (HasEpisodes(ctx, 0, playlist.getID()))
                            mPlayListIds.add(playlist.getID());
                } else {
                    for (final PlaylistItem playlist : playlists)
                        mPlayListIds.add(playlist.getID());
                }
            }
            if (localFiles)
                mPlayListIds.add(resources.getInteger(R.integer.playlist_local));

            if (!hideEmpty || HasEpisodes(ctx, 0, resources.getInteger(R.integer.playlist_inprogress)))
                mPlayListIds.add(resources.getInteger(R.integer.playlist_inprogress));

            if (!hideEmpty || HasEpisodes(ctx, 0, resources.getInteger(R.integer.playlist_downloads)))
                mPlayListIds.add(resources.getInteger(R.integer.playlist_downloads));

            mNumberOfPages = mPlayListIds.size();

            if (hasPremium) {
                for (int i = 0; i < mNumberOfPages; i++) {
                    if (mPlayListIds.get(i) == homeScreenId) {
                        mHomeScreen = i;
                        break;
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            final MainActivity ctx = mActivity.get();

            ctx.findViewById(R.id.main_splash_image).setVisibility(View.GONE);

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

            if (prefs.getInt("new_episode_count", 0) > 0) {
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("new_episode_count", 0);
                editor.apply();
            }
            /*
            if (prefs.getBoolean("show_no_network_message", false)) {
                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    final AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
                    alert.setMessage(ctx.getString(R.string.alert_downloads_no_network));
                    alert.setNeutralButton(ctx.getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();

                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("show_no_network_message", false);
                    editor.apply();
                }
            }
            */

            if (prefs.getInt("new_downloads_count", 0) > 0) {
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("new_downloads_count", 0);
                editor.apply();
            }

            final int visits = prefs.getInt("visits", 0) + 1;

            if (prefs.getInt("visits", 0) == 0)
                new AsyncTasks.CacheDirectory(ctx).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            if (visits == 1) {
                Utilities.StartJob(ctx);
                if (mNavDrawer != null && mNavDrawer.get() != null)
                    mNavDrawer.get().getController().peekDrawer();
            }

            if (visits > 40 && !prefs.getBoolean("rate_app_reminded", false)) {
                new AsyncTasks.WatchConnected(ctx,
                        new Interfaces.BooleanResponse() {
                            @Override
                            public void processFinish(final Boolean connected) {
                                if (connected && mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                                    final AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
                                    alert.setMessage(ctx.getString(R.string.rate_app_reminder));
                                    alert.setPositiveButton(ctx.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            Utilities.ShowOpenOnPhoneActivity(ctx);

                                            final PutDataMapRequest dataMap = PutDataMapRequest.create("/rateapp");
                                            CommonUtils.DeviceSync(ctx, dataMap);
                                            dialog.dismiss();
                                        }
                                    });

                                    alert.setNegativeButton(ctx.getString(R.string.confirm_no), new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    }).show();

                                    final SharedPreferences.Editor editor = prefs.edit();
                                    editor.putBoolean("rate_app_reminded", true);
                                    editor.apply();
                                }
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            if (visits > 20 && prefs.getBoolean("updatesEnabled", true) && prefs.getBoolean("updatesRefactor", true)) {
                final SharedPreferences.Editor editor = prefs.edit();
                Utilities.StartJob(ctx);
                editor.putBoolean("updatesRefactor", false);
                editor.apply();
            }

            //stop tracking at 100 visits
            if (visits < 100) {
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("visits", visits);
                editor.apply();
            }

            mViewPager.setAdapter(new FragmentPagerAdapter(ctx.getSupportFragmentManager()));
            mViewPager.setCurrentItem(mShowPodcastList ? 0 : mHomeScreen);
            mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                public void onPageScrollStateChanged(int state) { }

                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

                public void onPageSelected(int position) {
                    if (mNavDrawer != null && mNavDrawer.get() != null)
                        mNavDrawer.get().getController().closeDrawer();
                }
            });
            mViewPager.setVisibility(View.VISIBLE);

            /*
            mViewPager2 = ctx.findViewById(R.id.main_pager);
            mViewPager2.setAdapter(new FragmentPagerAdapter2(ctx));
            mViewPager2.setCurrentItem(mShowPodcastList ? 0 : mHomeScreen);
            mViewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback()
            {
                public void onPageScrollStateChanged(int state) {
                    if (mNavDrawer != null && mNavDrawer.get() != null)
                        mNavDrawer.get().getController().closeDrawer();
                }
            });
            mViewPager2.setVisibility(View.VISIBLE);

            final TabLayout tabs = ctx.findViewById(R.id.main_pager_dots);

            if (prefs.getBoolean("pref_paging_indicator", false) == false) {
                tabs.setupWithViewPager(mViewPager, true);
                tabs.setVisibility(View.VISIBLE);
            } else
                tabs.setVisibility(View.GONE);
            */

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                if (ctx.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ctx.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ctx, new String[]
                            {
                                    WRITE_EXTERNAL_STORAGE,
                                    READ_PHONE_STATE
                            }, PERMISSIONS_CODE);
                }
            }
        }
    }

    private static class FragmentPagerAdapter extends FragmentStatePagerAdapter {

        FragmentPagerAdapter(final FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int index) {
            return (index == 0) ? PodcastsListFragment.newInstance() : PlaylistsListFragment.newInstance(mPlayListIds.get(index));
        }

        @Override
        public int getCount() {
            if (mRefresh)
            {
                mRefresh = false;
                notifyDataSetChanged();
            }
            return mNumberOfPages;
        }
    }

    private static class FragmentPagerAdapter2 extends FragmentStateAdapter {

        FragmentPagerAdapter2(final FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return (position == 0) ? PodcastsListFragment.newInstance() : PlaylistsListFragment.newInstance(mPlayListIds.get(position));
        }

        @Override
        public int getItemCount() {
            if (mRefresh)
            {
                mRefresh = false;
                notifyDataSetChanged();
            }
            return mNumberOfPages;
        }
    }


    private BroadcastReceiver mFragmentsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //if (intent.getExtras().getBoolean("hide_paging_indicator"))
            //findViewById(R.id.main_pager_dots).setVisibility(View.GONE);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mBroadcastManger.registerReceiver(mFragmentsReceiver, new IntentFilter("fragment"));

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getBoolean("refresh_vp", false)) {
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("refresh_vp", false);
            editor.apply();
            mRefresh = true;
            new Init(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        if (Utilities.sleepTimerEnabled(this))
            setMainMenu();
    }

    @Override
    public void onPause() {
        mBroadcastManger.unregisterReceiver(mFragmentsReceiver);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String permissions[], @NonNull final int[] grantResults) {
    if (requestCode == PERMISSIONS_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                final File dirThumbs = new File(GetThumbnailDirectory(this));

                if (!dirThumbs.exists())
                    dirThumbs.mkdirs();

                final File dirLocal = new File(GetLocalDirectory(this));

                if (!dirLocal.exists())
                    dirLocal.mkdirs();

                if (mNavDrawer != null && mNavDrawer.get() != null)
                    mNavDrawer.get().getController().peekDrawer();
            }
        }
    }

    @Override
    public void onItemSelected(final int position) {
        final int id = mNavItems.get(position).getID();
        final Context ctx = this;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final SharedPreferences.Editor editor = prefs.edit();

        switch (id) {
            case 0:
                startActivity(new Intent(ctx, AddPodcastsActivity.class));
                break;
            case 1:
                editor.putBoolean("sleep_timer_running", true);
                editor.apply();
                setMainMenu();
                Utilities.StartSleepTimerJob(ctx);
                Utilities.ShowConfirmationActivity(ctx, ctx.getString(R.string.sleep_timer_started, prefs.getString("pref_sleep_timer", "0")));
                //CommonUtils.showToast(ctx, ctx.getString(R.string.sleep_timer_started, prefs.getString("pref_sleep_timer", "0")));
                break;
            case 2:
                editor.putBoolean("sleep_timer_running", false);
                editor.apply();
                setMainMenu();
                Utilities.CancelSleepTimerJob(ctx);
                Utilities.ShowConfirmationActivity(ctx, ctx.getString(R.string.sleep_timer_stopped));
                //CommonUtils.showToast(ctx, ctx.getString(R.string.sleep_timer_stopped));
                break;
            case 3:
                handleNetwork();
                break;
            case 4:
                startActivity(new Intent(ctx, SettingsPodcastsActivity.class));
                break;
        }
    }

    private void handleNetwork()
    {
        if (!CommonUtils.isNetworkAvailable(this))
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setMessage(getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(com.krisdb.wearcastslibrary.Constants.WifiIntent), 1);
                        dialog.dismiss();
                    }
                });

                alert.setNegativeButton(getString(R.string.confirm_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        }
            else
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            new com.krisdb.wearcasts.AsyncTasks.SyncPodcasts(this, 0, false, null,
                    new Interfaces.BackgroundSyncResponse() {
                        @Override
                        public void processFinish(final int newEpisodeCount, final int downloads, final List<PodcastItem> downloadEpisodes) {
                            //mViewPager2.setAdapter(new FragmentPagerAdapter2(mActivityRef.get()));
                            mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()));
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
}