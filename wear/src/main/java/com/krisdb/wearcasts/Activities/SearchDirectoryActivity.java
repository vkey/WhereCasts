package com.krisdb.wearcasts.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.krisdb.wearcasts.Adapters.AddPodcastsAdapter;
import com.krisdb.wearcasts.Adapters.NavigationAdapter;
import com.krisdb.wearcasts.Databases.DBDirectoryPodcasts;
import com.krisdb.wearcasts.Models.NavItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.ChannelItem;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.krisdb.wearcastslibrary.CommonUtils.GetRoundedLogo;

public class SearchDirectoryActivity extends BaseFragmentActivity implements WearableNavigationDrawerView.OnItemSelectedListener {
    private static int SPEECH_REQUEST_CODE = 1;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private Activity mActivity;
    private View mSearchVoiceImage;
    private EditText mSearchText;
    private ArrayList<NavItem> mNavItems;
    private WearableNavigationDrawerView mNavDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_directory_activity);
        mActivity = this;

        mProgressBar = findViewById(R.id.search_progress_bar);
        mProgressText = findViewById(R.id.search_progress_text);
        mSearchText = findViewById(R.id.search_action_text);
        mSearchVoiceImage = findViewById(R.id.search_action_voice);
        mNavDrawer = findViewById(R.id.drawer_nav_search);

        mNavItems = new ArrayList<>();
        final NavItem navItemSearch = new NavItem();
        navItemSearch.setID(0);
        navItemSearch.setTitle(getString(R.string.search));
        navItemSearch.setIcon("ic_action_search");
        mNavItems.add(navItemSearch);

        mNavDrawer.setAdapter(new NavigationAdapter(this, mNavItems));
        mNavDrawer.addOnItemSelectedListener(this);

        mSearchVoiceImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                startActivityForResult(intent, SPEECH_REQUEST_CODE);
            }
        });

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE){
                    final String text = mSearchText.getText().toString();

                    if (text.length() == 0) {
                        CommonUtils.showToast(mActivity, getString(R.string.alert_search_empty));
                        return true;
                    }
                    runSearch(text);

                    final InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
    }

    private void runSearch(final String query)
    {
        final List<PodcastItem> results = new ArrayList<>();
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setIndeterminate(true);
        mProgressText.setVisibility(View.VISIBLE);
        mProgressText.setText(getString(R.string.searching));
        mSearchText.setVisibility(View.GONE);
        mSearchVoiceImage.setVisibility(View.GONE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final String db_query = "%".concat(query.toLowerCase()).concat("%");

        final DBDirectoryPodcasts dbPodcasts = new DBDirectoryPodcasts(this);
        final SQLiteDatabase sdbPodcasts = dbPodcasts.select();
        final Cursor cursor = sdbPodcasts.rawQuery("SELECT [title],[url],[site_url],[description],[thumbnail_url],[thumbnail_name] FROM [tbl_directory_podcasts] WHERE [title] LIKE ? OR [description] LIKE ?", new String[]{db_query, db_query});

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {

                final PodcastItem podcast = new PodcastItem();
                final ChannelItem channel = new ChannelItem();
                channel.setTitle(cursor.getString(0));
                channel.setRSSUrl(cursor.getString(1));
                channel.setSiteUrl(cursor.getString(2));
                channel.setDescription(cursor.getString(3));

                if (cursor.getString(4) != null) {
                    channel.setThumbnailUrl(cursor.getString(4));
                    channel.setThumbnailName(cursor.getString(5));
                }
                podcast.setChannel(channel);
                podcast.setIsREST(false);
                podcast.setDisplayThumbnail(GetRoundedLogo(mActivity, podcast.getChannel()));
                results.add(podcast);

                cursor.moveToNext();
            }
        }

        cursor.close();
        dbPodcasts.close();

        new AsyncTasks.GetPodcastsDirectory(this, query,
                new Interfaces.PodcastsResponse() {
                    @Override
                    public void processFinish(List<PodcastItem> podcasts) {

                        results.addAll(podcasts);

                        if (results.size() == 0)
                        {
                            mProgressText.setText(getString(R.string.text_no_search_results));
                            mProgressBar.setVisibility(View.GONE);
                            mNavDrawer.getController().peekDrawer();
                            return;
                        }

                        final Set set = new TreeSet(new Comparator() {

                            @Override
                            public int compare(Object o1, Object o2) {

                                final PodcastItem p1 = (PodcastItem) o1;
                                final PodcastItem p2 = (PodcastItem) o2;

                                return (p1.getChannel().getTitle().compareToIgnoreCase(p2.getChannel().getTitle()));
                            }
                        });
                        set.addAll(results);

                        final ArrayList displayList = new ArrayList(set);
                        final int headerColor = Utilities.getHeaderColor(mActivity);

                        final WearableRecyclerView rv = findViewById(R.id.search_results);
                        rv.setLayoutManager(new LinearLayoutManager(mActivity));
                        rv.setAdapter(new AddPodcastsAdapter(mActivity, displayList, headerColor));

                        findViewById(R.id.search_results).setVisibility(View.VISIBLE);
                        mProgressBar.setVisibility(View.GONE);
                        mProgressText.setVisibility(View.GONE);
                        mNavDrawer.getController().peekDrawer();
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            final List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            runSearch(results.get(0));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onItemSelected(int pos) {
        final int id = mNavItems.get(pos).getID();
        switch (id) {
            case 0:
                mSearchText.setVisibility(View.VISIBLE);
                mSearchVoiceImage.setVisibility(View.VISIBLE);
                findViewById(R.id.search_results).setVisibility(View.GONE);
                mProgressText.setVisibility(View.GONE);
                break;
        }
    }
}