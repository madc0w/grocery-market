package arxiv;

import grocerymarket.DatabaseTask;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import util.XmlUtil;
import arxiv.model.Paper;
import arxiv.model.Subject;

import com.db4o.ObjectSet;
import com.db4o.query.Predicate;

public class ArxivApiPaperRipper extends DatabaseTask {

	final int startFileNum = 10;

	final File inDir = new File("output", "arXiv");
	final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	final DocumentBuilder builder;
	final XPathFactory xPathfactory = XPathFactory.newInstance();
	final Format sequenceFormat = new DecimalFormat("0000");
	final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private final ArxivService arxivService;

	private ArxivApiPaperRipper() throws ParserConfigurationException {
		this.builder = factory.newDocumentBuilder();
		this.arxivService = new ArxivService(getObjectContainer());
	}

	@Override
	protected void performWork() throws Exception {
		for (final File inFile : inDir.listFiles(new FileFilter() {

			@Override
			public boolean accept(final File file) {
				final String fileName = file.getName();
				if (!fileName.endsWith(".xml")) {
					return false;
				}
				final String n = fileName.substring(fileName.indexOf('_') + 1, fileName.indexOf('.'));
				return Integer.parseInt(n) >= startFileNum;
			}
		})) {
			logger.info("Parsing " + inFile.getAbsolutePath());
			final FileInputStream fis = new FileInputStream(inFile);
			final Document doc = builder.parse(fis);
			final XPath xPath = xPathfactory.newXPath();
			final NodeList headerNodes = (NodeList) xPath.compile("//header").evaluate(doc, XPathConstants.NODESET);
			for (int i = 0; i < headerNodes.getLength(); i++) {
				final Node headerNode = headerNodes.item(i);
				final Node setSpecNode = (Node) xPathfactory.newXPath().compile("setSpec").evaluate(headerNode, XPathConstants.NODE);
				if (setSpecNode == null) {
					logger.warn("Paper " + i + " Has no setSpec node.  Skipping.");
				} else {
					final String setSpec = setSpecNode.getTextContent();
					if (setSpec.equals("physics") || setSpec.startsWith("physics:")) {
						final Node idNode = (Node) xPathfactory.newXPath().compile("identifier").evaluate(headerNode, XPathConstants.NODE);
						final String id = idNode.getTextContent().substring(idNode.getTextContent().lastIndexOf(':') + 1);
						final URL pdfUrl = new URL("http", "arxiv.org", "/pdf/" + id);
						Paper paper = getPaper(pdfUrl);
						if (paper == null) {
							Thread.sleep(3000);

							final String url = "http://export.arxiv.org/oai2?verb=GetRecord&identifier=oai:arXiv.org:" + id
									+ "&metadataPrefix=arXiv";
							final Document paperDoc = XmlUtil.tryHard(url);
							final String title = XmlUtil.getTextFromNode(paperDoc, "title");
							final String abstractText = XmlUtil.getTextFromNode(paperDoc, "abstract");
							final Date submissionDate = dateFormat.parse(XmlUtil.getTextFromNode(paperDoc, "created"));
							paper = new Paper(title, abstractText, pdfUrl.toExternalForm(), null, submissionDate, 0, 0, null);
							getObjectContainer().store(paper);

							final Subject subject = arxivService.getSubject(setSpec.replace(':', '.'), null);
							paper.getSubjects().add(subject);
							getObjectContainer().store(paper.getSubjects());

							logger.info("committing " + id);
							getObjectContainer().commit();
						} else {
							logger.info("Paper " + pdfUrl.toExternalForm() + " already in database.  Skipping.");
						}
					}
				}
			}
		}
	}

	private Paper getPaper(final URL pdfUrl) {
		final String pdfUrlStr = pdfUrl.toExternalForm();
		final ObjectSet<Paper> result = getObjectContainer().query(new Predicate<Paper>() {

			@Override
			public boolean match(final Paper paper) {
				return paper.getPdfUrl().equals(pdfUrlStr);
			}
		});
		if (result.size() > 1) {
			throw new IllegalStateException(result.size() + " Papers found having pdfUrl " + pdfUrl);
		}
		return result.isEmpty() ? null : result.get(0);
	}

	public static void main(final String[] args) throws ParserConfigurationException {
		new ArxivApiPaperRipper().run();
	}
}
