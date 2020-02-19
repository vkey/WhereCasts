package com.krisdb.wearcastslibrary.Async;

import com.krisdb.wearcastslibrary.CommonUtils;

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
