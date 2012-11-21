package org.webseer.web;

import java.net.URI;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.webseer.java.FunctionDef;
import org.webseer.java.InputChannel;
import org.webseer.java.OutputChannel;

@FunctionDef(description = "Extracts text from an HTML document", keywords = { "extract", "text", "html" })
public class ExtractText extends HTMLParsingFunction {

	@InputChannel
	DocumentView htmlDoc;

	@OutputChannel
	String strippedText;

	@OutputChannel
	String strippedLocation;

	@OutputChannel
	String strippedEmphasis;

	@OutputChannel
	String strippedHeading;

	@OutputChannel
	String strippedLink;

	@OutputChannel
	String strippedTitle;

	@Override
	public void execute() throws Throwable {
		URI location = URI.create(htmlDoc.url);

		StringBuffer tokenizedURL = new StringBuffer();

		String host = location.getHost();
		if (host != null) {
			String[] hostParts = host.split("\\.");
			for (String hostPart : hostParts) {
				if (tokenizedURL.length() > 0) {
					tokenizedURL.append(' ');
				}
				tokenizedURL.append(hostPart);
			}
		}

		String path = location.getPath();
		if (path != null) {
			String[] pathParts = path.split("\\/");
			for (String pathPart : pathParts) {
				if (tokenizedURL.length() > 0) {
					tokenizedURL.append(' ');
				}
				tokenizedURL.append(pathPart);
			}
		}

		String query = location.getQuery();
		if (query != null) {
			String[] queryParts = query.split("\\&");
			for (String queryPart : queryParts) {
				String[] pair = queryPart.split("\\=");
				if (pair.length == 2) {
					if (tokenizedURL.length() > 0) {
						tokenizedURL.append(' ');
					}
					tokenizedURL.append(pair[0]);
					tokenizedURL.append(' ');
					tokenizedURL.append(pair[1]);
				}
			}
		}

		String hash = location.getFragment();
		if (hash != null) {
			if (tokenizedURL.length() > 0) {
				tokenizedURL.append(' ');
			}
			tokenizedURL.append(hash);
		}

		strippedLocation = tokenizedURL.toString();

		// Now do the parse
		Document doc = getDocument(htmlDoc);

		Node currentNode = doc.getDocumentElement();

		ParseContext context = new ParseContext();

		parseDocument(currentNode, context);

		strippedText = context.currentLine.toString();
		strippedEmphasis = context.emphasized.toString();
		strippedHeading = context.heading.toString();
		strippedTitle = context.title.toString();
		strippedLink = context.link.toString();
	}

	private void parseDocument(Node currentNode, ParseContext context) {
		switch (currentNode.getNodeType()) {
		case Node.ELEMENT_NODE:
			Element asElement = (Element) currentNode;
			if (isTitleTag(asElement)) {
				context.inTitleCount++;
			}
			if (isLinkType(asElement)) {
				context.inLinkCount++;
			}
			if (isHeading(asElement)) {
				context.inHeadingCount++;
			}
			if (isEmphasized(asElement)) {
				context.inEmphasizedCount++;
			}
			if (isLineBreakTag(asElement) && context.currentLine.length() > 0
					&& context.currentLine.charAt(context.currentLine.length() - 1) != '\n') {
				context.currentLine.append(System.getProperty("line.separator"));
			}
			break;
		case Node.TEXT_NODE:
			Text asText = (Text) currentNode;
			String text = asText.getData();

			if (context.inTitleCount > 0) {
				// text = Translate.decode(text);
				collapseInto(context.title, text);
			}
			if (isDisplayed(asText)) {
				if (!isInPre(asText)) {
					// text = Translate.decode(text);
					if (context.inLinkCount > 0) {
						collapseInto(context.link, text);
					}
					if (context.inHeadingCount > 0) {
						collapseInto(context.heading, text);
					}
					if (context.inEmphasizedCount > 0) {
						collapseInto(context.emphasized, text);
					}
					collapseInto(context.currentLine, text);
				} else {
					context.currentLine.append(text);
				}
			}
		}
		for (int i = 0; i < currentNode.getChildNodes().getLength(); i++) {
			parseDocument(currentNode.getChildNodes().item(i), context);
		}
		switch (currentNode.getNodeType()) {
		case Node.ELEMENT_NODE:
			Element asElement = (Element) currentNode;
			if (isTitleTag(asElement)) {
				context.inTitleCount--;
			}
			if (isLinkType(asElement)) {
				context.inLinkCount--;
			}
			if (isHeading(asElement)) {
				context.inHeadingCount--;
			}
			if (isEmphasized(asElement)) {
				context.inEmphasizedCount--;
			}
		}
	}

	private static class ParseContext {

		final StringBuilder currentLine = new StringBuilder();
		final StringBuilder emphasized = new StringBuilder();
		final StringBuilder heading = new StringBuilder();
		final StringBuilder title = new StringBuilder();
		final StringBuilder link = new StringBuilder();

		int inTitleCount = 0;
		int inEmphasizedCount = 0;
		int inHeadingCount = 0;
		int inLinkCount = 0;

	}

	private boolean isEmphasized(Element tag) {
		if (isEmphasisType(tag) || isHeadingTag(tag)) {
			return true;
		}
		if (isFontTag(tag) && tag.hasAttribute("size")) {
			String sizeString = tag.getAttribute("size");
			if (sizeString.startsWith("+")) {
				return true;
			} else {
				try {
					int size = Integer.parseInt(sizeString);
					if (size > 3) {
						return true;
					}
				} catch (NumberFormatException e) {
					// Ignore and trickle
				}
			}
		}
		return false;
	}

	private boolean isHeading(Element tag) {
		if (isH1Tag(tag) || isH2Tag(tag)) {
			return true;
		}
		if (isFontTag(tag) && tag.hasAttribute("size")) {
			String sizeString = tag.getAttribute("size");
			if (sizeString.startsWith("+")) {
				try {
					int size = Integer.parseInt(sizeString.substring(1));
					if (size > 1) {
						return true;
					}
				} catch (NumberFormatException e) {
					// Ignore and trickle
				}
			} else {
				try {
					int size = Integer.parseInt(sizeString);
					if (size > 4) {
						return true;
					}
				} catch (NumberFormatException e) {
					// Ignore and trickle
				}
			}
		}
		return false;
	}

	public static boolean isDisplayed(Text string) {
		if (string.getData().trim().length() == 0) {
			return false;
		}
		Node parent = (Element) string.getParentNode();
		String tagName = parent.getNodeName();
		while (parent != null) {
			if (tagName.equalsIgnoreCase("SCRIPT") || tagName.equalsIgnoreCase("STYLE")
					|| tagName.equalsIgnoreCase("OPTION")) {
				return false;
			}
			parent = parent.getParentNode();
		}
		return true;
	}

	private static void collapseInto(StringBuilder currentLine, String toCollapse) {
		String newline = System.getProperty("line.separator");

		int chars;
		int length;
		int state;
		char character;

		chars = toCollapse.length();
		if (0 != chars) {
			length = currentLine.length();
			state = 0 == length || currentLine.charAt(length - 1) == ' ' || newline.length() <= length
					&& currentLine.substring(length - newline.length(), length).equals(newline) ? 0 : 1;
			for (int i = 0; i < chars; i++) {
				character = toCollapse.charAt(i);
				switch (character) {
				// see HTML specification section 9.1 White space
				// http://www.w3.org/TR/html4/struct/text.html#h-9.1
				case '\u0000':
				case '\u0009':
				case '\u000C':
				case '\u000B':
				case '\u200B':
				case '\u0020':
				case '\r':
				case '\n':
					if (0 != state) {
						state = 1;
					}
					break;
				default:
					if (1 == state) {
						currentLine.append(' ');
					}
					state = 2;
					currentLine.append(character);
				}
			}
		}
	}

	private static boolean isInPre(Text string) {
		Node parent = (Node) string.getParentNode();
		if (parent.getNodeName().equalsIgnoreCase("PRE")) {
			return true;
		}
		return false;
	}
}
