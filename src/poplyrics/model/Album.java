package poplyrics.model;

import arxiv.model.Entity;

public class Album extends Entity {
	private final String name;
	private final Artist artist;

	public Album(final String name, final Artist artist) {
		this.name = name;
		this.artist = artist;
	}

	public String getName() {
		return name;
	}

	public Artist getArtist() {
		return artist;
	}
}
