package poplyrics;

import grocerymarket.DatabaseTask;
import poplyrics.model.Track;

import com.db4o.ObjectSet;

public class LyricsNormalizer extends DatabaseTask {

	LyricsNormalizer() {
	}

	@Override
	protected void performWork() throws Exception {
		final ObjectSet<Track> tracks = getObjectContainer().query(Track.class);
		for (final Track track : tracks) {
			normalizeLyrics(track);
			getObjectContainer().store(track);
			getObjectContainer().commit();
		}
	}

	public static void normalizeLyrics(final Track track) {
		String normalizedLyrics = track.getLyrics().toLowerCase();
		normalizedLyrics = normalizedLyrics.replaceAll("[,;./\\(\\):]", "");
		normalizedLyrics = normalizedLyrics.replaceAll("\\s+", " ");
		normalizedLyrics = normalizedLyrics.replaceAll("&", "and");
		track.setNormalizedLyrics(normalizedLyrics);
	}

	public static void main(final String[] args) {
		new LyricsNormalizer().run();
	}
}
