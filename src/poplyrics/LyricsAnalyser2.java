package poplyrics;

import grocerymarket.DatabaseTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import poplyrics.model.Track;
import util.LanguageUtils;

import com.db4o.query.Predicate;

public class LyricsAnalyser2 extends DatabaseTask {

	@Override
	protected void performWork() throws Exception {
		//		final String[] words = new String[] { "love", "hate", "peace", "war", "shit", "fuck", "hey", "yeah", "oh", "heart", "soul", "mama",
		//				"baby", "kiss", "sex", "stork", "yes", "no", "hello", "goodbye", "good", "bad", "sexy" };
		final String[] words = new String[] { "no" };

		for (final String queryWord : words) {
			System.out.println();
			System.out.println(queryWord);
			final int step = 5;
			for (int year = 1930; year <= 2015 - step; year += step) {
				analyze(queryWord, year, year + step);
			}
		}
	}

	private void analyze(final String queryWord, final int minYear, final int maxYear) throws NumberFormatException, IOException {
		final Map<String, Integer> wordFreqs = new HashMap<String, Integer>();
		final Map<String, Integer> wordTrackFreqs = new HashMap<String, Integer>();
		final Collection<Track> tracks = getObjectContainer().query(new Predicate<Track>() {

			@Override
			public boolean match(final Track track) {
				return track.getYear() >= minYear && track.getYear() < maxYear;
			}
		});

		for (final Track track : tracks) {
			if (track.getNormalizedLyrics() != null) {
				final Set<String> didPutWord = new HashSet<String>();
				for (String word : track.getNormalizedLyrics().split("\\s+")) {
					word = word.replaceAll("[\\?\\\"]", "");
					if (!word.trim().isEmpty()) {
						word = LanguageUtils.getEquivalentWord(word);
						Integer n = wordFreqs.get(word);
						if (n == null) {
							n = 0;
						}
						wordFreqs.put(word, n + 1);

						if (!didPutWord.contains(word)) {
							Integer trackN = wordTrackFreqs.get(word);
							if (trackN == null) {
								trackN = 0;
							}
							wordTrackFreqs.put(word, trackN + 1);
							didPutWord.add(word);
						}
					}
				}
			}
		}

		final Map<String, Integer> generalEnglishFreqs = LanguageUtils.generalEnglishFreqs();
		int nWordsEnglish = 0;
		for (final Entry<String, Integer> e : generalEnglishFreqs.entrySet()) {
			nWordsEnglish += e.getValue();
		}

		int nLyrics = 0;
		for (final Entry<String, Integer> e : wordFreqs.entrySet()) {
			nLyrics += e.getValue();
		}

		final Map<String, Double> comparingFreqs = new HashMap<String, Double>();
		for (final Entry<String, Integer> entry : wordFreqs.entrySet()) {
			final String word = entry.getKey();
			final double relativeFreqLyrics = (double) entry.getValue() / (double) nLyrics;
			if (generalEnglishFreqs.containsKey(word)) {
				final double relativeFreqGeneralEnglish = (double) generalEnglishFreqs.get(word) / (double) nWordsEnglish;
				comparingFreqs.put(word, relativeFreqLyrics / relativeFreqGeneralEnglish);
			}
		}

		final List<String> words = new ArrayList<String>(comparingFreqs.keySet());
		Collections.sort(words, new Comparator<String>() {

			@Override
			public int compare(final String word1, final String word2) {
				if (!generalEnglishFreqs.containsKey(word1) && !generalEnglishFreqs.containsKey(word2)) {
					return wordFreqs.get(word1).compareTo(wordFreqs.get(word2));
				} else {
					return comparingFreqs.get(word1).compareTo(comparingFreqs.get(word2));
				}
			}
		});

		if (wordFreqs.containsKey(queryWord)) {
			final double ratio = (double) wordTrackFreqs.get(queryWord) / tracks.size();
			System.out.println(minYear + "," + comparingFreqs.get(queryWord) + "," + generalEnglishFreqs.get(queryWord) + ","
					+ wordFreqs.get(queryWord) + "," + ratio);
		} else {
			System.out.println(minYear + ",0," + generalEnglishFreqs.get(queryWord) + ",0,0");
		}
	}

	public static void main(final String[] args) {
		new LyricsAnalyser2().run();
	}
}
