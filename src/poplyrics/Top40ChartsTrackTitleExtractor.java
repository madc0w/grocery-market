package poplyrics;

import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.db4o.ObjectSet;

import poplyrics.model.Artist;
import poplyrics.model.Track;

public class Top40ChartsTrackTitleExtractor extends AbstractTrackExtractor {

	final ObjectSet<Track> tracks;

	Top40ChartsTrackTitleExtractor() {
		tracks = getObjectContainer().query(Track.class);
	}

	@Override
	protected void performWork() throws Exception {
		int endYear = new GregorianCalendar().get(Calendar.YEAR);
		for (int year = 1950; year <= endYear; year++) {
			logger.info("Fetching year " + year);
			final URL url = new URL("http", "www.top40charts.net", "/index.php?page=" + year + "-music-charts");
			final Document doc = Jsoup.parse(url, 2000);
			final int n = year >= endYear - 1 ? 2 : 1;
			final Element table = doc.getElementById("table_" + n);
			//			final Element table = doc.select("table[id=\"table_" + n + "\"]").first();
			final int trackNameColumn, artistColumn;
			if (year == 1950) {
				artistColumn = 2;
				trackNameColumn = 3;
			} else {
				artistColumn = 3;
				trackNameColumn = 4;
			}
			for (final Element tr : table.select("tr")) {
				int i = 0;
				int rank = 0;
				String artistName = null;
				String trackName = null;
				for (final Element td : tr.getElementsByTag("td")) {
					if (i == 0) {
						rank = Integer.parseInt(td.text());
					} else if (i == artistColumn) {
						artistName = td.text();
					} else if (i == trackNameColumn) {
						trackName = td.text();
					}
					i++;
				}

				Track track = getTrack(artistName, trackName);
				if (track == null) {
					final Artist artist = getArtist(artistName);
					track = new Track(trackName, year, artist);
					track.setRank(rank);
					track.setSource(url.toExternalForm());
					getObjectContainer().store(track);
					getObjectContainer().commit();
					logger.info("Inserted new Track " + track);
				} else {
					logger.info("Found existing Track " + track + " (rank = " + track.getRank() + " vs. " + rank + ")");
				}
			}
		}
	}
}
