package com.krisdb.wearcasts;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.krisdb.wearcastslibrary.DateUtils;
import com.krisdb.wearcastslibrary.PodcastItem;

import java.util.List;

public class DBPodcastsEpisodes extends SQLiteOpenHelper
{
    private String mPlayistTable = "tbl_playlists";
    private String mPlayistTableRef = "tbl_playlists_xref";
    private static DBPodcastsEpisodes mInstance = null;

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
        super(context, "Episodes", null, 4);
    }

    public void onCreate(final SQLiteDatabase db)
    {
        final String sbCreate = "create table IF NOT EXISTS [tbl_podcast_episodes] (" +
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
        newStuff(db);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        newStuff(db);
    }

    private void newStuff(final SQLiteDatabase db)
    {
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
    }

    public SQLiteDatabase select()
    {
        return this.getReadableDatabase();
    }
    public SQLiteDatabase insert()
    {
        return this.getWritableDatabase();
    }

    public void insertAll(final List<PodcastItem> episodes) {

        final SQLiteDatabase db = this.getWritableDatabase();
        final SQLiteStatement statement = db.compileStatement("INSERT INTO [tbl_podcast_episodes] ([pid], [title], [description], [mediaurl], [url], [dateAdded], [pubDate]) VALUES (?,?,?,?,?,?,?)");

        db.beginTransaction();

        try {
            for (PodcastItem episode : episodes) {
                statement.bindLong(1, episode.getPodcastId());
                statement.bindString(2, episode.getTitle());
                statement.bindString(3, episode.getDescription());
                if (episode.getMediaUrl() != null)
                    statement.bindString(4, episode.getMediaUrl().toString());
                if (episode.getChannel().getRSSUrl() != null)
                    statement.bindString(5, episode.getChannel().getRSSUrl().toString());
                statement.bindString(6, DateUtils.GetDate());
                statement.bindString(7, episode.getPubDate().toString());

                long episodeId = statement.executeInsert();

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
        id = db.insert("tbl_podcast_episodes", null, cv);
        db.close();

        return id;
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
        db.delete("tbl_podcast_episodes","[pid] = ?", new String[] { podcastId.toString() });
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
        db.delete("tbl_podcast_episodes", null, null);
        db.close();
    }

    public void delete(final Integer id)
    {
        final SQLiteDatabase db = this.getWritableDatabase();

        db.delete("tbl_podcast_episodes","[id] = ?", new String[] { id.toString() });
        db.close();
    }

    public void update(final ContentValues cv, final Integer id)
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.update("tbl_podcast_episodes", cv, "[id] = ?", new String[] { id.toString() });
        db.close();
    }

    public void updateAll(final ContentValues cv, final Integer podcastId)
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.update("tbl_podcast_episodes", cv, "[pid] = ?", new String[] { podcastId.toString() });
        db.close();
    }

    public void updateAll(final ContentValues cv)
    {
        final SQLiteDatabase db = this.getWritableDatabase();
        db.update("tbl_podcast_episodes", cv, null, null);
        db.close();
    }
}
