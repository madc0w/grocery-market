package arxiv;

import grocerymarket.DatabaseTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import arxiv.model.Author;
import arxiv.model.Paper;
import arxiv.model.Subject;

public class CsvExtractor extends DatabaseTask {

	public static final DateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy");
	public static final File outputDir = new File("output");

	@Override
	protected void performWork() throws Exception {
		writeSubjects();
		writeAuthors();
		writePapers();
	}

	private void writePapers() throws IOException {
		File csvFile = new File(outputDir, "papers.csv");
		FileWriter fw = new FileWriter(csvFile);
		String header = buildCsvLine("title", "url", "pdf url", "submission date", "abstract", "comments", "subject", "subject", "subject",
				"subject", "subject", "subject", "author", "author", "author", "author", "author");
		fw.append(header);
		fw.flush();

		for (Paper paper : getObjectContainer().query(Paper.class)) {
			List<String> subjects = new ArrayList<String>();
			int i = 0;
			for (Subject subject : paper.getSubjects()) {
				subjects.add(subject.getArxivCode());
				i++;
			}
			while (i < 6) {
				subjects.add(null);
				i++;
			}
			List<String> authors = new ArrayList<String>();
			for (Author author : paper.getAuthors()) {
				authors.add(author.getUrl());
			}

			String line = buildCsvLine(paper.getTitle(), paper.getPaperUrl(), paper.getPdfUrl(), paper.getSubmissionDate(),
					paper.getAbstractText(), paper.getComments(), subjects, authors);
			fw.append(line);
			fw.flush();
		}
		fw.close();
		logger.info("Finished writing Papers");
	}

	private void writeAuthors() throws IOException {
		File csvFile = new File(outputDir, "authors.csv");
		FileWriter fw = new FileWriter(csvFile);
		String header = buildCsvLine("URL", "name", "other name", "other name", "other name", "other name");
		fw.append(header);
		fw.flush();

		for (Author author : getObjectContainer().query(Author.class)) {
			String line = buildCsvLine(author.getUrl(), author.getName(), author.getOtherNames());
			fw.append(line);
			fw.flush();
		}
		fw.close();
		logger.info("Finished writing Authors");
	}

	private void writeSubjects() throws IOException {
		File subjectCsvFile = new File(outputDir, "subjects.csv");
		FileWriter fw = new FileWriter(subjectCsvFile);
		String header = buildCsvLine("code", "name", "parent code");
		fw.append(header);
		fw.flush();

		for (Subject subject : getObjectContainer().query(Subject.class)) {
			String line = buildCsvLine(subject.getArxivCode(), subject.getName() == null ? null : subject.getName(),
					subject.getParentSubject() == null ? null : subject.getParentSubject().getArxivCode());
			fw.append(line);
			fw.flush();
		}
		fw.close();
		logger.info("Finished writing Subjects");
	}

	public String buildCsvLine(Object... values) {
		StringBuilder s = new StringBuilder();
		boolean isFirst = true;
		for (Object value : values) {
			if (!isFirst) {
				s.append(",");
			}
			if (value instanceof Number) {
				s.append(value);
			} else if (value instanceof Date) {
				s.append(dateFormat.format(value));
			} else if (value instanceof Collection) {
				boolean isFirstInside = true;
				for (Object sInner : (Collection) value) {
					if (!isFirstInside) {
						s.append(",");
					}
					s.append("\"" + (sInner == null ? "" : sInner.toString().trim()) + "\"");
					isFirstInside = false;
				}
			} else if (value != null) {
				s.append("\"").append(value.toString().trim().replaceAll("\"", "'")).append("\"");
			}
			isFirst = false;
		}
		s.append("\n");
		return s.toString();
	}

	public static void main(String[] args) {
		new CsvExtractor().run();
	}
}
