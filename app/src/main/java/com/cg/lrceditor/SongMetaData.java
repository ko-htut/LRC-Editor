package com.cg.lrceditor;

import java.io.Serializable;

public class SongMetaData implements Serializable {
    private String artistName = "";
    private String albumName = "";
    private String songName = "";
    private String composerName = "";

    public String getSongName() {
        return this.songName;
    }

    public String getArtistName() {
        return this.artistName;
    }

    public String getAlbumName() {
        return this.albumName;
    }

    public String getComposerName() {
        return this.composerName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public void setComposerName(String composerName) {
        this.composerName = composerName;
    }
}
