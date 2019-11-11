package com.krisdb.wearcasts.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.krisdb.wearcasts.BuildConfig;
import com.krisdb.wearcasts.R;

import java.util.Calendar;


public class AboutActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.page_title_about));

        ((TextView)findViewById(R.id.about_copyright)).setText(getString(R.string.copyright, String.valueOf(Calendar.getInstance().get(Calendar.YEAR))));
        ((TextView)findViewById(R.id.about_version)).setText(getString(R.string.version, BuildConfig.VERSION_NAME));

        final TextView tvPrivacy = (TextView)findViewById(R.id.about_privacy);
        final SpannableString content = new SpannableString(getString(R.string.privacy_policy));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        tvPrivacy.setText(content);

        tvPrivacy.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(getString(R.string.privacy_url)))));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
