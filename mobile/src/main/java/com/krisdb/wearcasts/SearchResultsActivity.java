package com.krisdb.wearcasts;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.ChannelItem;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class SearchResultsActivity extends AppCompatActivity {
    private RecyclerView mResultsList;
    private List<PodcastItem> mPodcasts;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private String mQuery;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search_results);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mResultsList = findViewById(R.id.search_results_list);
        mProgressBar = findViewById(R.id.search_results_progress_bar);
        mProgressText = findViewById(R.id.search_results_progress_text);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        mResultsList = (RecyclerView)findViewById(R.id.search_results_list);

        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mQuery = intent.getStringExtra(SearchManager.QUERY);
            SetSearchResults();
            setTitle(mQuery);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_directory, menu);

        final SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView)menu.findItem(R.id.search_directory).getActionView();
        searchView.setQuery(mQuery, false);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(getApplicationContext(), SearchResultsActivity.class)));

        return true;
    }

    private void SetSearchResults() {

        mProgressBar.setVisibility(View.VISIBLE);
        mProgressText.setVisibility(View.VISIBLE);
        String db_query = "%".concat(mQuery.toLowerCase()).concat("%");

        final DBDirectoryPodcasts dbPodcasts = new DBDirectoryPodcasts(this);
        final SQLiteDatabase sdbPodcasts = dbPodcasts.select();
        final Cursor cursor = sdbPodcasts.rawQuery("SELECT [title],[url],[site_url],[description],[thumbnail_url],[thumbnail_name] FROM [tbl_directory_podcasts] WHERE [title] LIKE ? OR [description] LIKE ?", new String[]{db_query, db_query});

        mPodcasts = new ArrayList<>();

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
                mPodcasts.add(podcast);

                cursor.moveToNext();
            }
        }

        cursor.close();
        dbPodcasts.close();

        new AsyncTasks.GetPodcastsDirectory(this, mQuery,
                new Interfaces.PodcastsResponse() {
                    @Override
                    public void processFinish(List<PodcastItem> podcasts) {

                        if (podcasts.size() > 0) {
                            mPodcasts.addAll(podcasts);
                            findViewById(R.id.search_results_listennotes_layout).setVisibility(View.VISIBLE);
                        }
                        else
                            findViewById(R.id.search_results_listennotes_layout).setVisibility(View.GONE);

                        final Set set = new TreeSet(new Comparator() {

                            @Override
                            public int compare(Object o1, Object o2) {

                                final PodcastItem p1 = (PodcastItem) o1;
                                final PodcastItem p2 = (PodcastItem) o2;

                                return (p1.getChannel().getTitle().compareToIgnoreCase(p2.getChannel().getTitle()));
                            }
                        });
                        set.addAll(mPodcasts);

                        final ArrayList displayList = new ArrayList(set);

                        mResultsList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                        mResultsList.setAdapter(new PodcastsAdapter(SearchResultsActivity.this, displayList, true));
                        mProgressBar.setVisibility(View.GONE);
                        mProgressText.setVisibility(View.GONE);
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
            finish();

        return super.onOptionsItemSelected(item);
    }
}