package com.krisdb.wearcasts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.FetchPodcast;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.krisdb.wearcastslibrary.CommonUtils.isValidUrl;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class UserAddFragment extends Fragment {

    private Activity mActivity;
    private static final int OPML_REQUEST_CODE = 42;
    private View mView;
    private ProgressBar mProgressOPML;
    private Boolean mWatchConnected = false;
    private TextView mTipView, mOPMLView;
    private CheckBox mThirdPartyAutoDownload;
    private LocalBroadcastManager mBroadcastManger;
    private WeakReference<Activity> mActivityRef;

    public static UserAddFragment newInstance(final Boolean connected) {

        final UserAddFragment f = new UserAddFragment();
        final Bundle bundle = new Bundle();
        bundle.putBoolean("connected", connected);
        f.setArguments(bundle);

       return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mActivityRef = new WeakReference<>(mActivity);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        setRetainInstance(true);

        mView = inflater.inflate(R.layout.fragment_user_add, container, false);
        mProgressOPML = mView.findViewById(R.id.import_opml_progress_bar);
        mThirdPartyAutoDownload = mView.findViewById(R.id.user_add_third_party_auto_download);
        mOPMLView = mView.findViewById(R.id.import_opml_text);
        final TextView mThirdPartyView = mView.findViewById(R.id.user_add_third_party_message);
        final Button btnImportPodcast = mView.findViewById(R.id.btn_import_podcast);
        final Button btnImportOPML = mView.findViewById(R.id.btn_import_opml);
        mTipView = mView.findViewById(R.id.main_tip);
        mBroadcastManger = LocalBroadcastManager.getInstance(mActivity);

        btnImportPodcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String title = ((TextView) mView.findViewById(R.id.tv_import_podcast_title)).getText() != null ?
                        ((TextView) mView.findViewById(R.id.tv_import_podcast_title)).getText().toString() : null;
                String link = ((TextView) mView.findViewById(R.id.tv_import_podcast_link)).getText().toString().trim();

                if (link.length() > 0) {
                    link = link.startsWith("http") == false ? "http://" + link.toLowerCase() : link.toLowerCase();
                    if (isValidUrl(link)) {
                        CommonUtils.showToast(mActivity, getString(R.string.alert_users_add_fetching_feed));
                        new FetchPodcast(title, link, new Interfaces.FetchPodcastResponse() {
                            @Override
                            public void processFinish(final PodcastItem podcast) {
                                new AsyncTasks.EpisodeCount(mActivity, podcast, new Interfaces.IntResponse() {
                                    @Override
                                    public void processFinish(int response) {
                                        if (response != 0) {
                                            Utilities.SendToWatch(mActivity, podcast);
                                            ((TextView) mView.findViewById(R.id.tv_import_podcast_title)).setText(null);
                                            ((TextView) mView.findViewById(R.id.tv_import_podcast_link)).setText(null);
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

                                            mTipView.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(getString(R.string.rss_validator_url))));
                                                }
                                            });

                                            mTipView.setTextColor(ContextCompat.getColor(mActivity, R.color.red));
                                        }
                                    }
                                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }

                            @Override
                            public void processFinish(final List<PodcastItem> podcasts) {
                                sendToWatch(podcasts);
                            }
                        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                    } else
                        showToast(mActivity, getString(R.string.validation_invalid_url), Toast.LENGTH_LONG);
                } else
                    showToast(mActivity, getString(R.string.validation_empty_url), Toast.LENGTH_LONG);
            }
        });

        btnImportOPML.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
                final SharedPreferences.Editor editor = prefs.edit();

                final int imports = prefs.getInt("opml_imports", 0) + 1;

                if (imports == 1) {
                    if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                        final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                        alert.setMessage(getString(R.string.confirm_import_opml));

                        alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("*/*");
                                startActivityForResult(intent, OPML_REQUEST_CODE);

                                dialog.dismiss();
                            }
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
            }
        });

        if (getArguments() != null) {
            mWatchConnected = getArguments().getBoolean("connected");
        }

        if (mWatchConnected) {
            btnImportPodcast.setEnabled(true);
            btnImportOPML.setEnabled(true);
            btnImportPodcast.setText(getString(R.string.button_text_import_podcast));
            btnImportOPML.setText(getString(R.string.button_text_import_opml));
        } else {
            btnImportPodcast.setText(getString(R.string.button_text_no_device));
            btnImportOPML.setText(getString(R.string.button_text_no_device));
        }
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        mThirdPartyAutoDownload.setChecked(prefs.getBoolean("third_party_audo_download", false));

        final Intent intent = mActivity.getIntent();

        if (intent.getType() != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            if (mWatchConnected) {
                final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                try {
                    mView.findViewById(R.id.user_add_third_party_progress).setVisibility(View.VISIBLE);
                    final JSONObject json = new JSONObject(text);

                    String url = null, title = null, description = null, message = null, pubDate = null;
                    Integer duration = 0;
                    final ImageView logo = mView.findViewById(R.id.user_add_third_party_logo);

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
                    mThirdPartyView.setText(message);

                    new CountDownTimer(5000, 100) {
                        public void onTick(long millisUntilFinished) {}

                        public void onFinish() {
                            if (mView.findViewById(R.id.user_add_third_party_progress).getVisibility() == View.VISIBLE) {
                                mView.findViewById(R.id.user_add_third_party_progress).setVisibility(View.GONE);
                                mOPMLView.setText(R.string.general_error);
                                mOPMLView.setVisibility(View.VISIBLE);
                                mOPMLView.setTextColor(ContextCompat.getColor(mActivity, R.color.red));
                                mOPMLView.setTextSize(14);
                            }
                        }
                    }.start();
                } catch (Exception ex) {
                    ((TextView) mView.findViewById(R.id.tv_import_podcast_link)).setText(intent.getStringExtra(Intent.EXTRA_TEXT));
                    mView.findViewById(R.id.user_add_third_party_progress).setVisibility(View.GONE);
                }
            }
            else
            {
                mThirdPartyView.setText(getString(R.string.text_third_party_no_watch));
                mThirdPartyView.setTextColor(ContextCompat.getColor(mActivity, R.color.red));
                mView.findViewById(R.id.user_add_third_party_layout).setVisibility(View.VISIBLE);
                mThirdPartyAutoDownload.setVisibility(View.GONE);
            }
        }

        final SharedPreferences.Editor editor = prefs.edit();

        mThirdPartyAutoDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putBoolean("third_party_audo_download", mThirdPartyAutoDownload.isChecked());
                editor.apply();
            }
        });

        final int visits = prefs.getInt("visits", 0) + 1;

        if (visits == 1) {
            mTipView.setText(getString(R.string.tip_1));
            mTipView.setTextColor(mActivity.getColor(R.color.red));
        } else
            mTipView.setText(getString(R.string.tip_2));

        //stop tracking at 100 visits
        if (visits < 100)
        {
            editor.putInt("visits", visits);
            editor.apply();
        }

        return mView;
    }

    @Override
    public void onActivityCreated(final Bundle icicle)
    {
        super.onActivityCreated(icicle);
    }

    private BroadcastReceiver mWatchResponse = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
             if (intent.getExtras().getBoolean("thirdparty")) {
                 mView.findViewById(R.id.user_add_third_party_layout).setVisibility(View.VISIBLE);
                 mView.findViewById(R.id.user_add_third_party_progress).setVisibility(View.GONE);            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mBroadcastManger.registerReceiver(mWatchResponse, new IntentFilter("watchresponse"));
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        if (resultCode == RESULT_OK)
        {
            if (requestCode == OPML_REQUEST_CODE) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (resultData != null) {
                            Uri uri = resultData.getData();

                            InputStream in = null;
                            try {
                                in = mActivity.getContentResolver().openInputStream(uri);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }

                            mActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    mProgressOPML.setVisibility(View.VISIBLE);
                                    mProgressOPML.setIndeterminate(true);
                                    mOPMLView.setGravity(Gravity.START);
                                    mOPMLView.setVisibility(View.VISIBLE);
                                    mOPMLView.setText(mActivity.getString(R.string.text_importing_opml_parsing));
                                    mOPMLView.setTextColor(mActivity.getColor(R.color.dark_grey));
                                }
                            });

                            final List<PodcastItem> podcasts = OPMLParser.parse(mActivity, in);

                            if (podcasts.size() > 0) {

                                new FetchPodcast(podcasts, new Interfaces.FetchPodcastResponse() {
                                    @Override
                                    public void processFinish(PodcastItem podcast) {
                                        Utilities.SendToWatch(mActivity, podcast);
                                        ((TextView) mView.findViewById(R.id.tv_import_podcast_title)).setText(null);
                                        ((TextView) mView.findViewById(R.id.tv_import_podcast_link)).setText(null);
                                    }

                                    @Override
                                    public void processFinish(List<PodcastItem> podcasts) {
                                        sendToWatch(podcasts);
                                    }
                                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            } else {
                                mActivity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        mProgressOPML.setVisibility(View.INVISIBLE);
                                        ((TextView) mView.findViewById(R.id.import_opml_text)).setText(getString(R.string.error_opml_import));
                                        ((TextView) mView.findViewById(R.id.import_opml_text)).setTextColor(mActivity.getColor(R.color.red));
                                    }
                                });
                            }
                        }
                    }
                }).start();
            }
        }
        //else
            //CommonUtils.showToast(mActivity, mActivity.getString(R.string.general_error));
    }

    private void sendToWatch(final List<PodcastItem> podcasts)
    {
        mProgressOPML.setIndeterminate(false);
        ((TextView)mView.findViewById(R.id.import_opml_text)).setText(getString(R.string.text_importing_opml_sending));
        ((TextView)mView.findViewById(R.id.import_opml_text)).setTextColor(mActivity.getColor(R.color.dark_grey));

        new com.krisdb.wearcasts.AsyncTasks.SendToWatch(mActivity, podcasts, mProgressOPML,
                new Interfaces.PodcastsResponse() {
                    @Override
                    public void processFinish(final List<PodcastItem> podcasts) {
                        mProgressOPML.setVisibility(View.INVISIBLE);

                        final TextView opmlText = mView.findViewById(R.id.import_opml_text);
                        opmlText.setText(getResources().getQuantityString(R.plurals.podcasts_added, podcasts.size(), podcasts.size()));
                        opmlText.setTextColor(mActivity.getColor(R.color.green));
                        opmlText.setGravity(Gravity.CENTER_HORIZONTAL);
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        mBroadcastManger.unregisterReceiver(mWatchResponse);
        super.onPause();
    }
}