package arxiv;

import grocerymarket.DatabaseTask;
import grocerymarket.NoSuchNodeException;
import grocerymarket.NodeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import arxiv.model.Author;
import arxiv.model.Institution;
import arxiv.model.Paper;

import com.db4o.ObjectSet;
import com.db4o.query.Predicate;

public class ArxivRipper extends DatabaseTask {

	private static final Pattern subjectsPattern = Pattern.compile("(.*) \\((.*)\\)$");
	private static final Pattern commentsPattern = Pattern.compile("(\\d+) pages, (\\d+) figures, (.*)");
	private static final DateFormat submissionDateFormat = new SimpleDateFormat("dd MMM yyyy");
	private static final DateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy");
	private final String nphServer;
	private final Date startDate, startTime;
	private int numRequests;
	private final ArxivService arxivService;

	public ArxivRipper(final String nphServer, final Date startDate) {
		this.startTime = new Date();
		this.nphServer = nphServer;
		this.startDate = startDate;
		this.arxivService = new ArxivService(getObjectContainer());
	}

	@Override
	protected void performWork() throws Exception {
		final XMLReader parser = new Parser();
		final Transformer transformer = TransformerFactory.newInstance().newTransformer();
		parser.setFeature(Parser.namespacesFeature, false);
		parser.setFeature(Parser.namespacePrefixesFeature, false);
		final XPathFactory xPathFactory = XPathFactory.newInstance();
		final XPath xPath = xPathFactory.newXPath();

		final LinkedHashMap<Date, URL> baseUrls = new LinkedHashMap<Date, URL>();
		//		String[] groups = new String[] { "cs", "math", "nlin", "physics", "q-bio", "q-fin", "stat" };
		final String[] groups = new String[] { "physics" };
		final Date today = new Date();
		for (final String group : groups) {
			//			Calendar cal = Calendar.getInstance();
			//			cal.set(Calendar.YEAR, yearStart);
			//			cal.set(Calendar.MONTH, Calendar.JANUARY);
			//			cal.set(Calendar.DAY_OF_MONTH, 1);
			final DateFormat catchupDateFormat = new SimpleDateFormat("'&sday='dd'&smonth='MM'&syear='yyyy");
			Date date = new Date(startDate.getTime());
			final Calendar cal = Calendar.getInstance();
			while (!date.after(today)) {
				final String formattedDate = catchupDateFormat.format(date);
				final URL url = new URL("http", "arxiv.org", "/catchup?archive=" + group + formattedDate + "&method=without");
				baseUrls.put(date, url);

				cal.setTime(date);
				cal.add(Calendar.DAY_OF_YEAR, 1);
				date = cal.getTime();
			}
		}

		//			final URL baseUrl = new URL("http", "arxiv.org", "/list/astro-ph/pastweek?show=99999");
		for (final Entry<Date, URL> baseUrl : baseUrls.entrySet()) {
			logger.info("Getting results from " + baseUrl.getValue().toExternalForm());
			logger.info("***  Date : " + displayDateFormat.format(baseUrl.getKey()) + "  proxy : " + nphServer);

			final DOMResult result = new DOMResult();
			try {
				transformer.transform(new SAXSource(parser, new InputSource(getInputStream(baseUrl.getValue()))), result);
			} catch (final Exception e) {
				logger.error("Failed to fetch contents of " + baseUrl.getValue().toExternalForm(), e);
				continue;
			}
			final Node root = result.getNode();
			//			System.out.println(NodeUtils.output(root));

			final Collection<Node> pdfUrlNodes = new ArrayList<Node>();
			final Node subRootNode = root; // NodeUtils.nodeAtEndOfPath(root, new int[] { 0, 1, 5, 1 });
			final NodeList aNodes = (NodeList) xPath.evaluate("//a", subRootNode, XPathConstants.NODESET);
			for (int i = 0; i < aNodes.getLength(); i++) {
				final Node aNode = aNodes.item(i);
				final Node hrefAttribute = aNode.getAttributes().getNamedItem("href");
				if (hrefAttribute != null) {
					final String aHref = hrefAttribute.getNodeValue();
					if (aHref.contains("/pdf/")) {
						pdfUrlNodes.add(aNode);
					}
				}
			}

			for (final Node pdfUrlNode : pdfUrlNodes) {
				String aHref = pdfUrlNode.getAttributes().getNamedItem("href").getNodeValue();
				aHref = aHref.substring(aHref.indexOf("/pdf/"));
				final URL pdfUrl = new URL("http", "arxiv.org", aHref);
				final int[] pathToNode = NodeUtils.pathToNode(subRootNode, pdfUrlNode);

				Paper paper = getPaper(pdfUrl);
				if (paper == null) {
					final int[] truncatedPath = NodeUtils.truncatePathPlusAdditionalPath(pathToNode, 1, new int[] { 0 });
					final Node paperUrlText = NodeUtils.nodeAtEndOfPath(subRootNode, truncatedPath);
					URL paperUrl;
					if (paperUrlText.getAttributes() != null && paperUrlText.getAttributes().getNamedItem("href") != null) {
						String href = paperUrlText.getAttributes().getNamedItem("href").getTextContent();
						if (href.indexOf("arxiv.org") != -1) {
							href = href.substring(href.indexOf("arxiv.org") + "arxiv.org".length());
						}
						paperUrl = new URL("http", "arxiv.org", href);
						final DOMResult paperResult = new DOMResult();
						transformer.transform(new SAXSource(parser, new InputSource(getInputStream(paperUrl))), paperResult);
						final Node paperRoot = paperResult.getNode();
						//								// TODO remove after done testing!
						//								//						paperUrl = new URL("http://arxiv.org/abs/1203.1842");

						int n = 0;
						try {
							NodeUtils.nodeAtEndOfPath(paperRoot, new int[] { 0, 1, 5, 3, 4 });
						} catch (final NoSuchNodeException e) {
							n = 1;
						}

						String submissionDateText;
						try {
							submissionDateText = NodeUtils.nodeAtEndOfPath(paperRoot, new int[] { n, 1, 5, 3, 4, 7, 0 }).getTextContent();
						} catch (final NoSuchNodeException e) {
							logger.error("Failed to get submissionDateText from:", e);
							logger.error(NodeUtils.output(paperRoot));
							continue;
						}
						submissionDateText = submissionDateText.substring("(Submitted on ".length(), submissionDateText.length() - 1);
						final Date submissionDate = submissionDateFormat.parse(submissionDateText);
						String abstractText = NodeUtils.nodeAtEndOfPath(paperRoot, new int[] { n, 1, 5, 3, 4, 9, 2 }).getTextContent();
						abstractText = abstractText.replaceAll("\\n", " ");

						final String commentsText = NodeUtils.nodeAtEndOfPath(paperRoot, new int[] { n, 1, 5, 3, 4, 13, 1, 0, 1, 0 })
								.getTextContent();
						int numPages = -1;
						int numFigures = -1;
						String comments = null;
						{
							final Matcher matcher = commentsPattern.matcher(commentsText);
							if (matcher.matches()) {
								numPages = Integer.parseInt(matcher.group(1));
								numFigures = Integer.parseInt(matcher.group(2));
								comments = matcher.group(3);
							} else {
								comments = commentsText;
							}
						}

						//					System.out.println(NodeUtils.output(paperRoot));
						String title = null;
						final NodeList metas = NodeUtils.nodeAtEndOfPath(paperRoot, new int[] { n, 0 }).getChildNodes();
						final NodeList authorNodes = NodeUtils.nodeAtEndOfPath(paperRoot, new int[] { n, 1, 5, 3, 4, 5 }).getChildNodes();
						final List<Author> authors = new ArrayList<Author>();
						for (int k = 0; k < authorNodes.getLength(); k++) {
							final Node node = authorNodes.item(k);
							if (node.getNodeName().equals("a")) {
								href = node.getAttributes().getNamedItem("href").getNodeValue();
								href = href.substring(href.indexOf("arxiv.org") + "arxiv.org".length());
								final URL url = new URL("http", "arxiv.org", href);
								final String name = node.getChildNodes().item(0).getNodeValue();
								final Author author = getAuthor(name, url);
								authors.add(author);
							}
						}

						for (int k = 0; k < metas.getLength(); k++) {
							final Node meta = metas.item(k);
							final Node nameNode = meta.getAttributes().getNamedItem("name");
							if (nameNode != null) {
								if (nameNode.getNodeValue().equals("citation_title")) {
									title = meta.getAttributes().getNamedItem("content").getNodeValue();
								}
							}
						}

						logger.info("Creating new Paper from " + paperUrl + " with title '" + title + "'");
						paper = new Paper(title, abstractText, pdfUrl.toExternalForm(), paperUrl.toExternalForm(), submissionDate,
								numPages, numFigures, comments);

						paper.getAuthors().addAll(authors);
						//					Node subjectsNode = NodeUtils.nodeAtEndOfPath(paperRoot, new int[] { n, 1, 5, 3, 4, 13, 1, 1, 1 });
						final Node subjectsNode = ((NodeList) xPath.evaluate("//td[@class='tablecell subjects']", paperRoot,
								XPathConstants.NODESET)).item(0);
						for (final String subjectText : subjectsNode.getTextContent().split(";")) {
							final Matcher matcher = subjectsPattern.matcher(subjectText);
							if (matcher.matches()) {
								final String name = matcher.group(1);
								final String code = matcher.group(2);
								paper.getSubjects().add(arxivService.getSubject(code, name));
							}
						}
						if (getPaper(pdfUrl) == null) {
							// TODO
							// CitedBy : http://ftp.equilibrium-wow.de/cgi-bin/nph-proxy.cgi/40/http/arxiv.org/cits/1203.4847
							// RefersTo : http://ftp.equilibrium-wow.de/cgi-bin/nph-proxy.cgi/40/http/arxiv.org/refs/1203.4847
							//						paper.getCitedBy().add(e);
							//						paper.getRefersTo().add(e);
							getObjectContainer().store(paper.getSubjects());
							getObjectContainer().store(paper.getAuthors());
							getObjectContainer().store(paper.getRefersTo());
							getObjectContainer().store(paper.getCitedBy());
							getObjectContainer().store(paper);
							logger.info("committing");
							getObjectContainer().commit();
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

	private Institution getInstitution(final String name) {
		final ObjectSet<Institution> result = getObjectContainer().query(new Predicate<Institution>() {

			@Override
			public boolean match(final Institution institution) {
				return institution.getName() == name || institution.getName().equals(name);
			}
		});
		if (result.size() > 1) {
			throw new IllegalStateException(result.size() + " Institutions found having name " + name);
		}
		Institution institution;
		if (result.isEmpty()) {
			institution = new Institution(name);
			getObjectContainer().store(institution);
		} else {
			institution = result.get(0);
		}
		return institution;
	}

	private Author getAuthor(final String name, final URL url) {
		final String urlStr = url.toExternalForm();
		final ObjectSet<Author> result = getObjectContainer().query(new Predicate<Author>() {
			@Override
			public boolean match(final Author author) {
				//				return author.getName().equals(name) || author.getUrl().equals(urlStr);
				return author.getUrl().equals(urlStr);
			}
		});
		if (result.size() > 1) {
			throw new IllegalStateException(result.size() + " Authors found having name " + name);
		}
		Author author;
		if (result.isEmpty()) {
			logger.info("Creating Author " + name);
			author = new Author(name, urlStr);
			getObjectContainer().store(author);
		} else {
			logger.info("Found Author " + name);
			author = result.get(0);
			if (!author.getName().equals(name)) {
				author.getOtherNames().add(name);
				getObjectContainer().store(author.getOtherNames());
				getObjectContainer().store(author);
			}
		}
		return author;
	}

	private static final NumberFormat doubleFormat = new DecimalFormat("0.000");

	public InputStream getInputStream(final URL url) throws IOException {
		numRequests++;
		//		try {
		//			Thread.sleep((int) ((5 + Math.random() * 5) * 1000));
		//		} catch (final InterruptedException e) {
		//		}
		final long elapsedTime = new Date().getTime() - startTime.getTime();
		final double requestsPerSecond = (double) elapsedTime / 1000 / numRequests;
		logger.info("Mean seconds per request : " + doubleFormat.format(requestsPerSecond));
		if (nphServer == null) {
			int numFailures = 0;
			while (true) {
				try {
					return url.openStream();
				} catch (final IOException e) {
					numFailures++;
					logger.error("Failed " + numFailures + " times.  Trying harder : " + url, e);
					try {
						Thread.sleep(5000);
					} catch (final InterruptedException e1) {
					}
				}
			}
		}
		//		URLEncoder.encode(
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
		final URL proxyUrl = new URL("http", server, path + "/000100A/http/" + urlString);
		return proxyUrl.openStream();
	}

	public static void main(final String[] args) throws ParseException {
		String nphServer = null;
		if (args.length > 0) {
			nphServer = args[0];
		}
		Date startDate = displayDateFormat.parse("01/01/2010");
		if (args.length > 1) {
			if (args[1].contains("/")) {
				startDate = displayDateFormat.parse(args[1]);
			} else {
				startDate = displayDateFormat.parse("01/01/" + args[1]);
			}
		}
		final ArxivRipper arxivRipper = new ArxivRipper(nphServer, startDate);
		arxivRipper.run();
	}

}
