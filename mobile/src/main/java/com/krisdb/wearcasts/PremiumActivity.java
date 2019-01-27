package com.krisdb.wearcasts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcastslibrary.AsyncTasks;
import com.krisdb.wearcastslibrary.CommonUtils;
import com.krisdb.wearcastslibrary.Interfaces;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class PremiumActivity extends AppCompatActivity {

    private Activity mActivity;
    private static final int UPLOAD_REQUEST_CODE = 43;
    public static final int PREMIUM_REQUEST_CODE = 100;
    public static final int PLAYLIST_REQUEST_CODE = 101;
    private TextView tvUploadSummary, mPremiumInstructionsText;
    private static WeakReference<ProgressBar> mProgressFileUpload;
    private IInAppBillingService mService;
    private Boolean mWatchConnected = false, mPremiumUnlocked = false;
    private LocalBroadcastManager mBroadcastManger;
    private Button mPremiumButton, mPlaylistsReadd;
    private Spinner mPlaylistSkus;
    private int mPlaylistPurchasedCount = 0;
    private WeakReference<PremiumActivity> mActivityRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);
        setTitle("Premium"); //TODO: Localize
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mActivity = this;

        mActivityRef = new WeakReference<>(this);

        mProgressFileUpload = new WeakReference<>((ProgressBar)findViewById(R.id.upload_file_progress));
        mPlaylistsReadd = findViewById(R.id.btn_playlists_readd);
        mPremiumInstructionsText = findViewById(R.id.premium_instructions);
        mPlaylistSkus = findViewById(R.id.playlist_buy_qty);
        mBroadcastManger = LocalBroadcastManager.getInstance(mActivity);
        tvUploadSummary = findViewById(R.id.upload_file_summary);
        mPremiumButton = findViewById(R.id.btn_unlock_premium);

        findViewById(R.id.btn_playlist_buy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlaylistSkus.getSelectedItemPosition() == 0)
                    CommonUtils.showToast(mActivity, getString(R.string.alert_playlists_quantity_none));
                else if (mPlaylistPurchasedCount > 0)
                {
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
                }
                else
                    showPlaylistPurchase();
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

        mPremiumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {

                    final Bundle bundle = mService.getBuyIntent(
                            mActivity.getResources().getInteger(com.krisdb.wearcastslibrary.R.integer.billing_apk_version),
                            getPackageName(),
                            mActivity.getString(com.krisdb.wearcastslibrary.R.string.inapp_premium_product_id),
                            "inapp",
                            null
                    );

                    final PendingIntent pendingIntent = bundle.getParcelable("BUY_INTENT");

                    if (pendingIntent != null)
                        startIntentSenderForResult(pendingIntent.getIntentSender(), PREMIUM_REQUEST_CODE, new Intent(), 0, 0, 0, null);
                    else
                        CommonUtils.showToast(mActivity, getString(R.string.alert_purchased));


                } catch (Exception e) {
                    e.printStackTrace();
                }
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

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    private void showPlaylistPurchase()
    {
        try {

            final Bundle bundle = mService.getBuyIntent(
                    mActivity.getResources().getInteger(com.krisdb.wearcastslibrary.R.integer.billing_apk_version),
                    mActivity.getPackageName(),
                    //mActivity.getString(com.krisdb.wearcastslibrary.R.string.inapp_premium_product_id),
                    "playlist_" + mPlaylistSkus.getSelectedItemPosition(),
                    "inapp",
                    null
            );

            final PendingIntent pendingIntent = bundle.getParcelable("BUY_INTENT");

            if (pendingIntent != null)
                startIntentSenderForResult(pendingIntent.getIntentSender(), PLAYLIST_REQUEST_CODE, new Intent(), 0, 0, 0, null);
            else
                CommonUtils.showToast(mActivity, getString(R.string.alert_purchased));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);

                        /*
            //removes purchase for testing
             try {
                int response = mService.consumePurchase(3, mActivity.getPackageName(), "inapp:" + mActivity.getPackageName() + ":android.test.purchased");
            } catch (android.os.RemoteException e) {
                e.printStackTrace();
            }
            */

            new AsyncTasks.HasUnlockedPremium(mActivity, mService,
                    new Interfaces.PremiumResponse() {
                        @Override
                        public void processFinish(final Boolean purchased, int playlistCount) {
                            mPremiumUnlocked = purchased;
                            mPlaylistPurchasedCount = playlistCount;
                            SetPremiumContent();
                            if (mWatchConnected)
                                Utilities.TogglePremiumOnWatch(mActivity, purchased);

                            if (mPlaylistPurchasedCount > 0) {
                                mPlaylistsReadd.setVisibility(View.VISIBLE);
                                mPlaylistsReadd.setText(getString(R.string.button_text_playlists_readd, playlistCount));
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
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    };

    private void SetPremiumContent()
    {
        boolean isDebuggable = ( 0 != ( getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE ) );

        if (isDebuggable)
            mPlaylistPurchasedCount = 5;

        mPlaylistsReadd.setVisibility(mPlaylistPurchasedCount  > 0 ? View.VISIBLE : View.INVISIBLE);

        if (isDebuggable || mPremiumUnlocked || mPlaylistPurchasedCount > 0) {
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
        if (resultCode == RESULT_OK)
        {
            if (requestCode == UPLOAD_REQUEST_CODE) {
                final ClipData clipData = resultData.getClipData();

                if (clipData == null)
                {
                    final Uri uriLocal = resultData.getData();
                    new SendFileToWatch(mActivity, uriLocal).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                else {

                    final int count = clipData.getItemCount();

                    for (int i = 0; i < count; i++) {
                        final ClipData.Item item = clipData.getItemAt(i);
                        new SendFileToWatch(mActivity, item.getUri()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            }
            else if (requestCode == PLAYLIST_REQUEST_CODE) {
                sendPlaylistsToWatch(mPlaylistSkus.getSelectedItemPosition());
            }
            else if (requestCode == PREMIUM_REQUEST_CODE) {

                final int responseCode = resultData.getIntExtra("RESPONSE_CODE", 0);
                final String purchaseData = resultData.getStringExtra("INAPP_PURCHASE_DATA");
                //String dataSignature = resultData.getStringExtra("INAPP_DATA_SIGNATURE");

                try {

                    final JSONObject jo = new JSONObject(purchaseData);

                    final String sku = jo.getString("productId");

                    mPremiumUnlocked = Objects.equals(sku, getString(R.string.inapp_premium_product_id));

                    SetPremiumContent();

                    Utilities.TogglePremiumOnWatch(mActivity, mPremiumUnlocked, true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        //else
            //CommonUtils.showToast(mActivity, mActivity.getString(R.string.general_error));
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
        if (mService != null) {

            try {
                unbindService(mServiceConn);
            }
            catch (IllegalArgumentException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onPause() {
        mBroadcastManger.unregisterReceiver(mFileUploadReceiver);
        mBroadcastManger.unregisterReceiver(mWatchResponse);
        super.onPause();
    }
}