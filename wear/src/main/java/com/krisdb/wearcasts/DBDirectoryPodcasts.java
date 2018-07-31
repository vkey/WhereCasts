package com.krisdb.wearcasts;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBDirectoryPodcasts extends SQLiteOpenHelper
{
    public DBDirectoryPodcasts(final Context context) {
        super(context, "DirectoryPodcasts", null, 1);
    }

    public void onCreate(final SQLiteDatabase db)
    {
        String sbCreate = "create table IF NOT EXISTS [tbl_directory_podcasts] (" +
                "[id] INTEGER primary key AUTOINCREMENT," +
                "[cid] INTEGER not null," +
                "[title] TEXT not null," +
                "[url] TEXT not null," + //rss url
                "[site_url] TEXT null," +
                "[thumbnail_url] TEXT null," +
                "[thumbnail_name] TEXT null," +
                "[description] TEXT null," +
                "[dateAdded] DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ");";

        db.execSQL(sbCreate);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        //db.execSQL("ALTER TABLE [tbl_podcasts] ADD COLUMN [site_url] TEXT null");
    }

    public SQLiteDatabase select()
    {
        return this.getReadableDatabase();
    }

    public void insert(final ContentValues cv)
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.insert("tbl_directory_podcasts", null, cv);
        db.close();
    }

    public void deleteAll()
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.delete("tbl_directory_podcasts", null, null);
        db.close();
    }

    public void delete(final Integer id)
    {
        final SQLiteDatabase db = this.getWritableDatabase();

        db.delete("tbl_directory_podcasts","[id] = ?", new String[] { id.toString() });
        db.close();
    }

    public void update(final ContentValues cv, final Integer itemId)
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.update("tbl_directory_podcasts", cv, "[id] = ?", new String[] { itemId.toString() });
        db.close();
    }

    public void updateAll(final ContentValues cv)
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.update("tbl_directory_podcasts", cv, null, null);
        db.close();
    }
}