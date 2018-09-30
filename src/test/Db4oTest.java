package test;

import test.model.TestObject;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.diagnostic.Diagnostic;
import com.db4o.diagnostic.DiagnosticListener;
import com.db4o.messaging.MessageContext;
import com.db4o.messaging.MessageRecipient;
import com.db4o.query.Predicate;

public class Db4oTest implements Runnable {

	private final ObjectContainer db = Db4oEmbedded.openFile(Db4oEmbedded.newConfiguration(), "test.dat");

	@Override
	public void run() {
		db.ext().configure().objectClass(TestObject.class).objectField("name").indexed(true);
		db.ext().configure().setOut(System.out);
		db.ext().configure().diagnostic().addListener(new DiagnosticListener() {
			@Override
			public void onDiagnostic(final Diagnostic diag) {
				System.out.println(diag);
			}
		});
		db.ext().configure().clientServer().setMessageRecipient(new MessageRecipient() {
			@Override
			public void processMessage(MessageContext context, Object o) {
				System.out.println(o);
			}
		});

		//		storeObjects();
		queryObjects();
	}

	private void queryObjects() {
		ObjectSet<TestObject> result = db.query(new Predicate<TestObject>() {
			@Override
			public boolean match(TestObject testObject) {
				return testObject.getName().equals("kaka");
				//				boolean test = testObject.getName().length() == 6;
				//				return test;
			}
		});
		System.out.println(result.size()); // displays 100... this should display 0!
	}

	private void storeObjects() {
		for (int i = 0; i < 100; i++) {
			TestObject testObj = new TestObject("test " + i);
			db.store(testObj);
		}
		db.commit();
	}

	public static void main(String[] args) {
		new Db4oTest().run();
	}

}
