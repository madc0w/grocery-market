package arxiv.model;

import java.util.Date;

public class Entity {

	public static final String CREATION_DATE = "creationDate";

	private final Date creationDate;

	protected Entity() {
		this.creationDate = new Date();
	}

	public Date getCreationDate() {
		return creationDate;
	}
}
