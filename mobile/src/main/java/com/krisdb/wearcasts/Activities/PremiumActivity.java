package com.krisdb.wearcasts.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcasts.R;
import com.krisdb.wearcasts.Utilities;
import com.krisdb.wearcastslibrary.Async.SendFileToWatch;
import com.krisdb.wearcastslibrary.Async.WatchConnected;
import com.krisdb.wearcastslibrary.CommonUtils;

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
    private TextView tvUploadSummary;
    private static WeakReference<ProgressBar> mProgressFileUpload;
    private Boolean mPremiumSubUnlocked = false, mPremiumInappUnlocked = false;
    private LocalBroadcastManager mBroadcastManger;
    private Button mPremiumButton, mPlaylistsReadd, mPlaylistsButButton;
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

        mProgressFileUpload = new WeakReference<>(findViewById(R.id.upload_file_progress));
        mPlaylistsReadd = findViewById(R.id.btn_playlists_readd);
        mPlaylistsButButton = findViewById(R.id.btn_playlist_buy);
        mPlaylistSkus = findViewById(R.id.playlist_buy_qty);
        mBroadcastManger = LocalBroadcastManager.getInstance(mActivity);
        tvUploadSummary = findViewById(R.id.upload_file_summary);
        mPremiumButton = findViewById(R.id.btn_unlock_premium);

        findViewById(R.id.btn_upload_file).setOnClickListener(view -> {
            final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, UPLOAD_REQUEST_CODE);
        });

        CommonUtils.executeAsync(new WatchConnected(mActivity), (connected) -> {
            if (!connected) {
                mPlaylistsButButton.setOnClickListener(view -> CommonUtils.showSnackbar(mPlaylistsButButton, getString(R.string.button_text_no_device)));

                mPremiumButton.setOnClickListener(view -> CommonUtils.showSnackbar(mPremiumButton, getString(R.string.button_text_no_device)));
            } else {

                mPlaylistsButButton.setOnClickListener(view -> {
                    if (mPlaylistSkus.getSelectedItemPosition() == 0)
                        CommonUtils.showSnackbar(mPlaylistsButButton, getString(R.string.alert_playlists_quantity_none));
                    else if (mPlaylistPurchasedCount > 0) {
                        if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                            final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                            alert.setMessage(getString(R.string.alert_playlists_purchase_disclaimer));
                            alert.setPositiveButton(mActivity.getString(R.string.ok), (dialog, which) -> {
                                if (mPremiumInappUnlocked)
                                    showPlaylistPurchaseInapp();
                                else
                                    showPlaylistPurchaseSub();
                            });

                            alert.setNegativeButton(mActivity.getString(R.string.cancel), (dialog, which) -> dialog.dismiss()).show();
                        }
                    } else
                    if (mPremiumInappUnlocked)
                        showPlaylistPurchaseInapp();
                    else
                        showPlaylistPurchaseSub();
                });

                mPremiumButton.setOnClickListener(view -> {
                    final List<String> skuList = new ArrayList<>();
                    skuList.add(getString(R.string.sub_premium_product_id));

                    final SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder()
                            .setSkusList(skuList)
                            .setType(BillingClient.SkuType.SUBS);
                    //.setType(BillingClient.SkuType.INAPP);

                    mBillingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
                        @Override
                        public void onSkuDetailsResponse(final BillingResult billingResult, final List<SkuDetails> skuDetailsList) {

                            for (final SkuDetails skuDetails : skuDetailsList) {
                                if (getString(R.string.sub_premium_product_id).equals(skuDetails.getSku())) {

                                    final BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                            .setSkuDetails(skuDetails)
                                            .build();

                                    final BillingResult result = mBillingClient.launchBillingFlow(mActivity, flowParams);

                                    if (result.getResponseCode() == ITEM_ALREADY_OWNED) {
                                        CommonUtils.showSnackbar(mPremiumButton, getString(R.string.alert_purchased));
                                    } else if (result.getResponseCode() != OK) {
                                        CommonUtils.showSnackbar(mPremiumButton, getString(R.string.general_error).concat("\n").concat("(").concat(result.getDebugMessage()).concat(")"));
                                    }
                                }
                            }
                        }
                    });
                });
            }
        });


        mBillingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener(this).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == OK) {

                    final Purchase.PurchasesResult purchasesResultInapp = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);

                    if (purchasesResultInapp.getResponseCode() == OK) {
                        final List<Purchase> purchases = purchasesResultInapp.getPurchasesList();
                        final List<Integer> playlistsPurchased = new ArrayList<>(); //maybe more then one playlist purchased, so get the largest.
                        for (final Purchase purchase : purchases) {
                            if (purchase.getSku().equals(getString(R.string.inapp_premium_product_id)))
                                mPremiumInappUnlocked = true;
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
                    }

                    final Purchase.PurchasesResult purchasesResultSubs = mBillingClient.queryPurchases(BillingClient.SkuType.SUBS);

                    if (purchasesResultSubs.getResponseCode() == OK) {

                        final List<Purchase> purchases = purchasesResultSubs.getPurchasesList();

                        for (final Purchase purchase : purchases) {
                            if (purchase.getSku().equals(getString(R.string.sub_premium_product_id)))
                                mPremiumSubUnlocked = true;
                        }

                        SetPremiumContent();
                        Utilities.TogglePremiumOnWatch(mActivity, mPremiumInappUnlocked || mPremiumSubUnlocked, false);

                        if (mPlaylistPurchasedCount > 0) {
                            mPlaylistsReadd.setVisibility(View.VISIBLE);
                            mPlaylistsReadd.setText(getString(R.string.button_text_playlists_readd, mPlaylistPurchasedCount));
                            mPlaylistsReadd.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (mActivityRef.get() != null && !mActivityRef.get().isFinishing()) {
                                        final AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
                                        alert.setMessage(getString(R.string.alert_playlists_readd_disclaimer));
                                        alert.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                                            sendPlaylistsToWatch(mPlaylistPurchasedCount);
                                            CommonUtils.showSnackbar(mPlaylistsReadd, getString(R.string.success));
                                        });

                                        alert.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss()).show();
                                    }
                                }
                            });
                        } else
                            mPlaylistsReadd.setVisibility(View.INVISIBLE);
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() { }
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
                    if (purchase.getSku().equals(getString(R.string.sub_premium_product_id))) {
                        mPremiumSubUnlocked = true;
                        Utilities.TogglePremiumOnWatch(mActivity, mPremiumSubUnlocked, false);
                        SetPremiumContent();
                        final AlertDialog.Builder alert1 = new AlertDialog.Builder(PremiumActivity.this);
                        alert1.setMessage(getString(R.string.alert_premium_purchased).concat("\n\n").concat(getString(R.string.alert_purchased_rating)));
                        alert1.setPositiveButton(getString(R.string.confirm_yes), (dialog, which) -> {
                            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.google_play_url)));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            dialog.dismiss();
                        });
                        alert1.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss()).show();
                    } else if (purchase.getSku().contains("playlist")) {
                        final AlertDialog.Builder alert2 = new AlertDialog.Builder(PremiumActivity.this);
                        alert2.setMessage(getString(R.string.alert_playlists_purchased).concat("\n\n").concat(getString(R.string.alert_purchased_rating)));
                        alert2.setPositiveButton(getString(R.string.generic_yes), (dialog, which) -> {
                            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.google_play_url)));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            dialog.dismiss();
                        });
                        alert2.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss()).show();
                        sendPlaylistsToWatch(mPlaylistSkus.getSelectedItemPosition());
                    }

                    if (!purchase.isAcknowledged()) {
                        final AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

                        mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult1 -> {
                            if (billingResult1.getResponseCode() == OK) {
                            }
                        });
                    }
                }
            }
        }
    }

    private void showPlaylistPurchaseSub() {
        final AlertDialog.Builder alert2 = new AlertDialog.Builder(PremiumActivity.this);
        alert2.setMessage(getString(R.string.alert_playlists_purchased).concat("\n\n").concat(getString(R.string.alert_purchased_rating)));
        alert2.setPositiveButton(getString(R.string.generic_yes), (dialog, which) -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.google_play_url)));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            dialog.dismiss();
        });
        alert2.setNegativeButton(getString(R.string.confirm_no), (dialog, which) -> dialog.dismiss()).show();

        sendPlaylistsToWatch(mPlaylistSkus.getSelectedItemPosition());
    }

    private void showPlaylistPurchaseInapp() {

        final List<String> skuList = new ArrayList<>();
        skuList.add("playlist_" + mPlaylistSkus.getSelectedItemPosition());

        final SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP);

        mBillingClient.querySkuDetailsAsync(params.build(), (billingResult, skuDetailsList) -> {
            for (final SkuDetails skuDetails : skuDetailsList) {
                if (skuDetails.getSku().contains("playlist")) {
                    final BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetails)
                            .build();

                    final BillingResult result = mBillingClient.launchBillingFlow(mActivity, flowParams);

                    if (result.getResponseCode() == ITEM_ALREADY_OWNED) {
                        CommonUtils.showSnackbar(mPremiumButton, getString(R.string.alert_purchased));
                    } else if (result.getResponseCode() != OK) {
                        CommonUtils.showSnackbar(mPremiumButton, getString(R.string.general_error).concat("\n").concat("(").concat(result.getDebugMessage()).concat(")"));
                    }
                }
            }
        });
    }

    private void SetPremiumContent()
    {
        //boolean isDebuggable = ( 0 != ( getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE ) );
        //mPlaylistsReadd.setVisibility(mPlaylistPurchasedCount  > 0 ? View.VISIBLE : View.INVISIBLE);

        if (mPremiumInappUnlocked || mPremiumSubUnlocked) {
            findViewById(R.id.btn_upload_file).setEnabled(true);
            tvUploadSummary.setText(mActivity.getString(R.string.upload_file_summary_unlocked));
            mPremiumButton.setText(mActivity.getString(R.string.button_text_resync_premium));
            mPremiumButton.setOnClickListener(view -> {
                mProgressFileUpload.get().setVisibility(View.VISIBLE);
                CommonUtils.showSnackbar(mPremiumButton, getString(R.string.button_text_resync_premium_waiting));
                Utilities.TogglePremiumOnWatch(mActivity, true, true);
            });
            mPlaylistSkus.setEnabled(true);
            mPlaylistsButButton.setEnabled(true);
            findViewById(R.id.btn_playlist_premium).setVisibility(View.INVISIBLE);
            findViewById(R.id.premium_trial).setVisibility(View.INVISIBLE);
            findViewById(R.id.premium_benefits_title).setVisibility(View.INVISIBLE);
            findViewById(R.id.premium_benefits_list).setVisibility(View.INVISIBLE);
        }
        else {
            mPlaylistSkus.setEnabled(false);
            mPlaylistsButButton.setEnabled(false);
            findViewById(R.id.btn_playlist_premium).setVisibility(View.VISIBLE);
            findViewById(R.id.btn_upload_file).setEnabled(false);
            tvUploadSummary.setText(mActivity.getString(R.string.upload_file_summary_locked));
            findViewById(R.id.premium_trial).setVisibility(View.VISIBLE);
            findViewById(R.id.premium_benefits_title).setVisibility(View.VISIBLE);
            findViewById(R.id.premium_benefits_list).setVisibility(View.VISIBLE);
        }
    }

    private void sendPlaylistsToWatch(final int count)
    {
        final PutDataMapRequest dataMap = PutDataMapRequest.create("/addplaylists");
        dataMap.getDataMap().putInt("number", count);
        CommonUtils.DeviceSync(mActivity, dataMap);
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
                CommonUtils.showSnackbar(mPlaylistsButButton, getString(R.string.success));
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

                    CommonUtils.executeAsync(new SendFileToWatch(mActivity, uriLocal), (response) -> { });

                } else {

                    final int count = clipData.getItemCount();

                    for (int i = 0; i < count; i++) {
                        final ClipData.Item item = clipData.getItemAt(i);
                        CommonUtils.executeAsync(new SendFileToWatch(mActivity, item.getUri()), (response) -> { });
                    }
                }
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