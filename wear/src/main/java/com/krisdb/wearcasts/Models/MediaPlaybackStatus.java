package com.krisdb.wearcasts.Models;

public class MediaPlaybackStatus {
    private String local_file;
    private int position, error_code, episodeid, playlistid;
    private boolean media_play, media_start, media_paused, media_error, media_completed, media_playlist_skip, media_playing;

    public int getPosition() {
        return position;
    }

    public void setPosition(final int position) {
        this.position = position;
    }

    public boolean getMediaPlay() {
        return media_play;
    }

    public void setMediaPlay(final boolean play) {
        this.media_play = play;
    }

    public boolean getMediaPlaying() {
        return media_playing;
    }

    public void setMediaPlaying(final boolean playing) {
        this.media_playing = playing;
    }

    public boolean getMediaStart() {
        return media_start;
    }

    public void setMediaStart(final boolean start) {
        this.media_start = start;
    }

    public boolean getMediaPaused() {
        return media_paused;
    }

    public void setMediaPaused(final boolean pause) {
        this.media_paused = pause;
    }

    public boolean getMediaError() {
        return media_error;
    }

    public void setMediaError(final boolean error) {
        this.media_error = error;
    }

    public boolean getMediaCompleted() {
        return media_completed;
    }

    public void setMediaCompleted(final boolean completed) {
        this.media_completed = completed;
    }

    public boolean getMediaPlaylistSkip() {
        return media_playlist_skip;
    }

    public void setMediaPlaylistSkip(final boolean skip) {
        this.media_playlist_skip = skip;
    }

    public int getErrorCode() {
        return error_code;
    }

    public void setErrorCode(final int code) {
        this.error_code = code;
    }

    public int getEpisodeId() {
        return episodeid;
    }

    public void setEpisodeId(final int id) {
        this.episodeid = id;
    }

    public int getPlaylistId() {
        return playlistid;
    }

    public void setPlaylistId(final int id) {
        this.playlistid = id;
    }

    public String getLocalFile() {
        return local_file;
    }

    public void setLocalFile(final String file) {
        this.local_file = file;
    }


}
