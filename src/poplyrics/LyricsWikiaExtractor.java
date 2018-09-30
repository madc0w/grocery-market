package poplyrics;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class LyricsWikiaExtractor implements Runnable { //extends DatabaseTask {

	private final String nphServer;

	LyricsWikiaExtractor(final String nphServer) {
		this.nphServer = nphServer;
	}

	@Override
	public void run() {
		try {
			//	@Override
			//	protected void performWork() throws Exception {

			final URL url = new URL("http", "lyrics.wikia.com", "/Category:Billboard_Hits");
			final Document doc;
			try {
				doc = Jsoup.parse(url, 2000);
				System.out.println(doc);
			} catch (final IOException e1) {
				throw new RuntimeException(e1);
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	//	Artist getArtist(final String name) {
	//		Artist artist = new Artist(name);
	//		final ObjectSet<Artist> result = getObjectContainer().query(new Predicate<Artist>() {
	//			@Override
	//			public boolean match(final Artist artist) {
	//				return artist.getName().equals(name);
	//			}
	//		});
	//		if (result.isEmpty()) {
	//			getObjectContainer().store(artist);
	//		} else {
	//			artist = result.get(0);
	//		}
	//		return artist;
	//	}

	public InputStream getInputStream(final URL url) throws IOException {
		//		numRequests++;
		//		try {
		//			Thread.sleep((int) ((5 + Math.random() * 5) * 1000));
		//		} catch (final InterruptedException e) {
		//		}
		//		final long elapsedTime = new Date().getTime() - startTime.getTime();
		//		final double requestsPerSecond = (double) elapsedTime / 1000 / numRequests;
		//		logger.info("Mean seconds per request : " + doubleFormat.format(requestsPerSecond));
		if (nphServer == null) {
			int numFailures = 0;
			while (numFailures < 6) {
				try {
					return url.openStream();
				} catch (final IOException e) {
					numFailures++;
					System.err.println("Failed " + numFailures + " times.  Trying harder : " + url);
					//					logger.error("Failed " + numFailures + " times.  Trying harder : " + url, e);
					try {
						Thread.sleep(5000);
					} catch (final InterruptedException e1) {
					}
				}
			}
			return null;
		}
		//		URLEncoder.encode(
		final String urlString = url.getHost() + url.getPath() + (url.getQuery() == null ? "" : "?" + url.getQuery());
		//				URL proxyUrl = new URL("http", "190.129.10.245", "/cgi-bin/nph-proxy.cgi/000100A/http/" + urlString);
		//		URL proxyUrl = new URL(
		//				"http://uvirtual.dpicuto.edu.bo/cgi-bin/nph-proxy.cgi/000100A/http/arxiv.org/catchup?archive=cs&sday=25&smonth=04&syear=2006&method=without");
		String server = nphServer;
		String path = "/cgi-bin/nph-proxy.cgi";
		if (nphServer.contains("/")) {
			server = nphServer.substring(0, nphServer.indexOf('/'));
			path = nphServer.substring(nphServer.indexOf('/'));
		}
		final URL proxyUrl = new URL("http", server, path + "/000100A/http/" + urlString);
		return proxyUrl.openStream();
	}

	public static void main(final String[] args) {
		String nphServer = null;
		if (args.length > 0) {
			nphServer = args[0];
		}
		new LyricsWikiaExtractor(nphServer).run();
	}
}
