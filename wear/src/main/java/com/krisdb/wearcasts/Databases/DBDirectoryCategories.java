package com.krisdb.wearcasts.Databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBDirectoryCategories extends SQLiteOpenHelper
{
    public DBDirectoryCategories(final Context context) {
        super(context, "DirectoryCategories", null, 1);
    }

    public void onCreate(final SQLiteDatabase db)
    {
        final StringBuilder sbCreate = new StringBuilder();
        sbCreate.append("create table IF NOT EXISTS [tbl_directory_categories] (");
        sbCreate.append("[id] INTEGER primary key AUTOINCREMENT,");
        sbCreate.append("[name] TEXT not null,");
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

    public long insert(final ContentValues cv)
    {
        long id;
        final SQLiteDatabase db = this.getWritableDatabase();
        id = db.insert("tbl_directory_categories", null, cv);
        db.close();

        return id;
    }

    public void deleteAll()
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.delete("tbl_directory_categories", null, null);
        db.close();
    }

    public void delete(final Integer id)
    {
        final SQLiteDatabase db = this.getWritableDatabase();

        db.delete("tbl_directory_categories","[id] = ?", new String[] { id.toString() });
        db.close();
    }

    public void update(final ContentValues cv, final Integer itemId)
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.update("tbl_directory_categories", cv, "[id] = ?", new String[] { itemId.toString() });
        db.close();
    }

    public void updateAll(final ContentValues cv)
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.update("tbl_directory_categories", cv, null, null);
        db.close();
    }
}