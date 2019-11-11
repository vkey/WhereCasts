package com.krisdb.wearcastslibrary.Async;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcastslibrary.CommonUtils;

import java.io.File;
import java.util.concurrent.Callable;

public class SendFileToWatch implements Callable<Boolean> {
    private final Context context;
    private static Uri mUri;

    public SendFileToWatch(final Context context, final Uri uri)
    {
        this.context = context;
        this.mUri = uri;
    }

    @Override
    public Boolean call() {
        final PutDataMapRequest dataMapUpload = PutDataMapRequest.create("/uploadfile");

        final Asset asset = Asset.createFromUri(mUri);
        dataMapUpload.getDataMap().putAsset("local_file", asset);
        String localFileName = null;

        if (mUri.toString().startsWith("content://")) {
            try (Cursor cursor = context.getContentResolver().query(mUri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    localFileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        } else if (mUri.toString().startsWith("file://")) {
            final File myFile = new File(mUri.toString());
            localFileName = myFile.getName();
        }

        dataMapUpload.getDataMap().putString("local_filename", localFileName);

        CommonUtils.DeviceSync(context, dataMapUpload);

        return false;
    }
}
