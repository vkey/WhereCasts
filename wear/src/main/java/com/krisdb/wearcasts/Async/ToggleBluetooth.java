package com.krisdb.wearcasts.Async;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.SystemClock;

import java.util.concurrent.Callable;

public class ToggleBluetooth implements Callable<Boolean> {
    private final Context context;
    private Boolean mDisable;

    public ToggleBluetooth(final Context context, final Boolean disable)
    {
        this.context = context;
        this.mDisable = disable;
    }

    @Override
    public Boolean call() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (mDisable)
            adapter.disable();
        else
            adapter.enable();

        SystemClock.sleep(1300);

        return false;
    }
}