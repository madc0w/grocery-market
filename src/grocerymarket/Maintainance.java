package grocerymarket;

import grocerymarket.model.Item;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.db4o.ObjectSet;

public class Maintainance extends DatabaseTask {

	@Override
	protected void performWork() throws Exception {
		dumpItems();
	}

	protected void dumpItems() throws IOException {
		File outFile = new File("output", "items.csv");
		FileWriter fw = new FileWriter(outFile);
		fw.write("item;price;unit price;store\n");
		ObjectSet<Item> items = getObjectContainer().query(Item.class);
		for (Item item : items) {
			fw.write(item.getLabel() + ";" + item.getPrice() + ";" + item.getUnitPrice() + ";" + item.getStore().getLabel() + "\n");
		}
		fw.close();
	}

	protected void countItems() {
		ObjectSet<Item> items = getObjectContainer().query(Item.class);
		logger.info("counted " + items.size() + " Items");
	}

	public static void main(String[] args) {
		new Maintainance().run();
	}
}
