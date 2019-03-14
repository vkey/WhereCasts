package com.krisdb.wearcastslibrary;

import android.graphics.drawable.Drawable;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class PodcastItem implements Serializable
{
    private String title, description, pubDate, displayDate, displayDuration; //pubdate must be String for inserting into sql
    private URL mediaurl, episodeUrl;
    private int pid, eid, position, duration, newCount, playlistid;
    private boolean read, istitle = false, finished, local, isREST, isSentToWatch, isDownloaded, isSelected;
    private ChannelItem channel;
    private PodcastItem latestEpisode;
    private transient Drawable displayThumb;

    public String getDisplayDate() { return displayDate; }

    public void setDisplayDate(final String date) { this.displayDate = date; }

    public String getDisplayDuration() { return displayDuration; }

    public void setDisplayDuration(final String duration) { this.displayDuration = duration; }

    public int getNewCount() { return newCount; }

    public void setNewCount(final int newCount) { this.newCount = newCount; }

    public Drawable getDisplayThumbnail() { return displayThumb; }

    public void setDisplayThumbnail(final Drawable thumb) { this.displayThumb = thumb; }

    public void setLatestEpisode(final PodcastItem episode)
    {
        this.latestEpisode = episode;
    }

    public PodcastItem getLatestEpisode()
    {
        return latestEpisode;
    }

    public void setChannel(final ChannelItem channel)
    {
        this.channel = channel;
    }

    public ChannelItem getChannel()
    {
        return channel;
    }

    public void setPubDate(final String date)
    {
        this.pubDate = date;
    }

    public String getPubDate()
    {
        return pubDate;
    }

    public void setRead(final Boolean read)
    {
        this.read = read;
    }
    public Boolean getRead() {
        return read;
    }

    public void setIsSelected(final Boolean selected)
    {
        this.isSelected = selected;
    }
    public Boolean getIsSelected() {
        return isSelected;
    }

    public void setIsDownloaded(final Boolean downloaded)
    {
        this.isDownloaded = downloaded;
    }
    public Boolean getIsDownloaded() {
        return isDownloaded;
    }

    public void setIsSenttoWatch(final Boolean sent)
    {
        this.isSentToWatch = sent;
    }
    public Boolean getIsSenttoWatch() {
        return isSentToWatch;
    }

    public void setIsLocal(final Boolean local)
    {
        this.local = local;
    }

    public Boolean getIsLocal() {
        return local;
    }

    public void setFinished(final Boolean finished)
    {
        this.finished = finished;
    }

    public Boolean getFinished() {
        return finished;
    }

    public void setIsREST(final Boolean isrest)
    {
        this.isREST = isrest;
    }

    public Boolean getIsREST() {
        return isREST;
    }

    public Boolean getIsTitle()
    {
        return istitle;
    }

    public void setIsTitle(final Boolean isTitle)
    {
        this.istitle = isTitle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title.trim();
    }

      public URL getEpisodeUrl() {
        return episodeUrl;
    }

    public void setEpisodeUrl(final String url) {
        try
        {
            this.episodeUrl = new URL(url);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
    }

    public URL getMediaUrl() {
        return mediaurl;
    }

    public void setMediaUrl(final String url) {
        try
        {
            this.mediaurl = new URL(url);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
    }
    public int getPodcastId() { return pid; }

    public void setPodcastId(final int id) { this.pid = id; }

    public int getPosition() { return position; }

    public void setPlaylistId(final int id) { this.playlistid = id; }

    public int getPlaylistId() { return playlistid; }

    public void setPosition(final int position) { this.position = position; }

    public int getDuration() { return duration; }

    public void setDuration(final int duration) { this.duration = duration; }

    public int getEpisodeId() { return eid; }

    public void setEpisodeId(final int id) { this.eid = id; }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description.trim();
    }

}