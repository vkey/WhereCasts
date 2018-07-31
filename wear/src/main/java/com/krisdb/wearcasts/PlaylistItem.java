package com.krisdb.wearcasts;

public class PlaylistItem
{
    private String name;
    private int id, episodeID;

    public String getName() { return name; }

    public void setName(final String name) { this.name = name; }

    public int getID() { return id; }

    public void setID(final int id) { this.id = id; }

    public int getEpisdeID() { return episodeID; }

    public void setEpisodeID(final int episodeID) { this.id = episodeID; }

    @Override
    public String toString() {
        return getName();
    }
}