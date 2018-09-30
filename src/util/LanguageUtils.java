package util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class LanguageUtils {

	private static final Map<String, Set<String>> equivalentWords = new HashMap<String, Set<String>>();

	public static Map<String, Set<String>> equivalentWords() {
		if (equivalentWords.isEmpty()) {
			{
				final Set<String> set = new HashSet<String>();
				set.add("loves");
				set.add("loving");
				equivalentWords.put("love", set);
			}
			{
				final Set<String> set = new HashSet<String>();
				set.add("hates");
				set.add("hating");
				equivalentWords.put("hate", set);
			}
			{
				final Set<String> set = new HashSet<String>();
				set.add("dreams");
				set.add("dreaming");
				equivalentWords.put("dream", set);
			}
			{
				final Set<String> set = new HashSet<String>();
				set.add("shits");
				set.add("shitting");
				equivalentWords.put("shit", set);
			}
			{
				final Set<String> set = new HashSet<String>();
				set.add("kisses");
				set.add("kissing");
				equivalentWords.put("kiss", set);
			}
			{
				final Set<String> set = new HashSet<String>();
				set.add("fucks");
				set.add("fucking");
				equivalentWords.put("fuck", set);
			}
			{
				final Set<String> set = new HashSet<String>();
				set.add("miracles");
				equivalentWords.put("miracle", set);
			}
			{
				final Set<String> set = new HashSet<String>();
				set.add("good-bye");
				set.add("good-byes");
				set.add("goodbyes");
				equivalentWords.put("goodbye", set);
			}
			{
				final Set<String> set = new HashSet<String>();
				set.add("hearts");
				equivalentWords.put("heart", set);
			}
			{
				final Set<String> set = new HashSet<String>();
				set.add("souls");
				equivalentWords.put("soul", set);
			}
			{
				final Set<String> set = new HashSet<String>();
				set.add("storks");
				equivalentWords.put("stork", set);
			}
			{
				final Set<String> set = new HashSet<String>();
				set.add("wars");
				equivalentWords.put("war", set);
			}
		}
		return equivalentWords;
	}

	public static String getEquivalentWord(final String word) {
		for (final Entry<String, Set<String>> e : equivalentWords().entrySet()) {
			if (e.getValue().contains(word)) {
				return e.getKey();
			}
		}
		return word;
	}

	public static Map<String, Integer> generalEnglishFreqs() throws NumberFormatException, IOException {
		final Map<String, Integer> generalEnglishFreqs = new HashMap<String, Integer>();
		final File generalEnglishFreqsFile = new File("output", "wordFreqs.csv");
		final FileReader fr = new FileReader(generalEnglishFreqsFile);
		final LineNumberReader lnr = new LineNumberReader(fr);
		String line;
		while ((line = lnr.readLine()) != null) {
			final String[] fields = line.split(",");
			if (fields.length != 2) {
				System.err.println("line has " + fields.length + " fields: " + line);
			} else {
				String word = fields[0].substring(1, fields[0].length() - 1);
				int freq = (int) Double.parseDouble(fields[1]);
				word = LanguageUtils.getEquivalentWord(word);
				if (generalEnglishFreqs.get(word) != null) {
					freq += generalEnglishFreqs.get(word);
				}
				generalEnglishFreqs.put(word, freq);
			}
		}
		lnr.close();
		return generalEnglishFreqs;
	}

}
