package poplyrics;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.db4o.query.Predicate;

import grocerymarket.DatabaseTask;
import poplyrics.model.Track;

public class LyricFinder extends DatabaseTask {

	final String word = "heart";

	@Override
	protected void performWork() throws Exception {

		final List<Track> matchingTracks = new LinkedList<Track>();
		final Map<Track, Integer> counts = new HashMap<Track, Integer>();

		int totalCount = 0;
		int totalMatchesCount = 0;
		int noLyricsCount = 0;
		for (final Track track : getObjectContainer().query(new Predicate<Track>() {
			private static final long serialVersionUID = -9104565722482206442L;

			@Override
			public boolean match(final Track track) {
				//				return track.getName().equals("Rapper's Delight");
				return true;
				//				return track.getYear() >= minYear && track.getYear() < maxYear;
			}
		})) {
			if (track.getNormalizedLyrics() != null) {
				totalCount++;
				int count = 0;
				final String[] words = track.getNormalizedLyrics().split("\\b");
				for (String _word : words) {
					_word = _word.replaceAll("[\\?\\\"\\,]", "");
					if (_word.trim().equalsIgnoreCase(word)) {
						count++;
						totalMatchesCount++;
					}
				}

				if (count > 0) {
					matchingTracks.add(track);
					counts.put(track, count);
				}
			} else {
				noLyricsCount++;
				System.out.println("*** No lyrics for " + track.getName());
			}
		}

		System.out.println(matchingTracks.size() + " tracks matched word '" + word + "'");

		matchingTracks.sort(new Comparator<Track>() {

			@Override
			public int compare(Track track1, Track track2) {
				return counts.get(track1).compareTo(counts.get(track2));
			}
		});

		for (Track track : matchingTracks) {
			System.out.println(track.getArtist() + " - " + track.getName() + " - " + track.getYear());
			System.out.println("count: " + counts.get(track));
			System.out.println(track.getLyrics());
			System.out.println();
		}
		System.out.println();
		System.out.println("Matching tracks :\t" + matchingTracks.size());
		System.out.println("Total matches :\t" + totalMatchesCount);
		System.out.println("Tracks with lyrics:\t" + totalCount);
		System.out.println("Tracks with no lyrics:\t" + noLyricsCount);
	}

	public static void main(final String[] args) {
		new LyricFinder().run();
	}
}
