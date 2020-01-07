package com.krisdb.wearcasts.Models;

public class OPMLImport {
    private boolean complete, podcasts, episodes, art;
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public boolean getComplete() {
        return complete;
    }

    public void setComplete(final boolean complete) {
        this.complete = complete;
    }

    public boolean getPodcasts() {
        return podcasts;
    }

    public void setPodcasts(final boolean complete) {
        this.podcasts = complete;
    }

    public boolean getEpisodes() {
        return episodes;
    }

    public void setEpisodes(final boolean complete) {
        this.episodes = complete;
    }

    public boolean getArt() {
        return art;
    }

    public void setArt(final boolean complete) {
        this.art = complete;
    }



}
