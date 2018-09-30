package grocerymarket;

import java.net.URL;
import java.net.URLConnection;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class AuchanScraper extends DatabaseTask {

	private AuchanScraper() {
	}

	@Override
	protected void performWork() throws Exception {
		final XMLReader parser = new Parser();
		final Transformer transformer = TransformerFactory.newInstance().newTransformer();
		parser.setFeature(Parser.namespacesFeature, true);
		parser.setFeature(Parser.namespacePrefixesFeature, false);
		//		parser.setFeature(Parser.xmlnsURIsFeature, false);
		//		final XPathFactory xPathFactory = XPathFactory.newInstance();
		//		final XPath xPath = xPathFactory.newXPath();

		final URL baseUrl = new URL("http", "www.auchandrive.lu", "/magasin/magasin.jsp?magIdProd=3");
		URLConnection baseConnection = baseUrl.openConnection();
		String cookie = baseConnection.getHeaderField("Set-Cookie");
		final DOMResult result = new DOMResult();
		transformer.transform(new SAXSource(parser, new InputSource(baseConnection.getInputStream())), result);
		final Node root = result.getNode();
		Node ulNode = NodeUtils.nodeAtEndOfPath(root, new int[] { 0, 1, 5, 1, 1, 1, 9, 1 });
		for (int i = 0; i < ulNode.getChildNodes().getLength(); i++) {
			Node aNode = NodeUtils.nodeAtEndOfPath(ulNode, new int[] { i, 0 });
			String href = aNode.getAttributes().getNamedItem("href").getNodeValue();

			final URL url = new URL("http", "www.auchandrive.lu", href);
			logger.info("Loading " + url);
			final DOMResult pageResult = new DOMResult();
			URLConnection connection = url.openConnection();
			connection.setRequestProperty("Cookie", cookie);
			transformer.transform(new SAXSource(parser, new InputSource(connection.getInputStream())), pageResult);
			final Node pageRoot = pageResult.getNode();
			//			System.out.println(NodeUtils.output(pageRoot));
			Node element = NodeUtils.nodeAtEndOfPath(pageRoot, new int[] { 0, 1, 1, 1, 7, 1, 1, 1 });
			boolean isFinished = false;
			for (int j = 0; !isFinished; j++) {
				try {
					Node child = NodeUtils.nodeAtEndOfPath(element, new int[] { (2 * (j + 1)) + 1, 5, 1, 3, 0, 0 });
					System.out.println(child.getNodeValue());
				} catch (NoSuchNodeException e) {
					isFinished = true;
				}
			}

			//			for (int j = 0; j < element.getChildNodes().getLength(); i++) {
			//				Node child = element.getChildNodes().item(j);
			//				System.out.println(NodeUtils.output(child));
			//			}
		}
	}

	public static void main(String[] args) {
		new AuchanScraper().run();
	}
}
