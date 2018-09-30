package poplyrics.model;

import arxiv.model.Entity;

public class Track extends Entity {

	private final String name;
	private final int year;
	private final Artist artist;
	private Album album;
	private String lyrics;
	private String normalizedLyrics;
	private int rank;
	private String source;

	public Track(final String name, final int year, final Artist artist) {
		this.name = name;
		this.year = year;
		this.artist = artist;
	}

	public String getName() {
		return name;
	}

	public int getYear() {
		return year;
	}

	public Album getAlbum() {
		return album;
	}

	public void setAlbum(final Album album) {
		this.album = album;
	}

	public String getLyrics() {
		return lyrics;
	}

	public void setLyrics(final String lyrics) {
		this.lyrics = lyrics;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(final int rank) {
		this.rank = rank;
	}

	public Artist getArtist() {
		return artist;
	}

	@Override
	public String toString() {
		return (artist == null ? "" : artist + " - ") + getName();
	}

	public String getNormalizedLyrics() {
		return normalizedLyrics;
	}

	public void setNormalizedLyrics(final String normalizedLyrics) {
		this.normalizedLyrics = normalizedLyrics;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}
