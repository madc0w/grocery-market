package arxiv;

import grocerymarket.DatabaseTask;

import java.io.File;
import java.io.FileWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mcavallo.opencloud.Cloud;
import org.mcavallo.opencloud.Tag;

import util.LanguageUtils;
import arxiv.model.Paper;

public class ArxivAbstractAnalyser extends DatabaseTask {

	final File outDir = new File("output", "arxiv");
	final Format dateFormat = new SimpleDateFormat("yyyy-MM");

	@Override
	protected void performWork() throws Exception {
		final Map<String, Integer> generalEnglishFreqs = LanguageUtils.generalEnglishFreqs();
		int nWordsEnglish = 0;
		for (final Entry<String, Integer> e : generalEnglishFreqs.entrySet()) {
			nWordsEnglish += e.getValue();
		}

		final List<Paper> papers = getObjectContainer().query(Paper.class);
		logger.info("Fetched " + papers.size() + " Papers");
		//		cloud.addInputFilter(new Filter<Tag>() {
		//
		//			@Override
		//			public boolean accept(final Tag tag) {
		//				final boolean accept = cloud.getTag(tag) == null;
		//				if (accept) {
		//					System.out.println("adding " + tag.getName());
		//				} else {
		//					System.out.println("NOT adding " + tag.getName());
		//				}
		//				return accept;
		//			}
		//
		//			@Override
		//			public void filter(final Collection<Tag> tag) {
		//			}
		//
		//		});

		final Date lastDate = new Date();
		final Calendar calStart = Calendar.getInstance();
		calStart.set(Calendar.DAY_OF_MONTH, 1);
		calStart.set(Calendar.MONTH, Calendar.JANUARY);
		calStart.set(Calendar.YEAR, 2007);
		Date startDate = calStart.getTime();

		final Calendar calEnd = Calendar.getInstance();
		calEnd.setTime(startDate);
		calEnd.add(Calendar.MONTH, 3);
		Date endDate = calEnd.getTime();

		do {
			final Cloud cloud = new Cloud();
			//		int i = 0;
			for (final Paper paper : papers) {
				if (!paper.getSubmissionDate().before(startDate) && !paper.getSubmissionDate().after(endDate)) {
					String abstractText = paper.getAbstractText().toLowerCase();
					abstractText = abstractText.replaceAll("\\\"", "");
					cloud.addText(abstractText);
					//			i++;
					//			if (i % 1000 == 0) {
					//				logger.info("Added " + i + " abstracts");
					//			}
				}
			}

			for (final Tag tag : cloud.allTags()) {
				if (tag.getName().matches(".*\\d.*")) {
					cloud.removeTag(tag);
				}
			}

			logger.info("Found " + cloud.allTags().size() + " tags");
			int nWordsAbstracts = 0;
			final Comparator<Tag> c = new Comparator<Tag>() {

				@Override
				public int compare(final Tag t1, final Tag t2) {
					return new Double(t1.getScore()).compareTo(t2.getScore());
				}
			};

			final Map<String, Integer> abstractFreqs = new HashMap<String, Integer>();
			final List<Tag> tags = cloud.allTags(c);
			for (final Tag tag : tags) {
				tag.setLink("http://arxiv.org");
				nWordsAbstracts += tag.getScore();
				abstractFreqs.put(tag.getName(), tag.getScoreInt());
				//			System.out.println("- " + tag.getName() + " NormScore=" + tag.getNormScore() + " Score=" + tag.getScore() + " Weight="
				//					+ tag.getWeight());
			}

			final Map<String, Double> comparingFreqs = new HashMap<String, Double>();
			for (final Entry<String, Integer> entry : abstractFreqs.entrySet()) {
				final String word = entry.getKey();
				final double relativeFreqAbstract = (double) entry.getValue() / (double) nWordsAbstracts;
				final double relativeFreqGeneralEnglish;
				if (generalEnglishFreqs.containsKey(word)) {
					relativeFreqGeneralEnglish = (double) generalEnglishFreqs.get(word) / (double) nWordsEnglish;
				} else {
					relativeFreqGeneralEnglish = 0;
				}
				comparingFreqs.put(word, relativeFreqAbstract / relativeFreqGeneralEnglish);
			}

			final List<String> words = new ArrayList<String>(comparingFreqs.keySet());
			Collections.sort(words, new Comparator<String>() {

				@Override
				public int compare(final String word1, final String word2) {
					if (!generalEnglishFreqs.containsKey(word1) && !generalEnglishFreqs.containsKey(word2)) {
						return abstractFreqs.get(word1).compareTo(abstractFreqs.get(word2));
					} else {
						return comparingFreqs.get(word1).compareTo(comparingFreqs.get(word2));
					}
				}
			});
			final File outFile = new File(outDir, "freqs_" + dateFormat.format(startDate) + "_" + dateFormat.format(endDate) + ".csv");
			final FileWriter fw = new FileWriter(outFile);
			for (final String word : words) {
				final String csvLine = "\"" + word + "\"," + comparingFreqs.get(word) + "," + generalEnglishFreqs.get(word) + ","
						+ abstractFreqs.get(word);
				fw.write(csvLine + "\n");
				fw.flush();
			}
			fw.close();
			logger.info("Wrote file " + outFile.getAbsolutePath());

			//		cloud.setMaxTagsToDisplay(1000);
			//		for (final Tag tag : tags) {
			//			final double r = tag.getScore() / total;
			//			if (r > 0.003) {
			//				System.out.println("Remvoing Tag " + tag.getName() + " having ratio " + r);
			//				cloud.removeTag(tag);
			//			}
			//		}

			//		final HTMLFormatter formatter = new HTMLFormatter();
			//		final String html = formatter.html(cloud, new Comparator<Tag>() {
			//
			//			@Override
			//			public int compare(final Tag o1, final Tag o2) {
			//				return Math.random() < 0.5 ? -1 : 1;
			//			}
			//		});
			//		System.out.println(html);

			calStart.add(Calendar.MONTH, 1);
			calEnd.add(Calendar.MONTH, 1);
			startDate = calStart.getTime();
			endDate = calEnd.getTime();
		} while (endDate.before(lastDate));
	}

	public static void main(final String[] args) {
		new ArxivAbstractAnalyser().run();
	}
}
