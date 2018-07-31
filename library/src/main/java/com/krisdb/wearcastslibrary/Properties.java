package com.krisdb.wearcastslibrary;

import com.google.android.gms.common.api.GoogleApiClient;


public class Properties {
    private static Properties mInstance = null;

    public static GoogleApiClient mGoogleApiClient;

    protected Properties(){}

    public static synchronized Properties getInstance(){
        if(null == mInstance){
            mInstance = new Properties();
        }
        return mInstance;
    }
}
