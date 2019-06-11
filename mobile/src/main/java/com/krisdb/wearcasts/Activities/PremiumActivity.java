package com.krisdb.wearcasts.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities;
import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.Purchase.PurchaseState.PURCHASED;

public class PremiumActivity extends AppCompatActivity implements PurchasesUpdatedListener {

    private Activity mActivity;
    private static final int UPLOAD_REQUEST_CODE = 43;
    private TextView tvUploadSummary, mPremiumInstructionsText, mPremiumInstructionsReview;
    private static WeakReference<ProgressBar> mProgressFileUpload;
    private Boolean mPremiumUnlocked = false;
    private LocalBroadcastManager mBroadcastManger;
    private Button mPremiumButton, mPlaylistsReadd;
    private Spinner mPlaylistSkus;
    private int mPlaylistPurchasedCount = 0;
    private WeakReference<PremiumActivity> mActivityRef;
    private BillingClient mBillingClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);
        setTitle(getString(R.string.page_title_premium));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mActivity = this;

        mActivityRef = new WeakReference<>(this);

        mProgressFileUpload = new WeakReference<>((ProgressBar) findViewById(R.id.upload_file_progress));
        mPlaylistsReadd = findViewById(R.id.btn_playlists_readd);
        mPremiumInstructionsText = findViewById(R.id.premium_instructions);
        mPremiumInstructionsReview = findViewById(R.id.premium_ratereview);
        mPlaylistSkus = findViewById(R.id.playlist_buy_qty);
        mBroadcastManger = LocalBroadcastManager.getInstance(mActivity);
        tvUploadSummary = findViewById(R.id.upload_file_summary);
        mPremiumButton = findViewById(R.id.btn_unlock_premium);

        mPremiumInstructionsReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.google_play_url)));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_upload_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(intent, UPLOAD_REQUEST_CODE);
            }
        });

        new AsyncTasks.WatchConnected(mActivity,
                new Interfaces.BooleanResponse() {
                    @Override
                    public void processFinish(Boolean connected) {
                        if (!connected) {
                            findViewById(R.id.btn_playlist_buy).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    CommonUtils.showToast(mActivity, getString(R.string.button_text_no_device));
                                }
                            });

                            mPremiumButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    CommonUtils.showToast(mActivity, getString(R.string.button_text_no_device));
                                }
                            });
                        } else {

                            findViewById(R.id.btn_playlist_buy).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (mPlaylistSkus.getSelectedItemPosition() == 0)
                                        CommonUtils.showToast(mActivity, getString(R.string.alert_playlists_quantity_none));
                                    else if (mPlaylistPurchasedCount > 0) {
                                        if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                                            final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                                            alert.setMessage(getString(R.string.alert_playlists_purchase_disclaimer));
                                            alert.setPositiveButton(mActivity.getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    showPlaylistPurchase();
                                                }
                                            });

                                            alert.setNegativeButton(mActivity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            }).show();
                                        }
                                    } else
                                        showPlaylistPurchase();
                                }
                            });

                            mPremiumButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                final List<String> skuList = new ArrayList<>();
                                skuList.add(getString(R.string.inapp_premium_product_id));

                                final SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder()
                                        .setSkusList(skuList)
                                        .setType(BillingClient.SkuType.INAPP);

                                mBillingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
                                    @Override
                                    public void onSkuDetailsResponse(final BillingResult billingResult, final List<SkuDetails> skuDetailsList) {

                                        for (final SkuDetails skuDetails : skuDetailsList) {
                                            if (getString(R.string.inapp_premium_product_id).equals(skuDetails.getSku())) {

                                                final BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                                        .setSkuDetails(skuDetails)
                                                        .build();

                                                final BillingResult result = mBillingClient.launchBillingFlow(mActivity, flowParams);

                                                if (result.getResponseCode() == ITEM_ALREADY_OWNED) {
                                                    CommonUtils.showToast(mActivity, getString(R.string.alert_purchased));
                                                } else if (result.getResponseCode() != OK) {
                                                    CommonUtils.showToast(mActivity, getString(R.string.general_error).concat("\n").concat("(").concat(result.getDebugMessage()).concat(")"));
                                                }
                                            }
                                        }
                                    }
                                });
                                }
                            });
                        }
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        mBillingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener(this).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == OK) {

                    final Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);

                    if (purchasesResult.getResponseCode() == OK) {

                        final List<Purchase> purchases = purchasesResult.getPurchasesList();

                        final List<Integer> playlistsPurchased = new ArrayList<>(); //maybe more then one playlist purchased, so get the largest.

                        for (final Purchase purchase : purchases) {
                            if (purchase.getSku().equals(getString(R.string.inapp_premium_product_id)))
                                mPremiumUnlocked = true;
                            else if (purchase.getSku().contains("playlist")) {
                                final String[] playlist = purchase.getSku().split("_");

                                if (playlist.length > 0)
                                    playlistsPurchased.add(Integer.valueOf(playlist[1]));
                            }
                        }

                        if (playlistsPurchased.size() > 0) {
                            Collections.sort(playlistsPurchased);
                            mPlaylistPurchasedCount = playlistsPurchased.get(playlistsPurchased.size() - 1);
                        }

                        SetPremiumContent();
                        Utilities.TogglePremiumOnWatch(mActivity, mPremiumUnlocked, false);

                        if (mPlaylistPurchasedCount > 0) {
                            mPlaylistsReadd.setVisibility(View.VISIBLE);
                            mPlaylistsReadd.setText(getString(R.string.button_text_playlists_readd, mPlaylistPurchasedCount));
                            mPlaylistsReadd.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                                        final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                                        alert.setMessage(getString(R.string.alert_playlists_readd_disclaimer));
                                        alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                sendPlaylistsToWatch(mPlaylistPurchasedCount);
                                            }
                                        });

                                        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        }).show();
                                    }
                                }
                            });
                        } else
                            mPlaylistsReadd.setVisibility(View.INVISIBLE);
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
            }
        });

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = prefs.edit();

        final int visits = prefs.getInt("visits", 0) + 1;

        //stop tracking at 100 visits
        if (visits < 100) {
            editor.putInt("visits", visits);
            editor.apply();
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == OK && purchases != null) {
            for (final Purchase purchase : purchases) {
                if (purchase.getPurchaseState() == PURCHASED) {
                    if (purchase.getSku().equals(getString(R.string.inapp_premium_product_id))) {
                        mPremiumUnlocked = true;
                        Utilities.TogglePremiumOnWatch(mActivity, mPremiumUnlocked, true);
                        SetPremiumContent();
                        mPremiumInstructionsText.setVisibility(View.VISIBLE);
                        mPremiumInstructionsReview.setVisibility(View.VISIBLE);
                        mPremiumInstructionsText.setText(getString(R.string.alert_premium_purchased));
                    } else if (purchase.getSku().contains("playlist")) {
                        sendPlaylistsToWatch(mPlaylistSkus.getSelectedItemPosition());
                    }

                    if (!purchase.isAcknowledged()) {
                        final AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

                        mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                            @Override
                            public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                                if (billingResult.getResponseCode() == OK) {
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    private void showPlaylistPurchase() {

        final List<String> skuList = new ArrayList<>();
        skuList.add("playlist_" + mPlaylistSkus.getSelectedItemPosition());

        final SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP);

        mBillingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(final BillingResult billingResult, final List<SkuDetails> skuDetailsList) {

                for (final SkuDetails skuDetails : skuDetailsList) {
                    if (skuDetails.getSku().contains("playlist")) {
                        final BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetails)
                                .build();

                        final BillingResult result = mBillingClient.launchBillingFlow(mActivity, flowParams);

                        if (result.getResponseCode() == ITEM_ALREADY_OWNED) {
                            CommonUtils.showToast(mActivity, getString(R.string.alert_purchased));
                        } else if (result.getResponseCode() != OK) {
                            CommonUtils.showToast(mActivity, getString(R.string.general_error).concat("\n").concat("(").concat(result.getDebugMessage()).concat(")"));
                        }
                    }
                }
            }
        });
    }

    private void SetPremiumContent()
    {
        boolean isDebuggable = ( 0 != ( getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE ) );

        mPlaylistsReadd.setVisibility(mPlaylistPurchasedCount  > 0 ? View.VISIBLE : View.INVISIBLE);

        if (isDebuggable || mPremiumUnlocked) {
            findViewById(R.id.btn_upload_file).setEnabled(true);
            tvUploadSummary.setText(mActivity.getString(R.string.upload_file_summary_unlocked));
            mPremiumButton.setText(mActivity.getString(R.string.button_text_resync_premium));
            mPremiumButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mProgressFileUpload.get().setVisibility(View.VISIBLE);
                    CommonUtils.showToast(mActivity, mActivity.getString(R.string.button_text_resync_premium_waiting), Toast.LENGTH_LONG);
                    Utilities.TogglePremiumOnWatch(mActivity, true, true);
                }
            });
        }
        else {
            findViewById(R.id.btn_upload_file).setEnabled(false);
            tvUploadSummary.setText(mActivity.getString(R.string.upload_file_summary_locked));
        }
    }

    private void sendPlaylistsToWatch(final int count)
    {
        final PutDataMapRequest dataMap = PutDataMapRequest.create("/addplaylists");
        dataMap.getDataMap().putInt("number", count);
        CommonUtils.DeviceSync(mActivity, dataMap);
        mPremiumInstructionsText.setVisibility(View.VISIBLE);
        mPremiumInstructionsReview.setVisibility(View.VISIBLE);
        mPremiumInstructionsText.setText(getString(R.string.alert_playlists_purchased));
    }

    @Override
    public void onResume() {
        super.onResume();
        mBroadcastManger.registerReceiver(mFileUploadReceiver, new IntentFilter("file_uploaded"));
        mBroadcastManger.registerReceiver(mWatchResponse, new IntentFilter("watchresponse"));

        SetPremiumContent();
    }

    private BroadcastReceiver mWatchResponse = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras().getBoolean("premium")) {
                CommonUtils.showToast(context, context.getString(R.string.success));
                mProgressFileUpload.get().setVisibility(View.GONE);
            }
        }
    };

    private BroadcastReceiver mFileUploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final Bundle extras = intent.getExtras();
            if (extras.getBoolean("started")) {
                tvUploadSummary.setText(mActivity.getString(R.string.text_file_upload_start));
                tvUploadSummary.setTextColor(mActivity.getColor(R.color.dark_grey));

                //mProgressOPML.setIndeterminate(false);
                //mProgressFileUpload.setMax(extras.getInt("length"));
            }
            else if (extras.getBoolean("processing"))
            {
                //mProgressOPML.setIndeterminate(false);
                //mProgressFileUpload.setProgress(extras.getInt("progress"));
            }
            else if (extras.getBoolean("finished")) {
                mProgressFileUpload.get().setVisibility(View.GONE);
                tvUploadSummary.setText(getString(R.string.text_file_uploaded));
                tvUploadSummary.setTextColor(mActivity.getColor(R.color.green));
            }
        }
    };

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == RESULT_OK) {
            if (requestCode == UPLOAD_REQUEST_CODE) {
                final ClipData clipData = resultData.getClipData();

                if (clipData == null) {
                    final Uri uriLocal = resultData.getData();
                    new SendFileToWatch(mActivity, uriLocal).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {

                    final int count = clipData.getItemCount();

                    for (int i = 0; i < count; i++) {
                        final ClipData.Item item = clipData.getItemAt(i);
                        new SendFileToWatch(mActivity, item.getUri()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            }
        }
    }

    public static class SendFileToWatch extends AsyncTask<Void, Void, Void> {
        private static Uri mUri;
        private static WeakReference<Activity> mActivity;

        SendFileToWatch(final Activity activity, final Uri uri)
        {
            mActivity = new WeakReference<>(activity);
            mUri = uri;
        }

        @Override
        protected void onPreExecute() {
            mProgressFileUpload.get().setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {

            /*
            final Task<List<Node>> nodeListTask = Wearable.getNodeClient(mActivity.get()).getConnectedNodes();

            try {
                List<Node> nodes = Tasks.await(nodeListTask);

                Task<ChannelClient.Channel> task = Wearable.getChannelClient(mActivity.get()).openChannel(nodes.get(0).getId(),"");

                Task<Void> task2 = Wearable.getChannelClient(mActivity.get()).sendFile(task.getResult(), mUri);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

*/
            final PutDataMapRequest dataMapUpload = PutDataMapRequest.create("/uploadfile");

            final Asset asset = Asset.createFromUri(mUri);
            dataMapUpload.getDataMap().putAsset("local_file", asset);
            String localFileName = null;

            if (mUri.toString().startsWith("content://")) {
                try (Cursor cursor = mActivity.get().getContentResolver().query(mUri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        localFileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                }
            } else if (mUri.toString().startsWith("file://")) {
                final File myFile = new File(mUri.toString());
                localFileName = myFile.getName();
            }

            dataMapUpload.getDataMap().putString("local_filename", localFileName);

            CommonUtils.DeviceSync(mActivity.get(), dataMapUpload);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBillingClient != null && mBillingClient.isReady()) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
    }

    @Override
    public void onPause() {
        mBroadcastManger.unregisterReceiver(mFileUploadReceiver);
        mBroadcastManger.unregisterReceiver(mWatchResponse);
        super.onPause();
    }
}