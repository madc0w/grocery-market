package arxiv.model;

import java.util.HashSet;
import java.util.Set;

public class Author extends Entity implements Comparable<Author> {

	public static final String NAME = "name";
	public static final String URLS = "url";

	private final String name;
	private final String url;
	private final Set<String> otherNames = new HashSet<String>();

	public Author(String name, String url) {
		this.url = url;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public int compareTo(Author o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public String toString() {
		return getName();
	}

	public Set<String> getOtherNames() {
		return otherNames;
	}
}
