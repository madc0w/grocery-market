package grocerymarket.model;

public enum Store {
	AUCHAN("Auchan Drive"), //
	CACTUS("Cactus@home");

	private final String label;

	private Store(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
