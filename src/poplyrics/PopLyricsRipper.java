package poplyrics;

import grocerymarket.DatabaseTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import poplyrics.model.Artist;
import poplyrics.model.Track;

import com.db4o.ObjectSet;
import com.db4o.query.Predicate;

public class PopLyricsRipper extends DatabaseTask {

	private final String nphServer;
	private final Pattern bandNameAndRankPattern = Pattern.compile("\\s*-\\s+(.*)\\s+\\(\\s*#\\s*(\\d+)\\s*\\)\\s*$");

	PopLyricsRipper(final String nphServer) {
		this.nphServer = nphServer;
	}

	@Override
	protected void performWork() throws Exception {
		final XMLReader parser = new Parser();
		final Transformer transformer = TransformerFactory.newInstance().newTransformer();
		parser.setFeature(Parser.namespacesFeature, false);
		parser.setFeature(Parser.namespacePrefixesFeature, false);
		//		final XPathFactory xPathFactory = XPathFactory.newInstance();
		//		final XPath xPath = xPathFactory.newXPath();

		for (int year = 1930; year <= 2014; year++) {
			final URL url = new URL("http", "ntl.matrix.com.br", "/pfilho/html/top40/" + year + ".html");
			logger.info("Getting year " + year);
			final DOMResult result = new DOMResult();
			//			try {
			transformer.transform(new SAXSource(parser, new InputSource(getInputStream(url))), result);
			final Node root = result.getNode();
			Node item = root;
			for (final int childNum : new int[] { 0, 1, 7 }) {
				item = item.getChildNodes().item(childNum);
			}
			URL lyricsUrl = null;
			String songName = null;
			String bandNameAndRank = null;
			for (int i = 0; i < item.getChildNodes().getLength(); i++) {
				final Node songNode = item.getChildNodes().item(i);
				if (songNode.getNodeName().equals("a")) {
					songName = null;
					bandNameAndRank = null;
					final Node href = songNode.getAttributes().getNamedItem("href");
					if (href != null && href.getNodeValue().startsWith("../lyrics/")) {
						lyricsUrl = new URL(url, href.getNodeValue());
						songName = songNode.getTextContent();
					}
				} else if (songNode.getNodeName().equals("#text") && songNode.getTextContent().trim().length() > 1) {
					bandNameAndRank = songNode.getTextContent();
				}
				if (lyricsUrl != null && songName != null && bandNameAndRank != null) {
					bandNameAndRank = bandNameAndRank.replace((char) 160, ' ');
					//					try {
					final String bandName;
					final int rank;
					final Matcher matcher = bandNameAndRankPattern.matcher(bandNameAndRank);
					if (matcher.matches()) {
						bandName = matcher.group(1).trim();
						rank = Integer.parseInt(matcher.group(2));
					} else {
						if (bandNameAndRank.startsWith(" - ")) {
							bandName = bandNameAndRank.substring(3).trim();
						} else {
							bandName = bandNameAndRank.trim();
						}
						logger.warn("Failed to parse rank for '" + bandNameAndRank + "'");
						rank = 0;
					}

					final Artist artist = getArtist(bandName);
					final Track track = new Track(songName, year, artist);
					final String songNameF = songName;
					final ObjectSet<Track> tracksResult = getObjectContainer().query(new Predicate<Track>() {
						@Override
						public boolean match(final Track track) {
							return track.getArtist().equals(artist) && track.getName().equals(songNameF);
						}
					});
					if (tracksResult.isEmpty()) {
						logger.info("Fetching lyrics for " + track + " from " + lyricsUrl);
						final StringBuilder lyrics = new StringBuilder();
						final InputStream inputStream = getInputStream(lyricsUrl);
						if (inputStream == null) {
							logger.warn("Failed to fetch lyrics from " + lyricsUrl);
						} else {
							final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
							String inputLine;
							while ((inputLine = in.readLine()) != null) {
								lyrics.append(inputLine).append(" / ");
							}
							in.close();

							track.setRank(rank);
							track.setLyrics(lyrics.toString());
							getObjectContainer().store(track);
						}
					}
					getObjectContainer().commit();

					lyricsUrl = null;
					songName = null;
					bandNameAndRank = null;
				}
			}

			//			} catch (final Exception e) {
			//				logger.error("Failed to fetch contents of " + url.toExternalForm(), e);
			//			}
		}

	}

	Artist getArtist(final String name) {
		Artist artist = new Artist(name);
		final ObjectSet<Artist> result = getObjectContainer().query(new Predicate<Artist>() {
			@Override
			public boolean match(final Artist artist) {
				return artist.getName().equals(name);
			}
		});
		if (result.isEmpty()) {
			getObjectContainer().store(artist);
		} else {
			artist = result.get(0);
		}
		return artist;
	}

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
					logger.error("Failed " + numFailures + " times.  Trying harder : " + url, e);
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
		new PopLyricsRipper(nphServer).run();
	}

}
