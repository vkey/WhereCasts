package com.krisdb.wearcastslibrary.Async;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class NodesConnected implements Callable<Boolean> {
    private final Context context;

    public NodesConnected(final Context context) {
        this.context = context;
    }

    @Override
    public Boolean call() {
        final Task<List<Node>> nodeListTask = Wearable.getNodeClient(context).getConnectedNodes();

        try {
            List<Node> nodes = Tasks.await(nodeListTask);
            return nodes.size() > 0;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
}