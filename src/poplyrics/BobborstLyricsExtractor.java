package poplyrics;

import java.net.URL;
import java.util.Calendar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import poplyrics.model.Artist;
import poplyrics.model.Track;

public class BobborstLyricsExtractor extends AbstractTrackExtractor {

	@Override
	protected void performWork() throws Exception {
		Calendar cal = Calendar.getInstance();
		for (int year = 1946; year <= cal.get(Calendar.YEAR); year++) {
			logger.info("Fetching year " + year);
			final URL url = new URL("http", "www.bobborst.com", "/popculture/top-100-songs-of-the-year/?year=" + year);
			final Document doc = Jsoup.parse(url, 5000);
			final Element tracksTable = doc.select(".songtable").iterator().next();
			int i = 0;
			for (final Element tr : tracksTable.select("tr")) {
				if (i >= 2) {
					final Elements tds = tr.select("td");
					final int rank = Integer.parseInt(tds.get(0).text());
					final String artistName = tds.get(1).text();
					final String trackName = tds.get(2).text();
					final Artist artist = getArtist(artistName);
					Track track = getTrack(artistName, trackName);
					if (track == null) {
						track = new Track(trackName, year, artist);
						track.setRank(rank);
						track.setSource(url.toExternalForm());
						logger.info("Inserting new Track " + track);
						getObjectContainer().store(track);
					} else {
						logger.info("Found existing Track " + track);
					}
					//					System.out.println(rank + "  " + artistName + "  " + trackName);
					getObjectContainer().commit();
				}
				i++;
			}
		}
	}

	public static void main(final String[] args) {
		new BobborstLyricsExtractor().run();
	}
}
