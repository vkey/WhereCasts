package com.krisdb.wearcasts;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.view.View;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.CommonUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.GetThumbnailDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class MainActivity extends BaseFragmentActivity implements WearableNavigationDrawerView.OnItemSelectedListener {
    private static int mNumberOfPages;
    public static List<Integer> mPlayListIds;
    private static List<NavItem> mNavItems;
    private static Boolean mRefresh = false, mShowPodcastList = false;
    private LocalBroadcastManager mBroadcastManger;
    private static int PERMISSIONS_CODE = 121;
    private static WeakReference<WearableNavigationDrawerView> mNavDrawer;
    private NavigationAdapter mNavAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mBroadcastManger = LocalBroadcastManager.getInstance(this);

        final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(102);

        //final SQLiteDatabase sdb = DatabaseHelper.select(this);
        //sdb.execSQL("DELETE FROM [tbl_podcast_episodes] WHERE [id] IN (SELECT [id] FROM [tbl_podcast_episodes] WHERE [pid] = 2 ORDER BY [pubdate] DESC LIMIT 1)");

        //final SQLiteDatabase sdb = DatabaseHelper.select(this);
        //sdb.execSQL("DELETE FROM [tbl_podcast_episodes] WHERE [id] IN (SELECT [id] FROM [tbl_podcast_episodes] WHERE [pid] = 1 ORDER BY [pubdate] DESC LIMIT 1)"); //CNN

        //final android.database.sqlite.SQLiteDatabase sdb = DatabaseHelper.select(this);
        //db.execSQL("DELETE FROM [tbl_playlists_xref] WHERE playlist_id = 1");

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


        new DBPodcastsEpisodes(getApplicationContext()).deletePlaylist(0);
        new DBPodcastsEpisodes(getApplicationContext()).deletePlaylist(1);
        new DBPodcastsEpisodes(getApplicationContext()).deletePlaylist(2);
        new DBPodcastsEpisodes(getApplicationContext()).deletePlaylist(3);
        new DBPodcastsEpisodes(getApplicationContext()).insertPlaylist("Comedy");
        new DBPodcastsEpisodes(getApplicationContext()).insertPlaylist("Gaming");
        new DBPodcastsEpisodes(getApplicationContext()).insertPlaylist("Videos");
        new DBPodcastsEpisodes(getApplicationContext()).insertPlaylist("Business");
        Utilities.resetHomeScreen(this);
        */

        mShowPodcastList = getIntent().getExtras() != null && getIntent().getExtras().getBoolean("new_episodes");

        new Init(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        mNavItems = Utilities.getNavItems(this);
        mNavDrawer = new WeakReference<>((WearableNavigationDrawerView)findViewById(R.id.drawer_nav_main));
        mNavAdapter = new NavigationAdapter(this, mNavItems);

        if (mNavDrawer.get() != null) {
            mNavDrawer.get().setAdapter(mNavAdapter);
            mNavDrawer.get().addOnItemSelectedListener(this);
        }
    }

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
            final Boolean hideEmpty = prefs.getBoolean("pref_hide_empty_playlists", false);
            final Boolean localFiles = (ContextCompat.checkSelfPermission(ctx, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && DBUtilities.GetLocalFiles(ctx).size() > 1);

            mPlayListIds = new ArrayList<>();
            mPlayListIds.add(resources.getInteger(R.integer.playlist_default));

            //third party: add check for playlist
            if (DBUtilities.HasEpisodes(ctx, 0, resources.getInteger(R.integer.playlist_playerfm)))
                mPlayListIds.add(resources.getInteger(R.integer.playlist_playerfm));

            final List<PlaylistItem> playlists = DBUtilities.getPlaylists(ctx, hideEmpty);

            for(final PlaylistItem playlist: playlists)
                mPlayListIds.add(playlist.getID());

            if (localFiles)
                mPlayListIds.add(resources.getInteger(R.integer.playlist_local));

            if (hideEmpty == false || DBUtilities.HasEpisodes(ctx, 0, resources.getInteger(R.integer.playlist_inprogress)))
                mPlayListIds.add(resources.getInteger(R.integer.playlist_inprogress));

            if (hideEmpty == false || DBUtilities.HasEpisodes(ctx, 0, resources.getInteger(R.integer.playlist_downloads)))
                mPlayListIds.add(resources.getInteger(R.integer.playlist_downloads));

            mNumberOfPages = mPlayListIds.size();

            for (int i = 0; i<mNumberOfPages; i++)
            {
                if (mPlayListIds.get(i) == homeScreenId)
                {
                    mHomeScreen = i;
                    break;
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

            if (visits >= 2 && prefs.getBoolean("long_press_tip_shown", false) == false && DBUtilities.GetPodcasts(ctx).size() > 0) {
                showToast(ctx, mActivity.get().getString(R.string.tips_swipe_long_press));

                final SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("long_press_tip_shown", true);
                editor.apply();
            }

            if (visits == 50)
            {
                final AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
                alert.setMessage(ctx.getString(R.string.rate_app_reminder));
                alert.setPositiveButton(ctx.getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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

            final ViewPager vpMain = ctx.findViewById(R.id.main_pager);
            vpMain.setAdapter(new FragmentPagerAdapter(ctx.getSupportFragmentManager()));
            vpMain.setCurrentItem(mShowPodcastList ? 0 : mHomeScreen);
            vpMain.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                public void onPageScrollStateChanged(int state) {
                }

                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                public void onPageSelected(int position) {
                    if (mNavDrawer != null && mNavDrawer.get() != null)
                        mNavDrawer.get().getController().closeDrawer();
                    //if (prefs.getBoolean("pref_paging_indicator", false) == false)
                    //ctx.findViewById(R.id.main_pager_dots).setVisibility(View.VISIBLE);
                }
            });
            vpMain.setVisibility(View.VISIBLE);

            /*
            final TabLayout tabs = ctx.findViewById(R.id.main_pager_dots);

            if (prefs.getBoolean("pref_paging_indicator", false) == false) {
                tabs.setupWithViewPager(vpMain, true);
                tabs.setVisibility(View.VISIBLE);
            } else
                tabs.setVisibility(View.GONE);
*/
            ActivityCompat.requestPermissions(ctx, new String[]
                    {
                            WRITE_EXTERNAL_STORAGE,
                            READ_PHONE_STATE
                    }, PERMISSIONS_CODE);
        }
    }

    private BroadcastReceiver mFragmentsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getExtras().getBoolean("hide_paging_indicator"))
                findViewById(R.id.main_pager_dots).setVisibility(View.GONE);
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
    }

    @Override
    public void onPause() {
        mBroadcastManger.unregisterReceiver(mFragmentsReceiver);
        super.onPause();
    }

    private static class FragmentPagerAdapter extends android.support.v4.app.FragmentStatePagerAdapter {

        FragmentPagerAdapter(final FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int index) {
            return (index == 0) ? PodcastsListFragment.newInstance() : PodcastEpisodesListFragment.newInstance(mPlayListIds.get(index), 0, null);
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

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String permissions[], @NonNull final int[] grantResults) {
    if (requestCode == PERMISSIONS_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                final File dirThumbs = new File(GetThumbnailDirectory());

                if (dirThumbs.exists() == false)
                    dirThumbs.mkdirs();

                final File dirLocal = new File(GetLocalDirectory());

                if (dirLocal.exists() == false)
                    dirLocal.mkdirs();
            }
        }
    }

    @Override
    public void onItemSelected(final int position) {
        final int id = mNavItems.get(position).getID();
        switch (id) {
            case 0:
                startActivity(new Intent(this, AddPodcastsActivity.class));
                break;
            case 1:
                final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

                if (adapter.isEnabled()) {
                    //CommonUtils.showToast(this,"Bluetooth off");
                    adapter.disable();
                }
                else {
                    //CommonUtils.showToast(this,"Bluetooth on");
                    adapter.enable();
                }

                mNavItems = Utilities.getNavItems(this, adapter);
                mNavAdapter.notifyDataSetChanged();
                break;
            case 2:
                startActivity(new Intent(this, SettingsPodcastsActivity.class));
                break;
        }
    }
}