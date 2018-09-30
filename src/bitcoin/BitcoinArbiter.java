package bitcoin;

import grocerymarket.NodeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class BitcoinArbiter implements Runnable {

	private static final String nphServer = null;

	@Override
	public void run() {
		try {
			performWork();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void performWork() throws Exception {
		final XMLReader parser = new Parser();
		final Transformer transformer = TransformerFactory.newInstance().newTransformer();
		parser.setFeature(Parser.namespacesFeature, false);
		parser.setFeature(Parser.namespacePrefixesFeature, false);
		final XPathFactory xPathFactory = XPathFactory.newInstance();
		final XPath xPath = xPathFactory.newXPath();

		final URL url = new URL("https", "bitcoin-24.com", "market");

		final DOMResult result = new DOMResult();
		boolean success = false;
		do {
			try {
				transformer.transform(new SAXSource(parser, new InputSource(getInputStream(url))), result);
				success = true;
			} catch (final Exception e) {
				e.printStackTrace();
				Thread.sleep(100);
			}
		} while (!success);
		final Node root = result.getNode();
		System.out.println(NodeUtils.output(root));
	}

	public InputStream getInputStream(final URL url) throws IOException {
		//		numRequests++;
		//		long elapsedTime = new Date().getTime() - startTime.getTime();
		//		double requestsPerSecond = ((double) elapsedTime / 1000) / numRequests;
		//		logger.info("Mean seconds per request : " + doubleFormat.format(requestsPerSecond));
		//		return url.openStream();
		//		URLEncoder.encode(
		if (nphServer == null) {
			return url.openStream();
		}

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

		final URL proxyUrl = new URL("https", server, path + "/000100A/" + url.getProtocol() + "/" + urlString);
		try {
			Thread.sleep(5 * 1000);
		} catch (final InterruptedException e) {
		}
		return proxyUrl.openStream();
	}

	public static void main(final String[] args) {
		new BitcoinArbiter().run();
	}

}
