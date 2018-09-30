package arxiv;

import grocerymarket.DatabaseTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import poplyrics.model.Artist;
import poplyrics.model.Track;
import arxiv.model.Author;
import arxiv.model.Paper;
import arxiv.model.Subject;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.Predicate;

public class Maintainance extends DatabaseTask {

	private final boolean isClientServerMode = true;
	private ObjectContainer db;

	@Override
	protected void performWork() throws Exception {
		final Predicate<Track> pred = new Predicate<Track>() {
			@Override
			public boolean match(final Track track) {
				return track.getNormalizedLyrics() != null && track.getNormalizedLyrics().contains("shit");
			}
		};
		showTracks(pred);
		//		countTracks();
	}

	void extractLyrics() throws IOException {
		final File outFile = new File("output", "allLyrics.txt");
		final FileWriter fw = new FileWriter(outFile);
		final ObjectSet<Track> tracks = getObjectContainer().query(Track.class);
		for (final Track track : tracks) {
			fw.write(track.getLyrics());
			fw.write("\n\n");
			fw.flush();
		}
		fw.close();
		System.out.println(tracks.size() + " Tracks");
	}

	void deleteTracks() {
		final ObjectSet<Track> tracks = getObjectContainer().query(Track.class);
		for (final Track track : tracks) {
			getObjectContainer().delete(track);
		}
		getObjectContainer().commit();
	}

	void deleteArtists() {
		final ObjectSet<Artist> artists = getObjectContainer().query(Artist.class);
		for (final Artist artist : artists) {
			getObjectContainer().delete(artist);
		}
		getObjectContainer().commit();
	}

	void countTracks() {
		for (int year = 1930; year <= 2014; year++) {
			final int yearF = year;
			final ObjectSet<Track> tracks = getObjectContainer().query(new Predicate<Track>() {

				@Override
				public boolean match(final Track track) {
					return track.getYear() == yearF;
				}
			});
			int numNoLyrics = 0;
			for (final Track t : tracks) {
				if (t.getLyrics() == null) {
					numNoLyrics++;
				}
			}

			System.out.println(year + "," + tracks.size() + "," + numNoLyrics);
			//			System.out.println(year + "," + tracks.size());
		}
	}

	void showTracks(final Predicate<Track> pred) {
		final ObjectSet<Track> tracks = getObjectContainer().query(pred);
		for (final Track track : tracks) {
			System.out.println(track.getYear());
			System.out.println(track);
			System.out.println(track.getLyrics());
		}
		System.out.println(tracks.size() + " Tracks");
	}

	void showTrackByTitle(final String title) {
		final ObjectSet<Track> tracks = getObjectContainer().query(new Predicate<Track>() {

			@Override
			public boolean match(final Track track) {
				return track.getName().equals(title);
			}
		});
		System.out.println(tracks.size() + " Tracks");
		for (final Track track : tracks) {
			System.out.println(track);
			System.out.println(track.getLyrics());
		}
	}

	void showTrackByLyric(final String lyric) {
		final ObjectSet<Track> tracks = getObjectContainer().query(new Predicate<Track>() {

			@Override
			public boolean match(final Track track) {
				return track.getNormalizedLyrics().contains(lyric);
			}
		});
		System.out.println(tracks.size() + " Tracks");
		for (final Track track : tracks) {
			System.out.println(track);
			System.out.println(track.getYear());
			System.out.println(track.getLyrics());
			System.out.println();
		}
	}

	void fixSubjects() {
		final List<Paper> papers = getObjectContainer().query(Paper.class);
		final List<Subject> physicsSubjects = getObjectContainer().query(new Predicate<Subject>() {

			@Override
			public boolean match(final Subject subject) {
				return subject.getArxivCode().equals("physics");
			}
		});

		final Subject physics = physicsSubjects.iterator().next();
		int n = 0;
		for (final Paper paper : papers) {
			if (paper.getSubjects().isEmpty()) {
				n++;
				paper.getSubjects().add(physics);
				getObjectContainer().store(paper.getSubjects());
				logger.info("Added physics subject to " + n + " Papers");
			}
		}
		getObjectContainer().commit();
	}

	void cleanDuplicatePapers() {
		final List<Paper> papers = getObjectContainer().query(Paper.class);
		final Map<String, List<Paper>> foundPapers = new HashMap<String, List<Paper>>();
		for (final Paper paper : papers) {
			if (foundPapers.containsKey(paper.getPdfUrl())) {
				final List<Paper> foundPapersForKey = foundPapers.get(paper.getPdfUrl());
				foundPapersForKey.add(paper);
			} else {
				final List<Paper> list = new ArrayList<Paper>();
				list.add(paper);
				foundPapers.put(paper.getPdfUrl(), list);
			}
		}

		for (final Entry<String, List<Paper>> entry : foundPapers.entrySet()) {
			if (entry.getValue().size() > 1) {
				boolean isFirst = true;
				for (final Paper paper : entry.getValue()) {
					if (!isFirst) {
						logger.info("Deleting duplicate of " + paper.getPdfUrl());
						getObjectContainer().delete(paper);
						getObjectContainer().commit();
					}
					isFirst = false;
				}
			}
		}

	}

	void writeAbstracts() throws IOException {
		final File outFile = new File("output", "abstracts.txt");
		final FileWriter fw = new FileWriter(outFile);
		int n = 0;
		//		final List<Paper> papers = getObjectContainer().query(new Predicate<Paper>() {
		//
		//			@Override
		//			public boolean match(final Paper arg0) {
		//				return true;
		//			}
		//
		//		}, new Comparator<Paper>() {
		//			@Override
		//			public int compare(final Paper paper1, final Paper paper2) {
		//				return paper1.getSubmissionDate().compareTo(paper2.getSubmissionDate());
		//			}
		//		});
		final List<Paper> papers = getObjectContainer().query(Paper.class);
		//		final List<Paper> papers = new ArrayList<Paper>(getObjectContainer().query(Paper.class)); //new Predicate<Paper>() {
		//
		//			@Override
		//			public boolean match(final Paper paper) {
		//				final boolean test = true;
		//				return test;
		//			}
		//		}));
		//		Collections.sort(papers, new Comparator<Paper>() {
		//			@Override
		//			public int compare(final Paper paper1, final Paper paper2) {
		//				return paper1.getSubmissionDate().compareTo(paper2.getSubmissionDate());
		//			}
		//		});
		final Format dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		for (final Paper paper : papers) {
			logger.info("Writing abstract for paper with submission date " + dateFormat.format(paper.getSubmissionDate()));
			if (paper.getAbstractText() != null) {
				n++;
				fw.write(paper.getAbstractText());
				fw.write("\n");
				fw.flush();
			}
		}
		logger.info("Wrote " + n + " abstracts");
		fw.close();
	}

	void cleanAuthors() {
		for (final Author author : getObjectContainer().query(Author.class)) {
			final Collection<Author> matchingAuthors = query(new Predicate<Author>() {
				@Override
				public boolean match(final Author author2) {
					return !author.getName().equals(author2.getName()) && author2.getUrl().equals(author.getUrl());
				}
			}, Author.class);

			if (!matchingAuthors.isEmpty()) {
				System.out.println("Found " + matchingAuthors.size() + " duplicate URLs for " + author + " :");
				for (final Author matchingAuthor : matchingAuthors) {
					System.out.println("\t" + matchingAuthor.getName());
				}
			}
		}
	}

	void displayAuthors() {
		final List<Author> authors = new ArrayList<Author>(getObjectContainer().query(new Predicate<Author>() {
			@Override
			public boolean match(final Author author) {
				final boolean test = true;
				return test;
			}
		}));

		//		List<Author> authors = new ArrayList<Author>(getObjectContainer().query(Author.class));
		Collections.sort(authors);
		for (final Author author : authors) {
			System.out.println(author);
		}
	}

	void cleanPapers() {
		final ObjectSet<Paper> papers = getObjectContainer().query(Paper.class);
		for (final Paper paper : papers) {
			if (paper.getSubjects().contains(null)) {
				System.out.println(paper);
				System.out.println("before: " + paper.getSubjects().size());
				paper.getSubjects().remove(null);
				System.out.println("after: " + paper.getSubjects().size());
				getObjectContainer().store(paper.getSubjects());
			}
		}
		getObjectContainer().commit();
	}

	//	private void deleteSubjects() {
	//		final ObjectSet<Subject> subjectsToDelete = getObjectContainer().query(new Predicate<Subject>() {
	//			@Override
	//			public boolean match(Subject subject) {
	//				boolean test = codesToDelete.contains(subject.getArxivCode());
	//				return test;
	//			}
	//		});
	//
	//		boolean didDeleteDouble = false;
	//		for (Subject subject : subjectsToDelete) {
	//			if (subject.getArxivCode().equals("cs.FL")) {
	//				if (!didDeleteDouble) {
	//					System.out.println("Deleting " + subject);
	//					getObjectContainer().delete(subject);
	//					didDeleteDouble = true;
	//				}
	//			} else {
	//				System.out.println("Deleting " + subject);
	//				getObjectContainer().delete(subject);
	//			}
	//		}
	//		getObjectContainer().commit();
	//	}

	@Override
	protected ObjectContainer getObjectContainer() {
		if (isClientServerMode) {
			return super.getObjectContainer();
		}
		if (db == null) {
			db = Db4oEmbedded.openFile(Db4oEmbedded.newConfiguration(), DatabaseServer.DATABASE_FILENAME);
		}
		return db;
	}

	private void displayPapers() {
		final ObjectSet<Paper> papers = getObjectContainer().query(Paper.class);
		System.out.println(papers.size() + " Papers");
		int n = 0;
		for (final Paper paper : papers) {
			n++;
			System.out.println(n + " " + paper.getPdfUrl());
			for (final Subject subject : paper.getSubjects()) {
				System.out.println("\t|" + subject + "|");
			}
		}

	}

	void displaySubjects() {
		final Collection<Subject> subjects = getObjectContainer().query(Subject.class);

		//		final ObjectSet<Subject> subjects = getObjectContainer().query(new Predicate<Subject>() {
		//			@Override
		//			public boolean match(Subject subject) {
		//				boolean test = true;
		//				return test;
		//			}
		//		}, new Comparator<Subject>() {
		//			@Override
		//			public int compare(Subject subject1, Subject subject2) {
		//				return subject1.getArxivCode().compareTo(subject2.getArxivCode());
		//			}
		//		});
		final Collection<Paper> papers = getObjectContainer().query(Paper.class);
		System.out.println("Found " + papers.size() + " Papers");

		int n = 0;
		for (final Subject subject : subjects) {
			n++;
			//			final Subject subject = new Subject(null, "cs.IT");
			//				papers = getObjectContainer().query(Paper.class);

			//			Collection<Paper> filteredPapers = query(new Predicate<Paper>() {
			//				@Override
			//				public boolean match(final Paper paper) {
			//					//						return paper.getAbstractText().equals(null);
			//					boolean test = paper.getSubjects().contains(subject);
			//					return test;
			//				}
			//			}, papers);
			System.out.println(subject + " : ");
			//			for (Paper paper : filteredPapers) {
			//				System.out.print("\t" + paper + "  ");
			//				boolean isFirst = true;
			//				for (Subject paperSubject : paper.getSubjects()) {
			//					if (paperSubject != null) {
			//						if (!isFirst) {
			//							System.out.print(", ");
			//						}
			//						System.out.print(paperSubject.getArxivCode());
			//						isFirst = false;
			//					}
			//				}
			//				System.out.println();
			//			}
		}
	}

	public <T> Set<T> query(final Predicate<T> predicate, final Class<T> clazz) {
		final ObjectSet<T> results = getObjectContainer().query(clazz);
		return query(predicate, results);
	}

	public <T> Set<T> query(final Predicate<T> predicate, final Collection<T> allObjects) {
		final Set<T> filtered = new HashSet<T>();
		for (final T result : allObjects) {
			if (predicate.match(result)) {
				filtered.add(result);
			}
		}
		return filtered;
	}

	public static void main(final String[] args) {
		new Maintainance().run();
	}
}
