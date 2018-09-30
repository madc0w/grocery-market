package grocerymarket.model;

public class Item {
	private final String label;
	private final double price;
	private final String unitPrice;
	private final Store store;

	public Item(String label, double price, String unitPrice, Store store) {
		this.label = label;
		this.price = price;
		this.unitPrice = unitPrice;
		this.store = store;
	}

	public String getLabel() {
		return label;
	}

	public double getPrice() {
		return price;
	}

	public String getUnitPrice() {
		return unitPrice;
	}

	public Store getStore() {
		return store;
	}

	@Override
	public String toString() {
		return getLabel() + " " + getStore().getLabel();
	}
}
