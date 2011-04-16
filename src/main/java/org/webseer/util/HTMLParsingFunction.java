package org.webseer.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.tidy.Configuration;
import org.w3c.tidy.Tidy;
import org.webseer.transformation.JavaFunction;
import org.webseer.web.DocumentView;

public abstract class HTMLParsingFunction implements JavaFunction {
	private static final Set<String> inlineTags = new HashSet<String>();

	private static final Set<String> breakTags = new HashSet<String>();

	static {
		inlineTags.add("FONT");
		inlineTags.add("NOBR");
		inlineTags.add("U");
		inlineTags.add("I");
		inlineTags.add("A");
		inlineTags.add("B");
		inlineTags.add("SPAN");
		inlineTags.add("IMG");
		inlineTags.add("INPUT");
		inlineTags.add("SELECT");
		inlineTags.add("CITE");
		inlineTags.add("BIG");
		inlineTags.add("BR");
		inlineTags.add("INPUT");
		inlineTags.add("OPTION");

		breakTags.add("BLOCKQUOTE");
		breakTags.add("BODY");
		breakTags.add("BR");
		breakTags.add("CENTER");
		breakTags.add("DD");
		breakTags.add("DIR");
		breakTags.add("DIV");
		breakTags.add("DL");
		breakTags.add("DT");
		breakTags.add("FORM");
		breakTags.add("H1");
		breakTags.add("H2");
		breakTags.add("H3");
		breakTags.add("H4");
		breakTags.add("H5");
		breakTags.add("H6");
		breakTags.add("HEAD");
		breakTags.add("HR");
		breakTags.add("HTML");
		breakTags.add("ISINDEX");
		breakTags.add("LI");
		breakTags.add("MENU");
		breakTags.add("NOFRAMES");
		breakTags.add("OL");
		breakTags.add("P");
		breakTags.add("PRE");
		breakTags.add("TD");
		breakTags.add("TH");
		breakTags.add("TITLE");
		breakTags.add("UL");
	}

	public Document getDocument(DocumentView view) throws IOException {
		ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(view.docData.toByteArray()));

		ZipEntry entry;
		Document document = null;
		while ((entry = zipStream.getNextEntry()) != null) {
			if (entry.getName().equals("file.html")) {
				document = getDocument(zipStream);
				break;
			}
		}

		zipStream.close();

		return document;
	}

	public Document getDocument(InputStream stream) {
		Tidy tidier = new Tidy();
		tidier.setErrout(new PrintWriter(new StringWriter()));
		tidier.setQuiet(false);
		tidier.setShowWarnings(true);
		tidier.setTidyMark(false);
		tidier.setCharEncoding(Configuration.UTF8);
		// tidier.setXHTML(true);

		Document doc = tidier.parseDOM(stream, null);
		return doc;
	}

	public static String getDomPath(Node tag) {
		Stack<Node> tagStack = new Stack<Node>();
		while (tag != null) {
			tagStack.push(tag);
			tag = tag.getParentNode();
		}
		StringBuilder domPath = new StringBuilder();
		while (!tagStack.isEmpty()) {
			domPath.append(tagStack.pop().getNodeName().toUpperCase());
			if (!tagStack.isEmpty()) {
				domPath.append("/");
			}
		}
		return domPath.toString();
	}

	public static String getLinkEvent(Element tag) {
		if (isAHrefTag(tag)) {
			try {
				URI hrefuri = new URI(tag.getAttribute("href"));
				if (hrefuri.getScheme() != null && hrefuri.getScheme().equalsIgnoreCase("javascript")) {
					return hrefuri.getSchemeSpecificPart();
				}
			} catch (URISyntaxException e) {
			}
		}
		return null;
	}

	public static String getOnBlurEvent(Element tag) {
		return tag.getAttribute("onBlur");
	}

	public static String getOnChangeEvent(Element tag) {
		return tag.getAttribute("onChange");
	}

	public static String getOnClickEvent(Element tag) {
		return tag.getAttribute("onClick");
	}

	public static String getOnDoubleClickEvent(Element tag) {
		return tag.getAttribute("onDblClick");
	}

	public static String getOnFocusEvent(Element tag) {
		return tag.getAttribute("onFocus");
	}

	public static String getOnKeyDownEvent(Element tag) {
		return tag.getAttribute("onKeyDown");
	}

	public static String getOnKeyUpEvent(Element tag) {
		return tag.getAttribute("onKeyUp");
	}

	public static String getOnMouseDownEvent(Element tag) {
		return tag.getAttribute("onMouseDown");
	}

	public static String getOnMouseMoveEvent(Element tag) {
		return tag.getAttribute("onMouseMove");
	}

	public static String getOnMouseOutEvent(Element tag) {
		return tag.getAttribute("onMouseOut");
	}

	public static String getOnMouseOverEvent(Element tag) {
		return tag.getAttribute("onMouseOver");
	}

	public static String getOnMouseUpEvent(Element tag) {
		return tag.getAttribute("onMouseUp");
	}

	public static String getOnResetEvent(Element tag) {
		return tag.getAttribute("onReset");
	}

	public static String getOnSubmitEvent(Element tag) {
		return tag.getAttribute("onSubmit");
	}

	public static String getXPath(Node tag) {
		Stack<String> tagStack = new Stack<String>();
		while (tag != null) {
			Node parent = tag.getParentNode();
			if (parent == null) {
				tagStack.push(tag.getNodeName().toUpperCase() + "[1]");
			} else {
				int pos = 1;
				for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
					Node child = parent.getChildNodes().item(i);
					if (child == tag) {
						break;
					}
					pos++;
				}
				tagStack.push(tag.getNodeName().toUpperCase() + "[" + pos + "]");
			}
			tag = parent;
		}
		StringBuilder domPath = new StringBuilder();
		while (!tagStack.isEmpty()) {
			domPath.append(tagStack.pop());
			if (!tagStack.isEmpty()) {
				domPath.append("/");
			}
		}
		return domPath.toString();
	}

	public static boolean hasAttribute(Element tag, String attribute, String value) {
		return tag.hasAttribute(attribute) && tag.getAttribute(attribute).equals(value);
	}

	public static boolean hasAttributeIgnoreCase(Element tag, String attribute, String value) {
		return tag.hasAttribute(attribute) && tag.getAttribute(attribute).equalsIgnoreCase(value);
	}

	public static boolean hasLinkEvent(Element tag) {
		return getLinkEvent(tag) != null;
	}

	public static boolean hasOnBlurEvent(Element tag) {
		return getOnBlurEvent(tag) != null;
	}

	public static boolean hasOnChangeEvent(Element tag) {
		return getOnChangeEvent(tag) != null;
	}

	public static boolean hasOnClickEvent(Element tag) {
		return getOnClickEvent(tag) != null;
	}

	public static boolean hasOnDoubleClickEvent(Element tag) {
		return getOnDoubleClickEvent(tag) != null;
	}

	public static boolean hasOnFocusEvent(Element tag) {
		return getOnFocusEvent(tag) != null;
	}

	public static boolean hasOnKeyDownEvent(Element tag) {
		return getOnKeyDownEvent(tag) != null;
	}

	public static boolean hasOnKeyUpEvent(Element tag) {
		return getOnKeyUpEvent(tag) != null;
	}

	public static boolean hasOnMouseDownEvent(Element tag) {
		return getOnMouseDownEvent(tag) != null;
	}

	public static boolean hasOnMouseMoveEvent(Element tag) {
		return getOnMouseMoveEvent(tag) != null;
	}

	public static boolean hasOnMouseOutEvent(Element tag) {
		return getOnMouseOutEvent(tag) != null;
	}

	public static boolean hasOnMouseOverEvent(Element tag) {
		return getOnMouseOverEvent(tag) != null;
	}

	public static boolean hasOnMouseUpEvent(Element tag) {
		return getOnMouseUpEvent(tag) != null;
	}

	public static boolean hasOnResetEvent(Element tag) {
		return getOnResetEvent(tag) != null;
	}

	public static boolean hasOnSubmitEvent(Element tag) {
		return getOnSubmitEvent(tag) != null;
	}

	public static boolean isAHrefTag(Element tag) {
		return isTag(tag, "a") && tag.hasAttribute("href");
	}

	public static boolean isAreaTag(Element tag) {
		return isTag(tag, "area");
	}

	public static boolean isBodyTag(Element tag) {
		return isTag(tag, "body");
	}

	public static boolean isBoldTag(Element tag) {
		return isTag(tag, "b");
	}

	public static boolean isEmbedTag(Element tag) {
		return isTag(tag, "embed");
	}

	public static boolean isEmphasisType(Element tag) {
		return isBoldTag(tag) || isItalicTag(tag) || isUnderlineTag(tag);
	}

	public static boolean isExternalScriptTag(Element tag) {
		return isScriptTag(tag) && tag.hasAttribute("src");
	}

	public static boolean isExternalStyleTag(Element tag) {
		return isLinkTag(tag) && tag.hasAttribute("href")
				&& (!tag.hasAttribute("type") || tag.getAttribute("type").contains("css"));
	}

	public static boolean isFontTag(Element tag) {
		return isTag(tag, "font");
	}

	public static boolean isFormTag(Element tag) {
		return isTag(tag, "form");
	}

	public static boolean isFormType(Element tag) {
		return isInputTag(tag) || isSelectTag(tag);
	}

	public static boolean isFrameTag(Element tag) {
		return isTag(tag, "frame");
	}

	public static boolean isH1Tag(Element tag) {
		return isTag(tag, "h1");
	}

	public static boolean isH2Tag(Element tag) {
		return isTag(tag, "h2");
	}

	public static boolean isHeadingTag(Element tag) {
		return isTag(tag, "h1") || isTag(tag, "h2") || isTag(tag, "h3") || isTag(tag, "h4") || isTag(tag, "h5")
				|| isTag(tag, "h6");
	}

	public static boolean isHeadTag(Element tag) {
		return isTag(tag, "head");
	}

	public static boolean isHrTag(Element tag) {
		return isTag(tag, "hr");
	}

	public static boolean isIFrameTag(Element tag) {
		return isTag(tag, "iframe");
	}

	public static boolean isImageTag(Element tag) {
		return isTag(tag, "img");
	}

	public static boolean isImageType(Element tag) {
		return isImageTag(tag) || isInputImageTag(tag);
	}

	public static boolean isInlineTagName(String tagName) {
		return inlineTags.contains(tagName.toUpperCase());
	}

	public static boolean isInputButtonTag(Element tag) {
		return isInputTag(tag) && hasAttributeIgnoreCase(tag, "type", "button");
	}

	public static boolean isInputCheckboxTag(Element tag) {
		return isInputTag(tag) && hasAttributeIgnoreCase(tag, "type", "checkbox");
	}

	public static boolean isInputFileTag(Element tag) {
		return isInputTag(tag) && hasAttributeIgnoreCase(tag, "type", "file");
	}

	public static boolean isInputHiddenTag(Element tag) {
		return isInputTag(tag) && hasAttributeIgnoreCase(tag, "type", "hidden");
	}

	public static boolean isInputImageTag(Element tag) {
		return isInputTag(tag) && hasAttributeIgnoreCase(tag, "type", "image");
	}

	public static boolean isInputPasswordTag(Element tag) {
		return isInputTag(tag) && hasAttributeIgnoreCase(tag, "type", "password");
	}

	public static boolean isInputRadioTag(Element tag) {
		return isInputTag(tag) && hasAttributeIgnoreCase(tag, "type", "radio");
	}

	public static boolean isInputResetTag(Element tag) {
		return isInputTag(tag) && hasAttributeIgnoreCase(tag, "type", "reset");
	}

	public static boolean isInputSubmitTag(Element tag) {
		return isInputTag(tag) && hasAttributeIgnoreCase(tag, "type", "submit");
	}

	public static boolean isInputTag(Element tag) {
		return isTag(tag, "input");
	}

	public static boolean isInputTextTag(Element tag) {
		return isInputTag(tag) && (!tag.hasAttribute("type") || hasAttributeIgnoreCase(tag, "type", "password"));
	}

	public static boolean isInternalScriptTag(Element tag) {
		return isScriptTag(tag) && !tag.hasAttribute("src");
	}

	public static boolean isInternalStyleTag(Element tag) {
		return isStyleTag(tag);
	}

	public static boolean isItalicTag(Element tag) {
		return isTag(tag, "i");
	}

	public static boolean isLineBreakTag(Element tag) {
		return isLineBreakTagName(tag.getTagName());
	}

	public static boolean isLineBreakTagName(String tagName) {
		return breakTags.contains(tagName.toUpperCase());
	}

	public static boolean isLinkTag(Element tag) {
		return isTag(tag, "link");
	}

	public static boolean isLinkType(Element tag) {
		return isAHrefTag(tag) || isAreaTag(tag) && tag.hasAttribute("href");
	}

	public static boolean isMetaTag(Element tag) {
		return isTag(tag, "meta");
	}

	public static boolean isObjectTag(Element tag) {
		return isTag(tag, "object");
	}

	public static boolean isObjectType(Element tag) {
		return isObjectTag(tag) || isEmbedTag(tag);
	}

	public static boolean isOptionTag(Element tag) {
		return isTag(tag, "option");
	}

	public static boolean isScriptTag(Element tag) {
		return isTag(tag, "script");
	}

	public static boolean isSelectTag(Element tag) {
		return isTag(tag, "select");
	}

	public static boolean isStyleTag(Element tag) {
		return isTag(tag, "style");
	}

	public static boolean isTableCellTag(Element tag) {
		return isTag(tag, "td");
	}

	public static boolean isTableTag(Element tag) {
		return isTag(tag, "table");
	}

	public static boolean isTextareaTag(Element tag) {
		return isTag(tag, "textarea");
	}

	public static boolean isTitleTag(Element tag) {
		return isTag(tag, "title");
	}

	public static boolean isUnderlineTag(Element tag) {
		return isTag(tag, "u");
	}

	private static boolean isTag(Element tag, String name) {
		return tag.getTagName().equalsIgnoreCase(name);
	}

}
