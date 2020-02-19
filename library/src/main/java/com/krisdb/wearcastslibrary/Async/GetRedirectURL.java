package com.krisdb.wearcastslibrary.Async;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.krisdb.wearcastslibrary.CommonUtils;

import java.io.File;
import java.net.URL;
import java.util.concurrent.Callable;

public class GetRedirectURL implements Callable<URL> {
    private static URL mURL;

    public GetRedirectURL(final URL url)
    {
        mURL = url;
    }

    @Override
    public URL call() {
        return CommonUtils.getRedirectUrl(mURL.toString());
    }
}
