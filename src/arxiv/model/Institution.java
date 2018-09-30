package arxiv.model;

public class Institution extends Entity {

	private final String name;

	public Institution(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
