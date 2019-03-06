package com.krisdb.wearcasts.Models;

public class NavItem {
    private String title, icon;
    private int id;

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title.trim();
    }

    public int getID() {
        return id;
    }

    public void setID(final int id) {
        this.id = id;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(final String icon) {
        this.icon = icon;
    }
}
