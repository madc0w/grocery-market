package arxiv;

import grocerymarket.DatabaseTask;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.Format;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import util.XmlUtil;

public class ArxivApiRipper extends DatabaseTask {

	final File outDir = new File("output", "arXiv");
	final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	final DocumentBuilder builder;
	final XPathFactory xPathfactory = XPathFactory.newInstance();
	final Format sequenceFormat = new DecimalFormat("0000");

	private ArxivApiRipper() throws ParserConfigurationException {
		builder = factory.newDocumentBuilder();
	}

	@Override
	protected void performWork() throws Exception {
		String resumptionToken = null;
		int i = 0;
		do {
			resumptionToken = extractData(resumptionToken, i++);
			try {
				Thread.sleep(30 * 1000);
			} catch (final InterruptedException e1) {
			}
		} while (resumptionToken != null);
	}

	String extractData(final String resumptionToken, final int i) throws Exception {
		String url = "http://export.arxiv.org/oai2?verb=ListIdentifiers";
		if (resumptionToken == null) {
			url += "&metadataPrefix=arXiv";
		} else {
			url += "&resumptionToken=" + resumptionToken;
		}
		final Document doc = XmlUtil.tryHard(url);

		final File outFile = new File(outDir, "ListIdentifiers_" + sequenceFormat.format(i) + ".xml");
		final FileOutputStream fos = new FileOutputStream(outFile);
		XmlUtil.printDocument(doc, fos);
		fos.close();
		logger.info("Wrote " + outFile.getAbsolutePath());

		final XPath xPath = xPathfactory.newXPath();
		final Node resumptionTokenNode = (Node) xPath.compile("//resumptionToken").evaluate(doc, XPathConstants.NODE);
		if (resumptionTokenNode == null) {
			return null;
		}
		return resumptionTokenNode.getTextContent();
	}

	public static void main(final String[] args) throws Exception {
		new ArxivApiRipper().run();
	}
}
