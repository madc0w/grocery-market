package poplyrics;

import grocerymarket.DatabaseTask;
import poplyrics.model.Artist;
import poplyrics.model.Track;

import com.db4o.ObjectSet;
import com.db4o.query.Predicate;

public abstract class AbstractTrackExtractor extends DatabaseTask {

	protected Artist getArtist(final String artistName) {
		final ObjectSet<Artist> result = getObjectContainer().query(new Predicate<Artist>() {
			@Override
			public boolean match(final Artist artist) {
				return artist.getName().equals(artistName);
			}
		});
		Artist artist;
		if (result.isEmpty()) {
			artist = new Artist(artistName);
			getObjectContainer().store(artist);
		} else {
			artist = result.iterator().next();
		}
		return artist;
	}

	protected Track getTrack(final String artist, final String trackName) {
		final ObjectSet<Track> result = getObjectContainer().query(new Predicate<Track>() {
			@Override
			public boolean match(final Track track) {
				return track.getArtist().getName().equals(artist) && track.getName().equals(trackName);
			}
		});
		if (result.size() > 1) {
			logger.error("Found " + result.size() + " entries for artist '" + artist + "' and track name '" + trackName + "'.");
		}
		return result.isEmpty() ? null : result.iterator().next();
	}

	public static void main(final String[] args) {
		new Top40ChartsTrackTitleExtractor().run();
	}

}
