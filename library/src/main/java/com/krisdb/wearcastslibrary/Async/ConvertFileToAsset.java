package com.krisdb.wearcastslibrary.Async;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ConvertFileToAsset implements Callable<DataClient.GetFdForAssetResponse> {
    private final Context context;
    private Asset mAsset;

    public ConvertFileToAsset(final Context context, final Asset asset) {
        this.context = context;
        this.mAsset = asset;
    }

    @Override
    public DataClient.GetFdForAssetResponse call() {
        DataClient.GetFdForAssetResponse response = null;

        final Task<DataClient.GetFdForAssetResponse> getFdForAssetResponseTask = Wearable.getDataClient(context).getFdForAsset(mAsset);
        try {
            response = Tasks.await(getFdForAssetResponseTask);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        return response;
    }
}

