package com.krisdb.wearcastslibrary;


import java.util.List;

public class PodcastCategory {
    private List<PodcastItem> podcasts;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name.trim();
    }

    public List<PodcastItem> getPodcasts() {
        return podcasts;
    }

    public void setPodcasts(final List<PodcastItem> podcasts) {
        this.podcasts = podcasts;
    }

}
