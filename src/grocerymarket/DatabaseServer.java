package grocerymarket;

import org.apache.log4j.Logger;

import com.db4o.ObjectServer;
import com.db4o.cs.Db4oClientServer;
import com.db4o.diagnostic.Diagnostic;
import com.db4o.diagnostic.DiagnosticListener;
import com.db4o.messaging.MessageContext;
import com.db4o.messaging.MessageRecipient;

public class DatabaseServer implements Runnable {
	protected static final Logger logger = Logger.getLogger(DatabaseServer.class);

	public static final int PORT = 5556;
	public static final String DB4OFILENAME = "poplyrics.dat";
	public static final String DB_USER = "admin";
	public static final String DB_PASSWORD = "blimey";

	protected ObjectServer server;

	public static void main(final String[] args) {
		new DatabaseServer().run();
	}

	protected String getDatabaseFileName() {
		return DB4OFILENAME;
	}

	protected int getPort() {
		return PORT;
	}

	@Override
	public void run() {
		server = Db4oClientServer.openServer(getDatabaseFileName(), getPort());

		//		final ServerConfiguration config = Db4oClientServer.newServerConfiguration();
		//		config.common().objectClass(Forecast.class).objectField(Forecast.DATE).indexed(true);

		//		server.ext().configure().objectClass(Forecast.class).objectField(Forecast.DATE).indexed(true);
		//		server.ext().configure().objectClass(Condition.class).objectField(Condition.LABEL).indexed(true);
		//		server.ext().configure().objectClass(Location.class).objectField(Location.CODE).indexed(true);
		server.ext().configure().clientServer().timeoutClientSocket(12 * 60 * 60 * 1000);
		server.ext().configure().clientServer().timeoutServerSocket(12 * 60 * 60 * 1000);

		server.ext().configure().setOut(System.out);
		server.ext().configure().diagnostic().addListener(new DiagnosticListener() {
			@Override
			public void onDiagnostic(final Diagnostic diag) {
				//				logger.info(diag.toString());
			}
		});
		server.ext().configure().clientServer().setMessageRecipient(new MessageRecipient() {

			@Override
			public void processMessage(final MessageContext context, final Object o) {
				logger.info(o.toString());
			}
		});
		try {
			server.grantAccess(DB_USER, DB_PASSWORD);

			logger.info("database is up");
			while (true) {
				try {
					Thread.sleep(10000);
				} catch (final InterruptedException e) {
				}
			}
		} catch (final Exception e) {
			logger.error(e);
		} finally {
			server.close();
		}
	}

}
