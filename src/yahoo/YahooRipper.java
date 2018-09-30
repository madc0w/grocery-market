package yahoo;

import grocerymarket.DatabaseTask;
import grocerymarket.NoSuchNodeException;
import grocerymarket.NodeUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class YahooRipper extends DatabaseTask {

	public static final File symbolsFile1 = new File("output", "sp500.csv");
	public static final File symbolsFile2 = new File("output", "nasdaq.csv");
	public static final File outFile = new File("output", "marketCapVsNumWorkers.csv");

	private YahooRipper() {
	}

	@Override
	protected void performWork() throws Exception {
		final FileWriter fw = new FileWriter(outFile);
		fw.write("symbol,market cap,employees\n");
		fw.flush();

		final Set<String> symbols = getSymbols();

		final XMLReader parser = new Parser();
		final Transformer transformer = TransformerFactory.newInstance().newTransformer();
		parser.setFeature(Parser.namespacesFeature, false);
		parser.setFeature(Parser.namespacePrefixesFeature, false);
		int n = 0;
		for (final String symbol : symbols) {
			n++;
			long marketCap = 0;
			int numEmployees = 0;
			{
				final URL baseUrl = new URL("http", "finance.yahoo.com", "/q/ks?s=" + symbol + "+Key+Statistics");
				final DOMResult result = new DOMResult();
				transformer.transform(new SAXSource(parser, new InputSource(baseUrl.openStream())), result);
				final Node root = result.getNode();
				boolean found = false;
				try {
					final Node table = NodeUtils.nodeAtEndOfPath(root, new int[] { 0, 1, 2, 3, 3, 1, 0, 5, 0, 0, 0 });
					for (int i = 0; i < table.getChildNodes().getLength() && !found; i++) {
						final Node tr = table.getChildNodes().item(i);
						final Node labelNode = NodeUtils.nodeAtEndOfPath(tr, new int[] { 0, 0 });
						if (labelNode != null && labelNode.getTextContent().equals("Market Cap (intraday)")) {
							found = true;
							final Node valueNode = NodeUtils.nodeAtEndOfPath(tr, new int[] { 1, 0, 0 });
							if (valueNode != null) {
								final String valueStr = valueNode.getTextContent().replaceAll(",", "");
								if (!valueStr.equals("N/A")) {
									int multiplier = 0;
									final String lastChar = valueStr.substring(valueStr.length() - 1);
									if (lastChar.equals("B")) {
										multiplier = 1000000000;
									} else if (lastChar.equals("M")) {
										multiplier = 1000000;
									} else if (lastChar.equals("K")) {
										multiplier = 1000;
									}
									try {
										if (multiplier == 0) {
											marketCap = Integer.parseInt(valueStr);
										} else {
											marketCap = (long) (Double.parseDouble(valueStr.substring(0, valueStr.length() - 1)) * multiplier);
										}
									} catch (final NumberFormatException e) {
										logger.warn("Failed to parse '" + valueStr + "'", e);
									}
								}
							}
						}
					}
				} catch (final NoSuchNodeException e) {
					logger.error("Failed to get market cap for " + symbol, e);
					System.out.println(NodeUtils.output(root));
				}
			}
			{
				final URL baseUrl = new URL("http", "finance.yahoo.com", "/q/pr?s=" + symbol + "+Profile");
				final DOMResult result = new DOMResult();
				transformer.transform(new SAXSource(parser, new InputSource(baseUrl.openStream())), result);
				final Node root = result.getNode();
				boolean found = false;
				final int[] nodeNums = new int[] { 20, 18, 16, 15, 17, 13 };
				nodeNumsLoop: for (final int nodeNum : nodeNums) {
					try {
						final Node table = NodeUtils.nodeAtEndOfPath(root, new int[] { 0, 1, 2, 3, 3, 1, 0, nodeNum, 0, 0, 0 });
						for (int i = 0; i < table.getChildNodes().getLength() && !found; i++) {
							final Node tr = table.getChildNodes().item(i);
							final Node labelNode = NodeUtils.nodeAtEndOfPath(tr, new int[] { 0, 0 });
							if (labelNode != null && labelNode.getTextContent().equals("Full Time Employees:")) {
								found = true;
								final Node valueNode = NodeUtils.nodeAtEndOfPath(tr, new int[] { 1, 0 });
								if (valueNode != null) {
									final String valueStr = valueNode.getTextContent().replaceAll(",", "");
									if (!valueStr.equals("N/A")) {
										try {
											numEmployees = Integer.parseInt(valueStr);
										} catch (final NumberFormatException e) {
											logger.warn("Failed to parse '" + valueStr + "'", e);
										}
									}
								}
								break nodeNumsLoop;
							}
						}
					} catch (final NoSuchNodeException e) {
						logger.warn("Failed to get number of employees for " + symbol + " with nodeNum " + nodeNum);
					}
				}
				if (!found) {
					logger.error("Failed to get number of employees for " + symbol);
					System.out.println(NodeUtils.output(root));
				}
			}

			if (marketCap > 0 && numEmployees > 0) {
				fw.write(symbol + "," + marketCap + "," + numEmployees + "\n");
				fw.flush();
				logger.info("Line written for " + symbol);
			} else {
				logger.info("No line written for " + symbol);
			}
			logger.info(n);
		}
		fw.close();
	}

	private Set<String> getSymbols() throws IOException {
		final Set<String> symbols = new HashSet<String>();
		{
			final FileReader fr = new FileReader(symbolsFile1);
			final LineNumberReader lnr = new LineNumberReader(fr);

			String line = lnr.readLine();
			while ((line = lnr.readLine()) != null) {
				final String[] fields = line.split(",");
				symbols.add(fields[0].replaceAll("\"", ""));
			}
		}
		{
			final FileReader fr = new FileReader(symbolsFile2);
			final LineNumberReader lnr = new LineNumberReader(fr);

			String line = lnr.readLine();
			while ((line = lnr.readLine()) != null) {
				final String[] fields = line.split("\t");
				if (fields.length > 0) {
					symbols.add(fields[0].replaceAll("\"", ""));
				}
			}
		}
		return symbols;
	}

	public static void main(final String[] args) {
		new YahooRipper().run();
	}
}
