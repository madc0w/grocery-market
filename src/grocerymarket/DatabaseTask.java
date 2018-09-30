package grocerymarket;

import org.apache.log4j.Logger;

import com.db4o.ObjectContainer;
import com.db4o.cs.Db4oClientServer;

public abstract class DatabaseTask implements Runnable {
	protected final Logger logger = Logger.getLogger(getClass());

	public static final String DEV_KEY = "AI39si4GBkDc6l3RudY4XgAKyGMg62sPf_3GucGGlcJitZS6H8whJEvWswepxTrnd-zz3TPcYfGpksy_ngTSvUh-DfqUaSOA2Q";

	private ObjectContainer db;

	protected int getPort() {
		return DatabaseServer.PORT;
	}

	protected DatabaseTask() {
		try {
			db = Db4oClientServer.openClient("localhost", getPort(), DatabaseServer.DB_USER, DatabaseServer.DB_PASSWORD).ext()
					.openSession();
		} catch (final Exception e) {
			logger.error(e);
		}
		//		final EmbeddedConfiguration config = Db4oEmbedded.newConfiguration();
		//		config.common().objectClass(Forecast.class).objectField(Forecast.DATE).indexed(true);
		//		db = Db4oEmbedded.openFile(config, "db_test.dat").ext().openSession();

	}

	protected void resetObjectContainer() {
		try {
			db.close();
		} catch (final Exception e) {
			logger.error("Failed to close db", e);
		}
		db = Db4oClientServer.openClient("localhost", 5555, DatabaseServer.DB_USER, DatabaseServer.DB_PASSWORD).ext().openSession();
		logger.info("ObjectContainer was successfully reset");
	}

	protected ObjectContainer getObjectContainer() {
		return db;
		//		while (true) {
		//			try {
		//				final EmbeddedConfiguration config = Db4oEmbedded.newConfiguration();
		//				config.common().objectClass(Forecast.class).objectField(Forecast.DATE).indexed(true);
		//				final ObjectContainer db = Db4oEmbedded.openFile(config, "db_test.dat");
		//				return db.ext().openSession();
		//			} catch (final DatabaseFileLockedException e) {
		//				try {
		//					Thread.sleep(200);
		//					logger.warn("database was locked.  trying again.");
		//				} catch (InterruptedException e1) {
		//				}
		//			}
		//		}
	}

	@Override
	public final void run() {
		logger.info("starting");
		try {
			performWork();
			logger.info("finished!");
		} catch (final Exception e) {
			logger.error("fail", e);
			e.printStackTrace();
		} finally {
			//			db.close();
		}
	}

	protected abstract void performWork() throws Exception;

}
