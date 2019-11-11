package com.krisdb.wearcasts.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities;
import com.krisdb.wearcastslibrary.Async.EpisodeCount;
import com.krisdb.wearcastslibrary.Async.FetchPodcast;
import com.krisdb.wearcastslibrary.Async.NodesConnected;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Date;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.krisdb.wearcastslibrary.CommonUtils.isValidUrl;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class UserAddActivity extends AppCompatActivity {

    private Activity mActivity;
    private static final int OPML_REQUEST_CODE = 42;
    private ProgressBar mProgressOPML;
    private TextView mTipView, mOPMLView;
    private CheckBox mThirdPartyAutoDownload;
    private LocalBroadcastManager mBroadcastManger;
    private WeakReference<Activity> mActivityRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_add);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.page_title_import));

        mActivity = this;
        mActivityRef = new WeakReference<>(mActivity);

        mProgressOPML = findViewById(R.id.import_opml_progress_bar);
        mThirdPartyAutoDownload = findViewById(R.id.user_add_third_party_auto_download);
        mOPMLView = findViewById(R.id.import_opml_text);
        final Button btnImportPodcast = findViewById(R.id.btn_import_podcast);
        final Button btnImportOPML = findViewById(R.id.btn_import_opml);
        mTipView = findViewById(R.id.main_tip);
        mBroadcastManger = LocalBroadcastManager.getInstance(mActivity);

        btnImportPodcast.setOnClickListener(view -> {

            final String title = ((TextView) findViewById(R.id.tv_import_podcast_title)).getText() != null ?
                    ((TextView) findViewById(R.id.tv_import_podcast_title)).getText().toString() : null;
            String link = ((TextView) findViewById(R.id.tv_import_podcast_link)).getText().toString().trim();

            if (link.length() > 0) {
                link = link.startsWith("http") == false ? "http://" + link.toLowerCase() : link.toLowerCase();
                if (isValidUrl(link)) {
                    CommonUtils.showToast(mActivity, getString(R.string.alert_users_add_fetching_feed));

                    CommonUtils.executeSingleThreadAsync(new FetchPodcast(title, link), (podcast) -> {

                        CommonUtils.executeSingleThreadAsync(new EpisodeCount(mActivity, podcast), (count) -> {
                            if (count != 0) {
                                Utilities.SendToWatch(mActivity, podcast);
                                ((TextView) findViewById(R.id.tv_import_podcast_title)).setText(null);
                                ((TextView) findViewById(R.id.tv_import_podcast_link)).setText(null);
                                mTipView.setText(getString(R.string.tip_2));
                                mTipView.setTextSize(14);
                                mTipView.setTextColor(ContextCompat.getColor(mActivity, R.color.dark_grey));
                            }
                            else
                            {
                                final SpannableString content = new SpannableString(getString(R.string.error_no_episode));
                                content.setSpan(new UnderlineSpan(), 49, content.length(), 0);
                                mTipView.setText(content);
                                mTipView.setTextSize(16);

                                mTipView.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(getString(R.string.rss_validator_url)))));

                                mTipView.setTextColor(ContextCompat.getColor(mActivity, R.color.wc_red));
                            }
                        });
                    });

                } else
                    showToast(mActivity, getString(R.string.validation_invalid_url), Toast.LENGTH_LONG);
            } else
                showToast(mActivity, getString(R.string.validation_empty_url), Toast.LENGTH_LONG);
        });

        btnImportOPML.setOnClickListener(view -> {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
            final SharedPreferences.Editor editor = prefs.edit();

            final int imports = prefs.getInt("opml_imports", 0) + 1;

            if (imports == 1) {
                if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                    final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                    alert.setMessage(getString(R.string.confirm_import_opml));

                    alert.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        startActivityForResult(intent, OPML_REQUEST_CODE);

                        dialog.dismiss();
                    });
                    alert.show();
                }
            } else {
                final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, OPML_REQUEST_CODE);
            }

            //stop tracking at 100 visits
            if (imports < 100) {
                editor.putInt("opml_imports", imports);
                editor.apply();
            }
        });

        CommonUtils.executeSingleThreadAsync(new NodesConnected(this), this::SetContent);
    }

    private void SetContent(final Boolean connected)
    {
        if (connected) {
            findViewById(R.id.btn_import_podcast).setEnabled(true);
            findViewById(R.id.btn_import_opml).setEnabled(true);

            ((TextView)findViewById(R.id.btn_import_podcast)).setText(getString(R.string.button_text_import_podcast));
            ((TextView)findViewById(R.id.btn_import_opml)).setText(getString(R.string.button_text_import_opml));
        } else {
            ((TextView)findViewById(R.id.btn_import_podcast)).setText(getString(R.string.button_text_no_device));
            ((TextView)findViewById(R.id.btn_import_opml)).setText(getString(R.string.button_text_no_device));
        }
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        mThirdPartyAutoDownload.setChecked(prefs.getBoolean("third_party_auto_download", false));

        final Intent intent = mActivity.getIntent();

        if (intent.getType() != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            if (connected) {
                final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                try {
                    findViewById(R.id.user_add_third_party_progress).setVisibility(View.VISIBLE);
                    final JSONObject json = new JSONObject(text);

                    String url = null, title = null, description = null, message = null, pubDate = null;
                    int duration = 0;
                    final ImageView logo = findViewById(R.id.user_add_third_party_logo);

                    final PodcastItem episode = new PodcastItem();

                    //PLAYER FM
                    if (json.has("platform") && (json.get("platform")).equals("fm.player"))
                    {
                        url = (String) json.get("url");
                        title = (String) json.get("title");
                        description = (String) json.get("description");
                        duration = Integer.valueOf((String)json.get("duration"));

                        final String jsonDate = (String) json.get("publishedAt");
                        final Date jsonDate2 = new Date(Long.valueOf(jsonDate) * 1000);
                        final String date = DateUtils.FormatDate(jsonDate2, "EEE, dd MMM yyyy HH:mm:ss zzz");
                        pubDate = DateUtils.FormatDate(date);

                        message = getString(R.string.text_third_party_greeting, getString(R.string.third_party_title_playerfm), title);
                        episode.setPlaylistId(getResources().getInteger(R.integer.playlist_playerfm));
                        logo.setImageDrawable(mActivity.getDrawable(R.drawable.ic_thirdparty_logos_playerfm));
                    }
                    //third party: add to conditional populating title,url,description,pubdate,duration

                    episode.setTitle(title);
                    episode.setMediaUrl(url);
                    episode.setDescription(description);
                    episode.setPubDate(pubDate);
                    episode.setDuration(duration);

                    Utilities.sendEpisode(mActivity, episode, mThirdPartyAutoDownload.isChecked());
                    ((TextView)findViewById(R.id.user_add_third_party_message)).setText(message);

                    new CountDownTimer(5000, 100) {
                        public void onTick(long millisUntilFinished) {}

                        public void onFinish() {
                            if (findViewById(R.id.user_add_third_party_progress).getVisibility() == View.VISIBLE) {
                                findViewById(R.id.user_add_third_party_progress).setVisibility(View.GONE);
                                mOPMLView.setText(R.string.general_error);
                                mOPMLView.setVisibility(View.VISIBLE);
                                mOPMLView.setTextColor(ContextCompat.getColor(mActivity, R.color.wc_red));
                                mOPMLView.setTextSize(14);
                            }
                        }
                    }.start();
                } catch (Exception ex) {
                    ((TextView) findViewById(R.id.tv_import_podcast_link)).setText(intent.getStringExtra(Intent.EXTRA_TEXT));
                    findViewById(R.id.user_add_third_party_progress).setVisibility(View.GONE);
                }
            }
            else
            {
                ((TextView)findViewById(R.id.user_add_third_party_message)).setText(getString(R.string.text_third_party_no_watch));
                ((TextView)findViewById(R.id.user_add_third_party_message)).setTextColor(ContextCompat.getColor(mActivity, R.color.wc_red));
                findViewById(R.id.user_add_third_party_layout).setVisibility(View.VISIBLE);
                mThirdPartyAutoDownload.setVisibility(View.GONE);
            }
        }

        final SharedPreferences.Editor editor = prefs.edit();

        mThirdPartyAutoDownload.setOnClickListener(view -> {
            editor.putBoolean("third_party_auto_download", mThirdPartyAutoDownload.isChecked());
            editor.apply();
        });

        final int visits = prefs.getInt("visits", 0) + 1;

        if (visits == 1) {
            mTipView.setText(getString(R.string.tip_1));
            mTipView.setTextColor(mActivity.getColor(R.color.wc_red));
        } else
            mTipView.setText(getString(R.string.tip_2));

        //stop tracking at 100 visits
        if (visits < 100)
        {
            editor.putInt("visits", visits);
            editor.apply();
        }
    }

    private BroadcastReceiver mWatchResponse = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Bundle extras = intent.getExtras();
             if (extras.getBoolean("thirdparty")) {
                 findViewById(R.id.user_add_third_party_layout).setVisibility(View.VISIBLE);
                 findViewById(R.id.user_add_third_party_progress).setVisibility(View.GONE);
             }
            else if (extras.getBoolean("opmlimport_complete")) {
                 mProgressOPML.setVisibility(View.GONE);
                 mOPMLView.setGravity(Gravity.START);
                 mOPMLView.setVisibility(View.VISIBLE);
                 mOPMLView.setText(getString(R.string.text_importing_opml_complete));
                 mOPMLView.setTextColor(mActivity.getColor(R.color.wc_general_green));
                 getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                 final Intent notificationIntent = new Intent(mActivity, DirectoryActivity.class);
                 notificationIntent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                 final PendingIntent intentDirectory = PendingIntent.getActivity(mActivity, 5, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                     final String channelID = getPackageName().concat(".opmlcomplete");

                     final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                     final NotificationChannel channel = new NotificationChannel(channelID, getString(R.string.notification_channel_opml_import_complete), NotificationManager.IMPORTANCE_DEFAULT);
                     notificationManager.createNotificationChannel(channel);

                     final Notification notification = new NotificationCompat.Builder(mActivity, channelID)
                             .setSmallIcon(R.drawable.ic_notification)
                             .setContentTitle(getString(R.string.notification_opml_import_complete_title))
                             .setContentIntent(intentDirectory)
                             .build();

                     notificationManager.notify(122, notification);
                 } else {
                     final NotificationCompat.Builder notificationBuilder =
                             new NotificationCompat.Builder(mActivity)
                                     .setSmallIcon(R.drawable.ic_notification)
                                     .setContentIntent(intentDirectory)
                                     .setContentTitle(getString(R.string.notification_opml_import_complete_title));

                     NotificationManagerCompat.from(mActivity).notify(122, notificationBuilder.build());
                 }
             }
            else if (extras.getBoolean("opmlimport_podcasts")) {
                 mProgressOPML.setVisibility(View.VISIBLE);
                 mOPMLView.setGravity(Gravity.START);
                 mOPMLView.setVisibility(View.VISIBLE);
                 mOPMLView.setText(getString(R.string.text_importing_opml_parsing, extras.getString("podcast_title")));
                 mOPMLView.setTextColor(mActivity.getColor(R.color.wc_gray));
             }
            else if (extras.getBoolean("opmlimport_episodes")) {
                 mProgressOPML.setVisibility(View.VISIBLE);
                 mOPMLView.setGravity(Gravity.START);
                 mOPMLView.setVisibility(View.VISIBLE);
                 mOPMLView.setText(getString(R.string.text_importing_opml_parsing, extras.getString("podcast_title_episodes")));
                 mOPMLView.setTextColor(mActivity.getColor(R.color.wc_gray));
             }
            else if (extras.getBoolean("opmlimport_art")) {
                 mProgressOPML.setVisibility(View.VISIBLE);
                 mOPMLView.setGravity(Gravity.START);
                 mOPMLView.setVisibility(View.VISIBLE);
                 mOPMLView.setText(getString(R.string.text_importing_opml_parsing, extras.getString("podcast_title_art")));
                 mOPMLView.setTextColor(mActivity.getColor(R.color.wc_gray));
             }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mBroadcastManger.registerReceiver(mWatchResponse, new IntentFilter("watchresponse"));
    }

    @Override
    public void onStop() {
        super.onStop();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onPause() {
        //mBroadcastManger.unregisterReceiver(mWatchResponse);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mBroadcastManger.unregisterReceiver(mWatchResponse);
        super.onDestroy();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (resultCode == RESULT_OK)
        {
            if (requestCode == OPML_REQUEST_CODE) {
                new Thread(() -> {
                    if (resultData != null) {
                        Uri uri = resultData.getData();

                        PutDataMapRequest dataMap = PutDataMapRequest.create("/opmlimport");
                        dataMap.getDataMap().putAsset("opml", Asset.createFromUri(uri));

                        CommonUtils.DeviceSync(mActivity, dataMap);

                        mActivity.runOnUiThread(() -> {
                            mProgressOPML.setVisibility(View.VISIBLE);
                            mProgressOPML.setIndeterminate(true);
                            mOPMLView.setGravity(Gravity.START);
                            mOPMLView.setVisibility(View.VISIBLE);
                            mOPMLView.setText(getString(R.string.text_importing_opml_sent));
                            mOPMLView.setTextColor(mActivity.getColor(R.color.dark_grey));
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        });
                    }
                }).start();
            }
        }
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