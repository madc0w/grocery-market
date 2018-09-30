package util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XmlUtil {

	private static final Logger logger = Logger.getLogger(XmlUtil.class);

	private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private static final XPathFactory xPathfactory = XPathFactory.newInstance();
	private static DocumentBuilder builder;

	static {
		try {
			builder = factory.newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			logger.error("Failed to init builder", e);
		}
	}

	public static void printDocument(final Document doc, final OutputStream out) throws IOException, TransformerException {
		final TransformerFactory tf = TransformerFactory.newInstance();
		final Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
	}

	public static Document tryHard(final String url) throws SAXException {
		int numFailures = 0;
		boolean isSuccess = false;
		Document doc = null;
		do {
			try {
				doc = builder.parse(url);
				isSuccess = true;
			} catch (final IOException e) {
				numFailures++;
				logger.error("Failed " + numFailures + " times.  Trying harder : " + url, e);
				try {
					Thread.sleep(30 * 1000);
				} catch (final InterruptedException e1) {
				}
			}
		} while (!isSuccess);
		return doc;
	}

	public static String getTextFromNode(final Node rootNode, final String nodeName) throws XPathExpressionException {
		final Node node = (Node) xPathfactory.newXPath().compile("//" + nodeName).evaluate(rootNode, XPathConstants.NODE);
		return node == null ? null : node.getTextContent();

	}
}
