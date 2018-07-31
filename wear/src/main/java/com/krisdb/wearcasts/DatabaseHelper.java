package com.krisdb.wearcasts;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseHelper
{
    private static DBPodcastsEpisodes dpPodcastEpisodes;
    private static SQLiteDatabase sdbEpisodesRead, sbEpisodesWrite;

    public static synchronized DBPodcastsEpisodes getHelper(Context context)
    {
        if (dpPodcastEpisodes == null)
            dpPodcastEpisodes = new DBPodcastsEpisodes(context);

        return dpPodcastEpisodes;
    }

    public static synchronized SQLiteDatabase select(Context context)
    {
        if (sdbEpisodesRead == null)
            sdbEpisodesRead = getHelper(context).getReadableDatabase();

        return sdbEpisodesRead;
    }

    public static synchronized SQLiteDatabase write(Context context)
    {
        if (sbEpisodesWrite == null)
            sbEpisodesWrite = getHelper(context).getWritableDatabase();

        return sbEpisodesWrite;
    }

}