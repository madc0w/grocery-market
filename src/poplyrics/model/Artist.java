package poplyrics.model;

import arxiv.model.Entity;

public class Artist extends Entity {
	private final String name;

	public Artist(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Artist) {
			return ((Artist) obj).getName().equals(getName());
		}
		return false;
	}
}
