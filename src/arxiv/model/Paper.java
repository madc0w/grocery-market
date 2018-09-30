package arxiv.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Paper extends Entity implements Comparable<Paper> {

	public static final String PDF_URL = "pdfUrl";

	private final String title, abstractText, comments;
	private final List<Author> authors = new ArrayList<Author>();
	private final List<Subject> subjects = new ArrayList<Subject>();
	private final String pdfUrl, paperUrl;
	private final Date submissionDate;
	private final int numPages, numFigures;
	private final Set<Paper> refersTo = new HashSet<Paper>();
	private final Set<Paper> citedBy = new HashSet<Paper>();

	public Paper(final String title, final String abstractText, final String pdfUrl, final String paperUrl, final Date submissionDate,
			final int numPages, final int numFigures, final String comments) {
		this.title = title;
		this.abstractText = abstractText;
		this.pdfUrl = pdfUrl;
		this.paperUrl = paperUrl;
		this.submissionDate = submissionDate;
		this.numFigures = numFigures;
		this.numPages = numPages;
		this.comments = comments;
	}

	public String getTitle() {
		return title;
	}

	public List<Author> getAuthors() {
		return authors;
	}

	public String getPdfUrl() {
		return pdfUrl;
	}

	public String getAbstractText() {
		return abstractText;
	}

	public Date getSubmissionDate() {
		return submissionDate;
	}

	public List<Subject> getSubjects() {
		return subjects;
	}

	public int getNumFigures() {
		return numFigures;
	}

	public int getNumPages() {
		return numPages;
	}

	public String getComments() {
		return comments;
	}

	public Set<Paper> getRefersTo() {
		return refersTo;
	}

	public Set<Paper> getCitedBy() {
		return citedBy;
	}

	public String getPaperUrl() {
		return paperUrl;
	}

	@Override
	public String toString() {
		return getPaperUrl();
	}

	@Override
	public int compareTo(final Paper paper) {
		return getSubmissionDate().compareTo(paper.getSubmissionDate());
	}
}
