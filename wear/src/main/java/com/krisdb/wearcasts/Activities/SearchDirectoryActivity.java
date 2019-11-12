package com.krisdb.wearcasts.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;

import com.krisdb.wearcasts.Adapters.AddPodcastsAdapter;
import com.krisdb.wearcasts.Adapters.NavigationAdapter;
import com.krisdb.wearcasts.Models.NavItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;
import com.krisdb.wearcastslibrary.Async.SearchPodcasts;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.ViewModels.SearchPodcastsViewModel;
import com.krisdb.wearcastslibrary.ViewModels.SearchPodcastsViewModelFactory;

import java.util.ArrayList;
import java.util.List;

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

        mSearchVoiceImage.setOnClickListener(v -> {
            final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        });

        mSearchText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE){
                final String text = mSearchText.getText().toString();

                if (text.length() == 0) {
                    Utilities.ShowFailureActivity(mActivity, getString(R.string.alert_search_empty));
                    //CommonUtils.showToast(mActivity, getString(R.string.alert_search_empty));
                    return true;
                }
                runSearch(text);

                final InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });
    }

    private void runSearch(final String query) {
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressText.setVisibility(View.VISIBLE);
        mProgressText.setText(getString(R.string.searching));
        mSearchText.setVisibility(View.GONE);
        mSearchVoiceImage.setVisibility(View.GONE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        CommonUtils.executeAsync(new SearchPodcasts(this, query), (results) -> {
            if (results.size() == 1) {
                mProgressText.setText(getString(R.string.text_no_search_results));
                mProgressBar.setVisibility(View.GONE);
                mNavDrawer.getController().peekDrawer();
                return;
            }

            final int headerColor = Utilities.getHeaderColor(mActivity);

            final WearableRecyclerView rv = findViewById(R.id.search_results);
            rv.setLayoutManager(new LinearLayoutManager(mActivity));
            rv.setAdapter(new AddPodcastsAdapter(mActivity, results, headerColor));

            findViewById(R.id.search_results).setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            mProgressText.setVisibility(View.GONE);
            mNavDrawer.getController().peekDrawer();
            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });

/*
        final SearchPodcastsViewModel model = ViewModelProviders.of(this, new SearchPodcastsViewModelFactory(getApplication(), query)).get(SearchPodcastsViewModel.class);
        model.getResults().observe(this, results -> {
        });
*/
    }

    @Override
    public void onPause() {
        super.onPause();

        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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