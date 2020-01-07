package com.krisdb.wearcasts.Models;

public class FileUploadProgress {
    private boolean started, processing, complete;

    public boolean getComplete() {
        return complete;
    }

    public void setComplete(final boolean complete) {
        this.complete = complete;
    }

    public boolean getStarted() {
        return started;
    }

    public void setStarted(final boolean started) {
        this.started = started;
    }

    public boolean getProcessing() {
        return processing;
    }

    public void setProcessing(final boolean processing) {
        this.processing = processing;
    }
}
