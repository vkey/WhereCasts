package com.krisdb.wearcasts.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.wear.widget.drawer.WearableNavigationDrawerView;

import com.krisdb.wearcasts.Adapters.NavigationAdapter;
import com.krisdb.wearcasts.Models.NavItem;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

public class SearchEpisodesActivity extends BaseFragmentActivity implements WearableNavigationDrawerView.OnItemSelectedListener {
    private static int SPEECH_REQUEST_CODE = 1;
    private TextView mProgressText;
    private Activity mActivity;
    private View mSearchVoiceImage;
    private EditText mSearchText;
    private ArrayList<NavItem> mNavItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_directory_activity);
        mActivity = this;

        mProgressText = findViewById(R.id.search_progress_text);
        mSearchText = findViewById(R.id.search_action_text);
        mSearchVoiceImage = findViewById(R.id.search_action_voice);

        final WearableNavigationDrawerView navDrawer = findViewById(R.id.drawer_nav_search);

        mNavItems = new ArrayList<>();
        final NavItem navItemSearch = new NavItem();
        navItemSearch.setID(0);
        navItemSearch.setTitle(getString(R.string.search));
        navItemSearch.setIcon("ic_action_search");
        mNavItems.add(navItemSearch);

        navDrawer.setAdapter(new NavigationAdapter(this, mNavItems));
        navDrawer.addOnItemSelectedListener(this);

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

    private void runSearch(final String query)
    {
        final Intent data = new Intent();
        data.setData(Uri.parse(query));
        setResult(RESULT_OK, data);
        finish();
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