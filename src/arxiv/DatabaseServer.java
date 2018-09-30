package arxiv;

import arxiv.model.Paper;

public class DatabaseServer extends grocerymarket.DatabaseServer {

	public static final String DATABASE_FILENAME = "arxiv.dat";

	@Override
	protected String getDatabaseFileName() {
		return DATABASE_FILENAME;
	}

	@Override
	public void run() {
		super.run();
		server.ext().configure().objectClass(Paper.class).objectField(Paper.PDF_URL).indexed(true);
	}

	public static void main(final String[] args) {
		new DatabaseServer().run();
	}

}
