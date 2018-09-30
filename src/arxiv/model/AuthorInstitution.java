package arxiv.model;

import java.util.Set;

public class AuthorInstitution extends Entity {

	private final Author author;
	private final Set<Institution> institutions;

	public AuthorInstitution(Author author, Set<Institution> institutions) {
		this.author = author;
		this.institutions = institutions;
	}

	public Author getAuthor() {
		return author;
	}

	public Set<Institution> getInstitution() {
		return institutions;
	}
}
