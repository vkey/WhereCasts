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
        final StringBuilder sbCreate = new StringBuilder();
        sbCreate.append("create table [tbl_directory_podcasts] (");
        sbCreate.append("[id] INTEGER primary key AUTOINCREMENT,");
        sbCreate.append("[cid] INTEGER not null,");
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