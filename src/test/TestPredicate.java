package test;

import test.model.TestObject;

import com.db4o.query.Predicate;

public class TestPredicate extends Predicate<TestObject> {
	@Override
	public boolean match(TestObject testObject) {
		if (testObject.getName().contains("12"))
			throw new RuntimeException();
		return true;
		//		return testObject.getName().equals(null) == testObject.getName().equals(null);
	}
}
