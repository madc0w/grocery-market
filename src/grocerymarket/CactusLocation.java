package grocerymarket;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

public class CactusLocation implements ResourceIterator<CactusLocation> {
	protected final Logger logger = Logger.getLogger(getClass());

	String baseUrl;
	private int pageNum;

	CactusLocation(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@Override
	public CactusLocation next() {
		CactusLocation next = new CactusLocation(getBaseUrl());
		next.pageNum = this.pageNum + 1;
		return next;
	}

	@Override
	public URL makeUrl() {
		String urlStr = getBaseUrl() + "?page=" + getPageNum();
		try {
			return new URL(urlStr);
		} catch (MalformedURLException e) {
			logger.fatal("Failed to make URL from string : " + urlStr);
			return null;
		}
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public int getPageNum() {
		return pageNum;
	}

	@Override
	public boolean hasNext() {
		// TODO return false when no more pages
		return true;
	}

}
