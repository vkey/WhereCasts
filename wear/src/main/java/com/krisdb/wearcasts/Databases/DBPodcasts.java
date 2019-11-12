package com.krisdb.wearcasts.Databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

public class DBPodcasts extends SQLiteOpenHelper
{
    public DBPodcasts(final Context context) {
        super(context, "Podcasts", null, 1);
    }

    public void onCreate(final SQLiteDatabase db)
    {
        final StringBuilder sbCreate = new StringBuilder();
        sbCreate.append("create table [tbl_podcasts] (");
        sbCreate.append("[id] INTEGER primary key AUTOINCREMENT,");
        sbCreate.append("[title] TEXT not null,");
        sbCreate.append("[url] TEXT not null,"); //rss url
        sbCreate.append("[site_url] TEXT null,");
        sbCreate.append("[thumbnail_url] TEXT null,");
        sbCreate.append("[thumbnail_name] TEXT null,");
        sbCreate.append("[description] TEXT null,");
        sbCreate.append("[dateAdded] DATETIME DEFAULT CURRENT_TIMESTAMP");
        sbCreate.append(");");

        db.execSQL(sbCreate.toString());
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {}

    public SQLiteDatabase select()
    {
        return this.getReadableDatabase();
    }

    public void insert(final List<PodcastItem> podcasts) {

        final SQLiteDatabase db = this.getWritableDatabase();
        final SQLiteStatement statement = db.compileStatement("INSERT INTO [tbl_podcasts] ([title], [url], [site_url], [thumbnail_url]) VALUES (?,?,?,?)");

        db.beginTransaction();

        try {
            for (PodcastItem podcast : podcasts) {
                statement.bindString(1, podcast.getTitle());
                statement.bindString(2, podcast.getChannel().getRSSUrl().toString());
                statement.bindString(3, podcast.getChannel().getSiteUrl().toString());
                statement.bindString(4, podcast.getChannel().getThumbnailUrl() != null ? podcast.getChannel().getThumbnailUrl().toString() : null);
                statement.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    public long insert(final ContentValues cv)
    {
        long id;
        final SQLiteDatabase db = this.getWritableDatabase();
        id = db.insert("tbl_podcasts", null, cv);
        db.close();

        return id;
    }

    public void deleteAll()
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.delete("tbl_podcasts", null, null);
        db.close();
    }

    public void delete(final Integer id)
    {
        final SQLiteDatabase db = this.getWritableDatabase();

        db.delete("tbl_podcasts","[id] = ?", new String[] { id.toString() });
        db.close();
    }

    public void update(final ContentValues cv, final Integer itemId)
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.update("tbl_podcasts", cv, "[id] = ?", new String[] { itemId.toString() });
        db.close();
    }

    public void updateAll(final ContentValues cv)
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.update("tbl_podcasts", cv, null, null);
        db.close();
    }
}