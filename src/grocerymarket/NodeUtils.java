package grocerymarket;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

public class NodeUtils {
	private static final Logger logger = Logger.getLogger(NodeUtils.class);

	private NodeUtils() {
	}

	public static int[] truncatePath(int[] path, int depth) {
		return truncatePathPlusAdditionalPath(path, depth, new int[] {});
	}

	public static int[] truncatePathPlusAdditionalPath(int[] path, int depth, int[] additionalPath) {
		int[] ret = new int[path.length - depth + additionalPath.length];
		int i = 0;
		for (int p : path) {
			if (i == path.length - depth) {
				break;
			}
			ret[i++] = p;
		}
		for (int p : additionalPath) {
			ret[i++] = p;
		}

		return ret;
	}

	public static int[] pathToNode(Node root, Node nodeToFind) {
		Path path = new Path();
		Path pathToNode = pathToNode(root, nodeToFind, path);
		int[] ret = new int[pathToNode.path.size()];
		int i = 0;
		for (int p : pathToNode.path) {
			ret[i++] = p;
		}
		return ret;
	}

	public static Node nodeAtEndOfPath(Node root, int[] path) {
		Node node = root;
		StringBuffer traversedPath = new StringBuffer();
		for (final int i : path) {
			if (node == null) {
				throw new NoSuchNodeException("Failed to find the path " + traversedPath + " under " + root);
			}
			node = node.getChildNodes().item(i);
			if (traversedPath.length() > 0) {
				traversedPath.append(", ");
			}
			traversedPath.append(i);
		}
		return node;
	}

	public static String output(Node node) {
		return output(node, new ArrayList<Integer>()).toString();
	}

	private static StringBuffer output(Node node, List<Integer> path) {
		StringBuffer buf = new StringBuffer();
		if (node == null) {
			logger.error("Passed node is null");
			buf.append("null");
		} else {
			boolean isFirst = true;
			for (int i : path) {
				if (!isFirst) {
					buf.append(", ");
				}
				isFirst = false;
				buf.append(i);
			}
			buf.append(":\t");

			buf.append(node.getNodeName()).append(" ");
			if (node.getNodeType() == Node.TEXT_NODE) {
				buf.append(node.getTextContent().trim());
			} else if (node.getAttributes() != null) {
				for (int i = 0; i < node.getAttributes().getLength(); i++) {
					Node attr = node.getAttributes().item(i);
					buf.append(attr.getNodeName()).append("=").append(attr.getNodeValue()).append(" ");
				}
			}
			buf.append("\n");
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				path.add(i);
				buf.append(output(node.getChildNodes().item(i), path));
				path.remove(path.size() - 1);
			}
		}
		return buf;
	}

	private static Path pathToNode(Node root, Node nodeToFind, Path path) {
		if (root == null) {
			logger.error("Passed node is null");
		} else if (root == nodeToFind || path.isComplete) {
			path.isComplete = true;
		} else {
			for (int i = 0; i < root.getChildNodes().getLength() && !path.isComplete; i++) {
				path.path.add(i);
				Node child = root.getChildNodes().item(i);
				pathToNode(child, nodeToFind, path);
				if (!path.isComplete) {
					path.path.remove(path.path.size() - 1);
				}
			}
		}
		return path;
	}

	private static class Path {
		private final List<Integer> path = new ArrayList<Integer>();
		boolean isComplete;
	}

	public static void main(String[] args) {
		int[] truncatePathPlusAdditionalPath = truncatePathPlusAdditionalPath(new int[] { 1, 2, 3, 4, 5 }, 2, new int[] { 44, 55, 66, 77 });
		System.out.println(truncatePathPlusAdditionalPath);
	}
}
