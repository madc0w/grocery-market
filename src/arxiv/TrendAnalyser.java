package arxiv;

import grocerymarket.DatabaseTask;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;

public class TrendAnalyser extends DatabaseTask {

	final String word = "graphene";
	final File inDir = new File("output");
	final WordFreq nullWordFreq = new WordFreq("\"" + word + "\",0,?,0");

	@Override
	protected void performWork() throws Exception {
		final File outFile = new File(inDir, "freqs_" + word + ".csv");
		final FileWriter fw = new FileWriter(outFile);
		for (final File file : inDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(final File dir, final String fileName) {
				return fileName.matches("^freqs_\\d{4}-\\d{2}_\\d{4}-\\d{2}\\.csv$");
			}
		})) {
			final WordFreq wordFreq = getLine(file);
			fw.write(wordFreq + "," + file.getName() + "\n");
			logger.info(file.getName());
		}
		fw.close();
	}

	WordFreq getLine(final File file) throws IOException {
		final FileReader fr = new FileReader(file);
		final LineNumberReader lnr = new LineNumberReader(fr);
		try {
			String line;
			while ((line = lnr.readLine()) != null) {
				final WordFreq wordFreq = new WordFreq(line);
				if (wordFreq.word.equals(word)) {
					return wordFreq;
				}
			}
			return nullWordFreq;
		} finally {
			lnr.close();
		}
	}

	public static void main(final String[] args) {
		new TrendAnalyser().run();
	}

	class WordFreq {
		final String word;
		final double relativeFreq;
		int generalFreq, domainFreq;

		WordFreq(final String line) {
			final String[] fields = line.split(",");
			word = fields[0].substring(1, fields[0].length() - 1);
			relativeFreq = Double.parseDouble(fields[1]);
			try {
				generalFreq = Integer.parseInt(fields[2]);
			} catch (final NumberFormatException e) {
				generalFreq = 0;
			}
			try {
				domainFreq = Integer.parseInt(fields[3]);
			} catch (final NumberFormatException e) {
				domainFreq = 0;
			}
		}

		@Override
		public String toString() {
			return "\"" + word + "\"," + relativeFreq + "," + generalFreq + "," + domainFreq;
		}
	}
}
