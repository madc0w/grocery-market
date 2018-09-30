package grocerymarket;

import java.net.URL;

public interface ResourceIterator<T> {

	public T next();

	public URL makeUrl();

	public boolean hasNext();
}
