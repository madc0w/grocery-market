package poplyrics;

import grocerymarket.DatabaseTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import poplyrics.model.Track;
import util.LanguageUtils;

import com.db4o.query.Predicate;

public class LyricsAnalyser extends DatabaseTask {

	final File outDir = new File("output", "popLyrics");
	final Format dateFormat = new SimpleDateFormat("yyyy");

	LyricsAnalyser() {
	}

	@Override
	protected void performWork() throws Exception {
		final int step = 5;
		for (int year = 1930; year <= 2000 - step; year += step) {
			writeFile(year, year + step);
		}
		writeFile(1930, 2000);
	}

	void writeFile(final int minYear, final int maxYear) throws NumberFormatException, IOException {
		final Map<String, Integer> wordFreqs = new HashMap<String, Integer>();
		for (final Track track : getObjectContainer().query(new Predicate<Track>() {

			@Override
			public boolean match(final Track track) {
				return track.getYear() >= minYear && track.getYear() < maxYear;
			}
		})) {
			if (track.getNormalizedLyrics() != null) {
				for (String word : track.getNormalizedLyrics().split("\\s+")) {
					word = word.replaceAll("[\\?\\\"]", "");
					if (!word.trim().isEmpty()) {
						Integer n = wordFreqs.get(word);
						if (n == null) {
							n = 0;
						}
						wordFreqs.put(word, n + 1);
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

		final File outFile = new File(outDir, "pop lyrics freqs " + minYear + "-" + (maxYear - 1) + ".csv");
		final FileWriter fw = new FileWriter(outFile);
		fw.write("word,relative freq (lyrics / general usage),general usage absolute freq,pop lyrics absolute freq\n");
		for (final String word : words) {
			final String csvLine = "\"" + word + "\"," + comparingFreqs.get(word) + "," + generalEnglishFreqs.get(word) + ","
					+ wordFreqs.get(word);
			fw.write(csvLine + "\n");
			fw.flush();

			if (word.equals("magic")) {
				System.out.println(minYear + "," + comparingFreqs.get(word) + "," + generalEnglishFreqs.get(word) + ","
						+ wordFreqs.get(word));
			}
		}
		fw.close();
		//		logger.info("Wrote file " + outFile.getAbsolutePath());
	}

	public static void main(final String[] args) {
		new LyricsAnalyser().run();
	}
}
