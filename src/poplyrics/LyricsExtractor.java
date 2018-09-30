package poplyrics;

import grocerymarket.DatabaseTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import poplyrics.model.Track;

import com.db4o.query.Predicate;

public class LyricsExtractor extends DatabaseTask {

	final int minYear = 0;

	LyricsExtractor() {
	}

	@Override
	protected void performWork() throws Exception {
		final byte[] inBytes = new byte[10000];
		System.in.read(inBytes);
		String in = new String(inBytes);
		in = in.substring(0, in.indexOf(0)).toLowerCase();
		in = in.replaceAll("[,;./]", "");
		in = in.replaceAll("\\s+", " ");
		in += " .";

		//		final String in = "this is just a test of .";

		final List<String> words = new ArrayList<String>();
		final String[] splitWords = in.split("\\s");
		Collection<Track> prevTracks = new HashSet<Track>();
		boolean isFinished = false;
		for (int i = 0; i < splitWords.length; i++) {
			final String word = splitWords[i];
			words.add(word);
			//			logger.info("seeking " + words);
			final Collection<Track> tracks = findTracks(words, minYear);
			if (tracks.isEmpty()) {
				if (isFinished) {
					break;
				}
				isFinished = true;
				words.remove(words.size() - 1);
				logger.info("Found " + words + " in:");
				for (final Track track : prevTracks) {
					logger.info("  " + track);
				}
				i--;
				words.clear();
			} else {
				isFinished = false;
			}
			prevTracks = tracks;
		}
	}

	Collection<Track> findTracks(final List<String> words, final int minYear) {
		String wordsStr = "";
		for (final String word : words) {
			if (!wordsStr.isEmpty()) {
				wordsStr += " ";
			}
			wordsStr += word;
		}
		if (wordsStr.isEmpty()) {
			return new HashSet<Track>();
		}
		final String wordsStrF = " " + wordsStr + " ";
		final String wordsStrFstart = wordsStr + " ";
		final String wordsStrFend = " " + wordsStr;
		return getObjectContainer().query(new Predicate<Track>() {
			@Override
			public boolean match(final Track track) {
				return track.getYear() >= minYear && track.getNormalizedLyrics() != null && ( //
						track.getNormalizedLyrics().contains(wordsStrF) || //
								track.getNormalizedLyrics().startsWith(wordsStrFstart) || //
						track.getNormalizedLyrics().endsWith(wordsStrFend) //
						);
			}
		});
	}

	public static void main(final String[] args) {
		new LyricsExtractor().run();
	}
}
