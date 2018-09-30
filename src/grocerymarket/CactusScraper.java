package grocerymarket;

import grocerymarket.model.Item;
import grocerymarket.model.Store;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import com.db4o.ObjectSet;

public class CactusScraper extends DatabaseTask {

	private CactusScraper() {
	}

	@Override
	public void performWork() throws TransformerFactoryConfigurationError, SAXNotRecognizedException, SAXNotSupportedException,
			TransformerException, IOException, XPathExpressionException {

		final XMLReader parser = new Parser();
		final Transformer transformer = TransformerFactory.newInstance().newTransformer();
		parser.setFeature(Parser.namespacesFeature, false);
		parser.setFeature(Parser.namespacePrefixesFeature, false);
		final XPathFactory xPathFactory = XPathFactory.newInstance();
		final XPath xPath = xPathFactory.newXPath();

		String[] pageNames = new String[] { //
		"Produits-surgeles", //
				"Promotions-et-Nouveautes", //
				"Le-marche-du-Frais", //
				"Epicerie", //
				"Boissons", //
				"Bebe-et-Enfants", //
				"Soins-Textiles-et-Nettoyage", //
				"Animalerie", //
				"Hygiene-et-Sante", //
				"Cuisine,-Menage-et-Maison", //
				"Epicerie-sucree", //
				"Petit-dejeuner" //
		};

		for (String pageName : pageNames) {
			logger.info("Reading page : " + pageName);
			int maxPage = 0;
			{
				final URL baseUrl = new URL("http", "cactusathome.lu", "/SHOPPING/CATEGORIES/" + pageName + ".aspx?pagesize=50");
				final DOMResult result = new DOMResult();
				transformer.transform(new SAXSource(parser, new InputSource(baseUrl.openStream())), result);
				final Node root = result.getNode();
				final NodeList elements = (NodeList) xPath.evaluate("//a[@class='UnselectedPage']", root, XPathConstants.NODESET);

				for (int i = 0; i < elements.getLength(); i++) {
					String href = elements.item(i).getAttributes().getNamedItem("href").getNodeValue();
					String pageNumStr = href.substring(href.indexOf("page=") + 5);
					maxPage = Math.max(maxPage, Integer.parseInt(pageNumStr));
				}
			}

			for (int pageNum = 1; pageNum <= maxPage; pageNum++) {
				final URL url = new URL("http", "cactusathome.lu", "/SHOPPING/CATEGORIES/" + pageName + ".aspx?pagesize=50&page=" + pageNum);
				final DOMResult result = new DOMResult();
				transformer.transform(new SAXSource(parser, new InputSource(url.openStream())), result);
				final Node root = result.getNode();
				//				System.out.println(NodeUtils.output(root));

				final Element element = (Element) xPath
						.evaluate(
								"//table[@id='plc_lt_zoneCactusContenu_SubpagePlaceholder_SubpagePlaceholder_lt_zoneSubContent_ProductList_lstElem']",
								root, XPathConstants.NODE);
				//			System.out.println(output(element));
				for (int i = 0; i < element.getChildNodes().getLength(); i++) {
					final Node trNode = element.getChildNodes().item(i);
					String itemName;
					double itemPrice;
					String unitPriceStr;
					{
						int[] path = new int[] { 0, 1, 0, 1, 0, 0 };
						itemName = NodeUtils.nodeAtEndOfPath(trNode, path).getTextContent().trim();
						//					System.out.print(itemName);
						//					System.out.print(",");
					}
					{
						int[] path = new int[] { 0, 1, 0, 3, 0, 0 };
						String itemPriceStr = NodeUtils.nodeAtEndOfPath(trNode, path).getTextContent().trim();
						itemPriceStr = itemPriceStr.replace(',', '.');
						itemPrice = Double.parseDouble(itemPriceStr.split(" ")[0]);
						//					System.out.print(itemPrice);
						//					System.out.print(",");
					}
					{
						int[] path = new int[] { 0, 1, 0, 3, 3 };
						unitPriceStr = NodeUtils.nodeAtEndOfPath(trNode, path).getTextContent().trim();
						unitPriceStr = unitPriceStr.replace(',', '.');
						unitPriceStr = unitPriceStr.replaceAll("€", "");
						//					double itemPrice = Double.parseDouble(itemPriceStr.split(" ")[0]);
						//					System.out.print(unitPriceStr);
					}
					//				System.out.println();
					Item item = new Item(itemName, itemPrice, unitPriceStr, Store.CACTUS);
					logger.info("Read " + item);
					store(item);
					getObjectContainer().commit();
				}
			}
		}
	}

	public void store(Item item) {
		Item queryItem = new Item(item.getLabel(), 0, null, item.getStore());
		ObjectSet<Item> existing = getObjectContainer().queryByExample(queryItem);
		if (existing.isEmpty()) {
			getObjectContainer().store(item);
		}
	}

	public static void main(String[] args) {
		new CactusScraper().run();
	}

}
