package com.krisdb.wearcasts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

public class DBPodcastsEpisodes extends SQLiteOpenHelper
{
    private String mPlayistTable = "tbl_playlists";
    private String mEpisodesTable = "tbl_podcast_episodes";
    private String mPlayistTableRef = "tbl_playlists_xref";
    private String mPodcastsTable = "tbl_podcasts";
    private static DBPodcastsEpisodes mInstance = null;
    private Context mContext;

    public static DBPodcastsEpisodes getInstance(Context ctx) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new DBPodcastsEpisodes(ctx.getApplicationContext());
        }

        return mInstance;
    }


    public DBPodcastsEpisodes(final Context context) {
        super(context, "Episodes", null, 5);
        mContext = context;
    }

    public void onCreate(final SQLiteDatabase db)
    {
        final String sbCreate = "create table IF NOT EXISTS ["+mEpisodesTable+"] (" +
                "[id] INTEGER primary key AUTOINCREMENT," +
                "[pid] INTEGER not null," +
                "[title] TEXT not null," +
                "[url] TEXT null," + //episode url
                "[description] TEXT null," +
                "[mediaurl] TEXT null," +
                "[downloadurl] TEXT null," +
                "[duration] INTEGER null," +
                "[position] INTEGER not null DEFAULT 0," +
                "[downloadid] INTEGER not null DEFAULT 0," +
                "[download] INTEGER not null DEFAULT 0," +
                "[dateDownload] DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "[finished] INTEGER not null DEFAULT 0," +
                "[buffering] INTEGER not null DEFAULT 0," +
                "[playing] INTEGER not null DEFAULT 0," +
                "[read] INTEGER not null DEFAULT 0," +
                "[new] INTEGER not null DEFAULT 1," +
                "[deleted] INTEGER not null DEFAULT 0," +
                "[upnext] INTEGER not null DEFAULT 0," +
                "[pubDate] DATETIME null," +
                "[dateAdded] DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ");";

        db.execSQL(sbCreate);

        final String table_playlists = "create table [" + mPlayistTable + "] (" +
                "[playlist_id] INTEGER primary key AUTOINCREMENT," +
                "[name] TEXT not null," +
                "[dateCreated] DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ");";
        db.execSQL(table_playlists);

        final String table_playlists_xref = "create table [" + mPlayistTableRef + "] (" +
                "[id] INTEGER primary key AUTOINCREMENT," +
                "[episode_id] INTEGER not null," +
                "[playlist_id] INTEGER not null" +
                ");";
        db.execSQL(table_playlists_xref);

        final StringBuilder sbCreatePodcastsTable = new StringBuilder();
        sbCreatePodcastsTable.append("create table IF NOT EXISTS [" + mPodcastsTable + "] (");
        sbCreatePodcastsTable.append("[id] INTEGER primary key AUTOINCREMENT,");
        sbCreatePodcastsTable.append("[title] TEXT not null,");
        sbCreatePodcastsTable.append("[url] TEXT not null,"); //rss url
        sbCreatePodcastsTable.append("[site_url] TEXT null,");
        sbCreatePodcastsTable.append("[thumbnail_url] TEXT null,");
        sbCreatePodcastsTable.append("[thumbnail_name] TEXT null,");
        sbCreatePodcastsTable.append("[description] TEXT null,");
        sbCreatePodcastsTable.append("[dateAdded] DATETIME DEFAULT CURRENT_TIMESTAMP");
        sbCreatePodcastsTable.append(");");

        db.execSQL(sbCreatePodcastsTable.toString());
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {

        final StringBuilder sbCreate = new StringBuilder();
        sbCreate.append("create table IF NOT EXISTS ["+mPodcastsTable+"] (");
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

        final DBPodcasts dbPodcasts = new DBPodcasts(mContext);
        final SQLiteDatabase sdb = dbPodcasts.select();

        final Cursor cursor = sdb.rawQuery("SELECT id,title,url,site_url,thumbnail_url,thumbnail_name,description,dateAdded  FROM ["+mPodcastsTable+"]", null);

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {

                final ContentValues cv1 = new ContentValues();
                cv1.put("title", cursor.getString(1));
                cv1.put("url", cursor.getString(2));
                cv1.put("site_url", cursor.getString(3));
                cv1.put("thumbnail_url", cursor.getString(4));
                cv1.put("thumbnail_name", cursor.getString(5));
                cv1.put("dateAdded", DateUtils.GetDate());

                final long id = db.insert(mPodcastsTable, null, cv1);

                final ContentValues cv2 = new ContentValues();
                cv2.put("pid", id);

                db.update(mEpisodesTable, cv2, "[id] = ?", new String[] { String.valueOf(cursor.getInt(0)) });

                cursor.moveToNext();
            }
        }

        cursor.close();
        sdb.close();
        dbPodcasts.close();
    }


    public SQLiteDatabase select()
    {
        return this.getReadableDatabase();
    }

    public SQLiteDatabase insert()
    {
        return this.getWritableDatabase();
    }

    public void insertAll(final List<PodcastItem> episodes, final Boolean assignPlaylist, final Boolean download) {

        final SQLiteDatabase db = this.getWritableDatabase();
        final SQLiteStatement statement = db.compileStatement("INSERT INTO ["+mEpisodesTable+"] ([pid], [title], [description], [mediaurl], [url], [dateAdded], [pubDate], [duration]) VALUES (?,?,?,?,?,?,?,?)");

        db.beginTransaction();

        try {
            for (PodcastItem episode : episodes) {
                statement.bindLong(1, episode.getPodcastId());
                statement.bindString(2, episode.getTitle() != null ? episode.getTitle() : "");
                statement.bindString(3, episode.getDescription());
                if (episode.getMediaUrl() != null)
                    statement.bindString(4, episode.getMediaUrl().toString());
                if (episode.getEpisodeUrl() != null)
                    statement.bindString(5, episode.getEpisodeUrl().toString());
                statement.bindString(6, DateUtils.GetDate());
                statement.bindString(7, episode.getPubDate());
                statement.bindLong(8, episode.getDuration());

                int epidodeId = (int)statement.executeInsert();

            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    public void addToPlaylist(final int playlistId, final List<PodcastItem> episodes) {

        final SQLiteDatabase db = this.getWritableDatabase();
        final SQLiteStatement statement = db.compileStatement("INSERT INTO ".concat(mPlayistTableRef).concat(" ([episode_id], [playlist_id]) VALUES (?,?)"));
        db.beginTransaction();

        try {
            for (final PodcastItem episode : episodes) {
                statement.bindLong(1, episode.getEpisodeId());
                statement.bindLong(2, playlistId);
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
        id = db.insert(mEpisodesTable, null, cv);
        db.close();

        return id;
    }

    public long insertPodcast(final ContentValues cv)
    {
        long id;
        final SQLiteDatabase db = this.getWritableDatabase();
        id = db.insert(mPodcastsTable, null, cv);
        db.close();

        return id;
    }

    public void deletePodcast(final Integer id)
    {
        final SQLiteDatabase db = this.getWritableDatabase();

        db.delete(mPodcastsTable,"[id] = ?", new String[] { id.toString() });
        db.close();
    }

    public void updatePodcast(final ContentValues cv, final Integer itemId)
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.update(mPodcastsTable, cv, "[id] = ?", new String[] { itemId.toString() });
        db.close();
    }

    public long insertPlaylist(final String name)
    {
        final ContentValues cv = new ContentValues();
        cv.put("name", name);

        long id;
        final SQLiteDatabase db = this.getWritableDatabase();
        id = db.insert(mPlayistTable, null, cv);
        db.close();

        return id;
    }

    public void addEpisodeToPlaylist(final Integer playlistId, final int episodeId)
    {
        final ContentValues cv = new ContentValues();
        cv.put("playlist_id", playlistId);
        cv.put("episode_id", episodeId);

        final SQLiteDatabase db = this.getWritableDatabase();
        db.insert(mPlayistTableRef, null, cv);
        db.close();
    }

    public void deleteEpisodeFromPlaylist(final Integer playlistId, final Integer episodeId)
    {
        final SQLiteDatabase db = this.getWritableDatabase();

        db.delete(mPlayistTableRef,"[playlist_id] = ? AND [episode_id] = ?", new String[] { playlistId.toString(), episodeId.toString() });
        db.close();
    }

    public void deleteEpisodeFromPlaylists(final Integer episodeId)
    {
        final SQLiteDatabase db = this.getWritableDatabase();

        db.delete(mPlayistTableRef,"[episode_id] = ?", new String[] { episodeId.toString() });
        db.close();
    }

    public void deletePlaylist(final Integer playlistId)
    {
        final SQLiteDatabase db = this.getWritableDatabase();

        db.delete(mPlayistTable,"[playlist_id] = ?", new String[] { playlistId.toString() });
        db.delete(mPlayistTableRef,"[playlist_id] = ?", new String[] { playlistId.toString() });
        db.close();
    }

    public void unsubscribe(final Context ctx, final Integer podcastId)
    {
        final List<PodcastItem> episodes = DBUtilities.GetEpisodes(ctx, podcastId);

        if (episodes.size() == 1) return;

        final SQLiteDatabase db = this.getWritableDatabase();

        final StringBuilder sbSql = new StringBuilder();
        sbSql.append("DELETE FROM ");
        sbSql.append(mPlayistTableRef);
        sbSql.append(" WHERE ");

        int count = 0;
        final int size = episodes.size() - 2;

        for (final PodcastItem episode : episodes)
        {
            if (episode.getEpisodeId() == 0) continue;
            sbSql.append("[episode_id] = ");
            sbSql.append(episode.getEpisodeId());
            if (count++ < size)
                sbSql.append(" OR ");
        }

        db.execSQL(sbSql.toString());
        db.delete(mEpisodesTable,"[pid] = ?", new String[] { podcastId.toString() });
        db.close();
    }

    public void deleteAllPlaylists()
    {
        final SQLiteDatabase db = this.getWritableDatabase();

        db.delete(mPlayistTable,null, null);
        db.delete(mPlayistTableRef,null, null);
        db.close();
    }

    public void updatePlaylist(final String name, final Integer id)
    {
        final ContentValues cv = new ContentValues();
        cv.put("name", name);

        final SQLiteDatabase db = this.getWritableDatabase();
        db.update(mPlayistTable, cv, "[playlist_id] = ?", new String[] { id.toString() });
        db.close();
    }

    public void deleteAll()
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.delete(mEpisodesTable, null, null);
        db.close();
    }

    public void delete(final Integer id)
    {
        final SQLiteDatabase db = this.getWritableDatabase();

        db.delete(mEpisodesTable,"[id] = ?", new String[] { id.toString() });
        db.close();
    }

    public void update(final ContentValues cv, final Integer id)
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.update(mEpisodesTable, cv, "[id] = ?", new String[] { id.toString() });
        db.close();
    }

    public void updateAll(final ContentValues cv, final Integer podcastId)
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.update(mEpisodesTable, cv, "[pid] = ?", new String[] { podcastId.toString() });
        db.close();
    }

    public void updateAll(final ContentValues cv)
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.update(mEpisodesTable, cv, null, null);
        db.close();
    }
}
