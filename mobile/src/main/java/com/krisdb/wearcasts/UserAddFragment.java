package com.krisdb.wearcasts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;
import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.FetchPodcast;
import com.krisdb.wearcastslibrary.Interfaces;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.krisdb.wearcastslibrary.CommonUtils.isValidUrl;
import static com.krisdb.wearcastslibrary.CommonUtils.showToast;

public class UserAddFragment extends Fragment implements DataClient.OnDataChangedListener, CapabilityClient.OnCapabilityChangedListener  {

    private Activity mActivity;
    private static final int OPML_REQUEST_CODE = 42;
    private View mView;
    private ProgressBar mProgressOPML;
    private Boolean mWatchConnected = false;

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
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        setRetainInstance(true);

        mView = inflater.inflate(R.layout.fragment_user_add, container, false);
        mProgressOPML = mView.findViewById(R.id.import_opml_progress_bar);
        final Button btnImportPodcast = mView.findViewById(R.id.btn_import_podcast);
        final Button btnImportOPML = mView.findViewById(R.id.btn_import_opml);

        btnImportPodcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String title = ((TextView) mView.findViewById(R.id.tv_import_podcast_title)).getText() != null ?
                        ((TextView) mView.findViewById(R.id.tv_import_podcast_title)).getText().toString() : null;
                String link = ((TextView) mView.findViewById(R.id.tv_import_podcast_link)).getText().toString().trim();

                if (link.length() > 0) {
                    link = link.startsWith("http") == false ? "http://" + link.toLowerCase() : link.toLowerCase();
                    if (isValidUrl(link)) {
                        new FetchPodcast(title, link, new Interfaces.FetchPodcastResponse() {
                            @Override
                            public void processFinish(final PodcastItem podcast) {

                                new AsyncTasks.EpisodeCount(mActivity, podcast, new Interfaces.IntResponse() {
                                    @Override
                                    public void processFinish(int response) {
                                        final TextView tvTip = mView.findViewById(R.id.main_tip);
                                        if (response == 1) {
                                            Utilities.SendToWatch(mActivity, podcast);
                                            ((TextView) mView.findViewById(R.id.tv_import_podcast_title)).setText(null);
                                            ((TextView) mView.findViewById(R.id.tv_import_podcast_link)).setText(null);
                                            tvTip.setText(getString(R.string.tip_2));
                                            tvTip.setTextSize(14);
                                            tvTip.setTextColor(ContextCompat.getColor(mActivity, R.color.dark_grey));
                                        }
                                        else
                                        {
                                            final SpannableString content = new SpannableString(getString(R.string.error_no_episode));
                                            content.setSpan(new UnderlineSpan(), 49, content.length(), 0);
                                            tvTip.setText(content);
                                            tvTip.setTextSize(16);

                                            tvTip.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(getString(R.string.rss_validator_url))));
                                                }
                                            });

                                            tvTip.setTextColor(ContextCompat.getColor(mActivity, R.color.red));
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
            mWatchConnected= true;
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

        final Intent intent = mActivity.getIntent();

        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null)
            ((TextView) mView.findViewById(R.id.tv_import_podcast_link)).setText(intent.getStringExtra(Intent.EXTRA_TEXT));

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        final SharedPreferences.Editor editor = prefs.edit();

        final int visits = prefs.getInt("visits", 0) + 1;

        if (visits == 1) {
            ((TextView) mView.findViewById(R.id.main_tip)).setText(getString(R.string.tip_1));
            ((TextView) mView.findViewById(R.id.main_tip)).setTextColor(mActivity.getColor(R.color.red));
        } else
            ((TextView) mView.findViewById(R.id.main_tip)).setText(getString(R.string.tip_2));

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

    @Override
    public void onResume() {
        super.onResume();
        Wearable.getDataClient(mActivity).addListener(this);
        Wearable.getCapabilityClient(mActivity).addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);
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
                                    ((TextView) mView.findViewById(R.id.import_opml_text)).setGravity(Gravity.START);
                                    mView.findViewById(R.id.import_opml_text).setVisibility(View.VISIBLE);
                                    ((TextView) mView.findViewById(R.id.import_opml_text)).setText(mActivity.getString(R.string.text_importing_opml_parsing));
                                    ((TextView) mView.findViewById(R.id.import_opml_text)).setTextColor(mActivity.getColor(R.color.dark_grey));
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
        Wearable.getDataClient(mActivity).removeListener(this);
        Wearable.getCapabilityClient(mActivity).removeListener(this);
        super.onPause();
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {

    }
}