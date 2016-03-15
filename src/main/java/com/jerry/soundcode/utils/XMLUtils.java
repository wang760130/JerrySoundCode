package com.jerry.soundcode.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

class XMLUtils {

	private static final String PROPS_DTD_URI = "http://java.sun.com/dtd/properties.dtd";

	private static final String PROPS_DTD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<!-- DTD for properties -->"
			+ "<!ELEMENT properties ( comment?, entry* ) >"
			+ "<!ATTLIST properties"
			+ " version CDATA #FIXED \"1.0\">"
			+ "<!ELEMENT comment (#PCDATA) >"
			+ "<!ELEMENT entry (#PCDATA) >"
			+ "<!ATTLIST entry " + " key CDATA #REQUIRED>";

	
	private static final String EXTERNAL_XML_VERSION = "1.0";

	static void load(Properties props, InputStream in) throws IOException,
			InvalidPropertiesFormatException {
		Document doc = null;
		try {
			doc = getLoadingDoc(in);
		} catch (SAXException saxe) {
			throw new InvalidPropertiesFormatException(saxe);
		}
		Element propertiesElement = doc.getDocumentElement();
		String xmlVersion = propertiesElement.getAttribute("version");
		if (xmlVersion.compareTo(EXTERNAL_XML_VERSION) > 0)
			throw new InvalidPropertiesFormatException(
					"Exported Properties file format version "
							+ xmlVersion
							+ " is not supported. This java installation can read"
							+ " versions " + EXTERNAL_XML_VERSION
							+ " or older. You"
							+ " may need to install a newer version of JDK.");
		importProperties(props, propertiesElement);
	}

	static Document getLoadingDoc(InputStream in) throws SAXException,
			IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setValidating(true);
		dbf.setCoalescing(true);
		dbf.setIgnoringComments(true);
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setEntityResolver(new Resolver());
			db.setErrorHandler(new EH());
			InputSource is = new InputSource(in);
			return db.parse(is);
		} catch (ParserConfigurationException x) {
			throw new Error(x);
		}
	}

	static void importProperties(Properties props, Element propertiesElement) {
		NodeList entries = propertiesElement.getChildNodes();
		int numEntries = entries.getLength();
		int start = numEntries > 0
				&& entries.item(0).getNodeName().equals("comment") ? 1 : 0;
		for (int i = start; i < numEntries; i++) {
			Element entry = (Element) entries.item(i);
			if (entry.hasAttribute("key")) {
				Node n = entry.getFirstChild();
				String val = (n == null) ? "" : n.getNodeValue();
				props.setProperty(entry.getAttribute("key"), val);
			}
		}
	}

	static void save(Properties props, OutputStream os, String comment,
			String encoding) throws IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			assert (false);
		}
		Document doc = db.newDocument();
		Element properties = (Element) doc.appendChild(doc
				.createElement("properties"));

		if (comment != null) {
			Element comments = (Element) properties.appendChild(doc
					.createElement("comment"));
			comments.appendChild(doc.createTextNode(comment));
		}

		Set keys = props.keySet();
		Iterator i = keys.iterator();
		while (i.hasNext()) {
			String key = (String) i.next();
			Element entry = (Element) properties.appendChild(doc
					.createElement("entry"));
			entry.setAttribute("key", key);
			entry.appendChild(doc.createTextNode(props.getProperty(key)));
		}
		emitDocument(doc, os, encoding);
	}

	static void emitDocument(Document doc, OutputStream os, String encoding)
			throws IOException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = null;
		try {
			t = tf.newTransformer();
			t.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, PROPS_DTD_URI);
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.setOutputProperty(OutputKeys.METHOD, "xml");
			t.setOutputProperty(OutputKeys.ENCODING, encoding);
		} catch (TransformerConfigurationException tce) {
			assert (false);
		}
		DOMSource doms = new DOMSource(doc);
		StreamResult sr = new StreamResult(os);
		try {
			t.transform(doms, sr);
		} catch (TransformerException te) {
			IOException ioe = new IOException();
			ioe.initCause(te);
			throw ioe;
		}
	}

	private static class Resolver implements EntityResolver {
		public InputSource resolveEntity(String pid, String sid)
				throws SAXException {
			if (sid.equals(PROPS_DTD_URI)) {
				InputSource is;
				is = new InputSource(new StringReader(PROPS_DTD));
				is.setSystemId(PROPS_DTD_URI);
				return is;
			}
			throw new SAXException("Invalid system identifier: " + sid);
		}
	}

	private static class EH implements ErrorHandler {
		public void error(SAXParseException x) throws SAXException {
			throw x;
		}

		public void fatalError(SAXParseException x) throws SAXException {
			throw x;
		}

		public void warning(SAXParseException x) throws SAXException {
			throw x;
		}
	}

}
