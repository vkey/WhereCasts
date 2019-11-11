package com.krisdb.wearcastslibrary.Async;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class GetConnectedNodes implements Callable<List<Node>> {
    private final Context context;

    public GetConnectedNodes(final Context context) {
        this.context = context;
    }

    @Override
    public List<Node> call() {

        final Task<List<Node>> nodeListTask = Wearable.getNodeClient(context).getConnectedNodes();
        try {
            return Tasks.await(nodeListTask);

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }
}