package arxiv;

import org.apache.log4j.Logger;

import arxiv.model.Subject;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.Predicate;

public class ArxivService {
	protected final Logger logger = Logger.getLogger(getClass());

	private final ObjectContainer oc;

	public ArxivService(final ObjectContainer oc) {
		this.oc = oc;
	}

	public Subject getSubject(final String code, final String name) {
		Subject parentSubject = null;
		if (code.indexOf('.') > 0) {
			final String parentSubjectCode = code.split("\\.")[0];
			parentSubject = getSubject(parentSubjectCode, null);
		}

		final ObjectSet<Subject> result = oc.query(new Predicate<Subject>() {
			@Override
			public boolean match(final Subject subject) {
				return subject.getArxivCode().equals(code);
			}
		});
		if (result.size() > 1) {
			throw new IllegalStateException(result.size() + " Subjects found having code " + code);
		}
		Subject subject;
		if (result.isEmpty()) {
			if (code.matches(".*[^a-zA-Z\\.\\-].*")) {
				throw new IllegalStateException("Attempt to create subject with code'" + code + "' aborted.");
			} else {
				logger.info("Creating Subject " + code);
				subject = new Subject(name, code);
				subject.setParentSubject(parentSubject);
				oc.store(subject);
				oc.commit();
			}
		} else {
			subject = result.get(0);
		}
		return subject;
	}

}
