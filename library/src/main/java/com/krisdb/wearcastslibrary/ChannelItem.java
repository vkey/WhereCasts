package com.krisdb.wearcastslibrary;


import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class ChannelItem implements Serializable {
    private String title, description, thumbnailname, displayTitle;
    private URL siteurl, rssurl, thumbnail;

    public String getThumbnailName() {
        return thumbnailname;
    }

    public void setThumbnailName(final String thumbnailname) {
        this.thumbnailname = thumbnailname.trim();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title.trim();
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    public void setDisplayTitle(final String title) {
        this.displayTitle = title.trim();
    }

    public URL getSiteUrl() {
        return siteurl;
    }

    public void setSiteUrl(final String url) {
        try
        {
            this.siteurl = new URL(url);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
    }

    public URL getRSSUrl() {
        return rssurl;
    }

    public void setRSSUrl(final String url) {
        try
        {
            this.rssurl = new URL(url);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
    }

    public URL getThumbnailUrl() {
        return thumbnail;
    }

    public void setThumbnailUrl(final String url) {
        try
        {
            this.thumbnail = new URL(url);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description.trim();
    }
}
