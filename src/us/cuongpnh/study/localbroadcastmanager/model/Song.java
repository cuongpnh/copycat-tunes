package us.cuongpnh.study.localbroadcastmanager.model;

import android.content.ContentUris;
import android.graphics.Bitmap;
import android.net.Uri;

public class Song {
	private long id;
	private String artist;
	private String title;
	private String album;
	private long duration;
	private Bitmap imageDisplay;
	private Uri albumArtUri;
	private long albumId;
	
	public Song(long id, String artist, String title, String album, long duration) {
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.album = album;
        this.duration = duration;
    }
	
	public Song(long id, String artist, String title, String album, long duration, long albumId) {
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.album = album;
        this.duration = duration;
        this.albumId = albumId;
        this.albumArtUri = null;
    }
    public Song(long id, String artist, String title, String album, long duration, long albumId, Uri albumArtUri) {
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.album = album;
        this.duration = duration;
        this.albumId = albumId;
        this.albumArtUri = albumArtUri;
    }

    public long getId() {
        return id;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getAlbum() {
        return album;
    }

    public long getDuration() {
        return duration;
    }

    public Uri getURI() {
        return ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
    }

	public Bitmap getImageDisplay() {
		return imageDisplay;
	}

	public void setImageDisplay(Bitmap displayImage) {
		this.imageDisplay = displayImage;
	}

	public Uri getAlbumArtUri() {
		return albumArtUri;
	}

	public void setAlbumArtUri(Uri albumArtUri) {
		this.albumArtUri = albumArtUri;
	}

	public long getAlbumId() {
		return albumId;
	}
	public void setAlbumId(long albumId) {
		this.albumId = albumId;
	}
}
