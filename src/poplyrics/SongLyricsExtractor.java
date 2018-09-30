package poplyrics;

import grocerymarket.DatabaseTask;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import poplyrics.model.Track;

import com.db4o.ObjectSet;

public class SongLyricsExtractor extends DatabaseTask {

	@Override
	protected void performWork() throws Exception {
		final ObjectSet<Track> tracks = getObjectContainer().query(Track.class);
		for (final Track track : tracks) {
			if (track.getLyrics() == null) {
				final String query = URLEncoder.encode(track.getArtist() + " - " + track.getName(), "UTF-8");
				final URL url = new URL("http", "www.songlyrics.com", "/index.php?section=search&searchW=" + query + "&submit=Search");
				final Document doc = Jsoup.parse(url, 15000);
				final Elements h3 = doc.getElementsByTag("h3");
				if (h3.size() >= 2) {
					final Element link = h3.get(1);
					final String trackNameToMatch = track.getName().toLowerCase().trim().replaceAll("[,\\!\\.]", "").replaceAll("-", " ");
					final String linkNameToMatch = link.text().toLowerCase().trim().replaceAll("[,\\!\\.]", "").replaceAll("-", " ");
					if (linkNameToMatch.contains(trackNameToMatch)) {
						final String href = link.getElementsByTag("a").iterator().next().attributes().get("href");
						logger.info("Fetching " + href);
						final URL lyricsUrl = new URL(href);
						boolean isSuccess = false;
						try {
							do {
								try {
									final Document lyricsDoc = Jsoup.parse(lyricsUrl, 5000);
									isSuccess = true;
									final Element songLyricsDiv = lyricsDoc.getElementById("songLyricsDiv");
									final String lyrics = songLyricsDiv.text().replaceAll("\\(.*?\\)", "");
									track.setLyrics(lyrics);
									LyricsNormalizer.normalizeLyrics(track);
									getObjectContainer().store(track);
									getObjectContainer().commit();
								} catch (final SocketTimeoutException e) {
									logger.warn("Timed out fetching " + url + " ... will try harder");
								}
							} while (!isSuccess);
						} catch (final IOException e) {
							logger.error("Failed to fetch " + href, e);
						}
					} else {
						logger.warn("Result '" + link.text() + "' did not match for " + track);
					}
				} else {
					logger.warn("No results for " + track);
				}
			}
		}
	}

	public static void main(final String[] args) {
		new SongLyricsExtractor().run();
	}
}
