package tagcloud;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;

import org.mcavallo.opencloud.Cloud;
import org.mcavallo.opencloud.Tag;

public class CorpusAnalyser implements Runnable {

	File inDir = new File("input", "txt");
	File outDir = new File("output");

	@Override
	public void run() {
		final Cloud cloud = new Cloud();

		try {
			for (final File inFile : inDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(final File file) {
					return file.getName().endsWith(".txt");
				}
			})) {
				String line;
				final FileReader fr = new FileReader(inFile);
				final LineNumberReader lnr = new LineNumberReader(fr);
				while ((line = lnr.readLine()) != null) {
					line = line.toLowerCase();
					cloud.addText(line);
				}
				lnr.close();
			}

			final File outFile = new File(outDir, "wordFreqs.csv");
			final FileWriter fw = new FileWriter(outFile);
			for (final Tag tag : cloud.allTags()) {
				fw.write("\"" + tag.getName() + "\"," + tag.getScore() + "\n");
			}
			fw.close();

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		new CorpusAnalyser().run();
	}
}
