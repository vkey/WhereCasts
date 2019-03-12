package com.krisdb.wearcasts.Activities;

import android.app.AlertDialog;
import android.app.NotificationManager;
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
import android.os.SystemClock;
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
import android.widget.ImageView;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcasts.Adapters.NavigationAdapter;
import com.krisdb.wearcasts.Fragments.PodcastEpisodesListFragment;
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

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.krisdb.wearcasts.Utilities.EpisodeUtilities.HasEpisodes;
import static com.krisdb.wearcasts.Utilities.PlaylistsUtilities.getPlaylists;
import static com.krisdb.wearcastslibrary.CommonUtils.GetLocalDirectory;
import static com.krisdb.wearcastslibrary.CommonUtils.GetRoundedLogo;
import static com.krisdb.wearcastslibrary.CommonUtils.GetThumbnailDirectory;

public class MainActivity extends BaseFragmentActivity implements WearableNavigationDrawerView.OnItemSelectedListener {
    private static int mNumberOfPages;
    public static List<Integer> mPlayListIds;
    private static List<NavItem> mNavItems;
    private static Boolean mRefresh = false, mShowPodcastList = false;
    private LocalBroadcastManager mBroadcastManger;
    private static int PERMISSIONS_CODE = 121;
    private static WeakReference<WearableNavigationDrawerView> mNavDrawer;
    private static WeakReference<MainActivity> mActivityRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mBroadcastManger = LocalBroadcastManager.getInstance(this);
        mActivityRef = new WeakReference<>(this);

        final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(102);

        //CommonUtils.showToast(this, CommonUtils.getDensityName(this));

        //final SQLiteDatabase sdb = DatabaseHelper.select(this);
        //sdb.execSQL("DELETE FROM [tbl_podcast_episodes] WHERE [id] IN (SELECT [id] FROM [tbl_podcast_episodes] WHERE [pid] = 2 ORDER BY [pubdate] DESC LIMIT 1)");

        //final SQLiteDatabase sdb = DatabaseHelper.select(this);
        //sdb.execSQL("DELETE FROM [tbl_podcast_episodes] WHERE [id] IN (SELECT [id] FROM [tbl_podcast_episodes] WHERE [pid] = 1 ORDER BY [pubdate] DESC LIMIT 1)"); //CNN

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
        ((ImageView)findViewById(R.id.main_splash_image)).setImageDrawable(GetRoundedLogo(this, null));

        new Init(this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

        mNavItems = Utilities.getNavItems(this);
        mNavDrawer = new WeakReference<>((WearableNavigationDrawerView)findViewById(R.id.drawer_nav_main));

        if (mNavDrawer.get() != null) {
            mNavDrawer.get().setAdapter(new NavigationAdapter(this, mNavItems));
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
            final boolean hideEmpty = prefs.getBoolean("pref_hide_empty_playlists", false);
            final boolean localFiles = (ContextCompat.checkSelfPermission(ctx, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && DBUtilities.GetLocalFiles(ctx).size() > 1);

            mPlayListIds = new ArrayList<>();
            mPlayListIds.add(resources.getInteger(R.integer.playlist_default));

            //third party: add check for playlist
            if (HasEpisodes(ctx, 0, resources.getInteger(R.integer.playlist_playerfm)))
                mPlayListIds.add(resources.getInteger(R.integer.playlist_playerfm));

            final List<PlaylistItem> playlists = getPlaylists(ctx, hideEmpty);

            if (PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("pref_display_show_downloaded_episodes", false))
            {
                for (final PlaylistItem playlist : playlists)
                    if (HasEpisodes(ctx, 0, playlist.getID()))
                        mPlayListIds.add(playlist.getID());
            }
            else {
                for (final PlaylistItem playlist : playlists)
                    mPlayListIds.add(playlist.getID());
            }

            //if (hideEmpty == false || DBUtilities.HasEpisodes(ctx, 0, resources.getInteger(R.integer.playlist_radio)))
                //mPlayListIds.add(resources.getInteger(R.integer.playlist_radio));

            if (localFiles)
                mPlayListIds.add(resources.getInteger(R.integer.playlist_local));

            if (hideEmpty == false || HasEpisodes(ctx, 0, resources.getInteger(R.integer.playlist_inprogress)))
                mPlayListIds.add(resources.getInteger(R.integer.playlist_inprogress));

            if (hideEmpty == false || HasEpisodes(ctx, 0, resources.getInteger(R.integer.playlist_downloads)))
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

            if (visits > 40 && prefs.getBoolean("rate_app_reminded", false) == false)
            {
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
                                            final PutDataMapRequest dataMap = PutDataMapRequest.create("/rateapp");
                                            CommonUtils.DeviceSync(ctx, dataMap);

                                            final SharedPreferences.Editor editor = prefs.edit();
                                            editor.putBoolean("rate_app_reminded", true);
                                            editor.apply();

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

            final ViewPager vp = ctx.findViewById(R.id.main_pager);

            vp.setAdapter(new FragmentPagerAdapter(ctx.getSupportFragmentManager()));
            vp.setCurrentItem(mShowPodcastList ? 0 : mHomeScreen);
            vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
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

            vp.setVisibility(View.VISIBLE);

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
                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                    alert.setMessage(R.string.alert_main_menu_start_update);
                    alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            handleNetwork();
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
                break;
            case 2:
                startActivity(new Intent(this, SettingsPodcastsActivity.class));
                break;
        }
    }

    private void handleNetwork()
    {
        if (CommonUtils.getActiveNetwork(this) == null)
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setMessage(getString(R.string.alert_episode_network_notfound));
                alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"), 1);
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
        else if (CommonUtils.HighBandwidthNetwork(this) == false)
        {
            if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setMessage(getString(R.string.alert_episode_network_no_high_bandwidth));
                alert.setPositiveButton(getString(R.string.confirm_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"), 1);
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
            new com.krisdb.wearcasts.AsyncTasks.SyncPodcasts(this, 0, false,
                    new Interfaces.BackgroundSyncResponse() {
                        @Override
                        public void processFinish(final int count, final int downloads) {
                            if (count > 0) {
                                mShowPodcastList = true;
                                new Init(MainActivity.this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                            }
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                new com.krisdb.wearcasts.AsyncTasks.SyncPodcasts(this, 0, false,
                        new Interfaces.BackgroundSyncResponse() {
                            @Override
                            public void processFinish(final int count, final int downloads) {
                                mShowPodcastList = true;
                                new Init(MainActivity.this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }
}