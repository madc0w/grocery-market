package arxiv.model;

public class Subject extends Entity {

	private final String name;
	private final String arxivCode;
	private Subject parentSubject;

	public Subject(String name, String arxivCode) {
		this.name = name;
		this.arxivCode = arxivCode;
	}

	public String getName() {
		return name;
	}

	public String getArxivCode() {
		return arxivCode;
	}

	public void setParentSubject(Subject parentSubject) {
		this.parentSubject = parentSubject;
	}

	public Subject getParentSubject() {
		return parentSubject;
	}

	@Override
	public String toString() {
		String toString = getArxivCode();
		if (getName() != null) {
			toString += ", " + getName();
		}
		if (getParentSubject() != null) {
			toString += ",  parent: " + getParentSubject();
		}
		return toString;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Subject) {
			return ((Subject) obj).getArxivCode().equals(getArxivCode());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getArxivCode().hashCode();
	}
}
