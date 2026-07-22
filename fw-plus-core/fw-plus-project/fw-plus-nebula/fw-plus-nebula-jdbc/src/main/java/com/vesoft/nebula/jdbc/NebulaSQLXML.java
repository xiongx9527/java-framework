package com.vesoft.nebula.jdbc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.SQLXML;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamWriter;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

public class NebulaSQLXML implements SQLXML {
	private String value;
	private boolean freed;
	private Result pendingResult;
	private StringWriter pendingWriter;
	private XMLStreamWriter pendingXmlStreamWriter;

	public NebulaSQLXML() {
		this(null);
	}

	public NebulaSQLXML(String value) {
		this.value = value;
	}

	@Override
	public void free() throws SQLException {
		freed = true;
		value = null;
	}

	@Override
	public InputStream getBinaryStream() throws SQLException {
		checkOpen();
		materializePendingResult();
		return value == null ? null : new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public OutputStream setBinaryStream() throws SQLException {
		checkOpen();
		clearPendingResult();
		return new ByteArrayOutputStream() {
			@Override
			public void close() {
				NebulaSQLXML.this.value = new String(toByteArray(), StandardCharsets.UTF_8);
			}
		};
	}

	@Override
	public Reader getCharacterStream() throws SQLException {
		checkOpen();
		materializePendingResult();
		return value == null ? null : new StringReader(value);
	}

	@Override
	public Writer setCharacterStream() throws SQLException {
		checkOpen();
		clearPendingResult();
		return new StringWriter() {
			@Override
			public void close() {
				NebulaSQLXML.this.value = toString();
			}
		};
	}

	@Override
	public String getString() throws SQLException {
		checkOpen();
		materializePendingResult();
		return value;
	}

	@Override
	public void setString(String value) throws SQLException {
		checkOpen();
		clearPendingResult();
		this.value = value;
	}

	@Override
	public <T extends Source> T getSource(Class<T> sourceClass) throws SQLException {
		checkOpen();
		materializePendingResult();
		String xml = value == null ? "" : value;
		if (sourceClass == null || sourceClass.isAssignableFrom(StreamSource.class)) {
			return sourceClass == null
					? (T) new StreamSource(new StringReader(xml))
					: sourceClass.cast(new StreamSource(new StringReader(xml)));
		}
		if (sourceClass.isAssignableFrom(DOMSource.class)) {
			try {
				return sourceClass.cast(new DOMSource(DocumentBuilderFactory.newInstance()
						.newDocumentBuilder()
						.parse(new InputSource(new StringReader(xml)))));
			} catch (Exception exception) {
				throw new SQLException("Failed to create DOMSource from SQLXML.", exception);
			}
		}
		if (sourceClass.isAssignableFrom(SAXSource.class)) {
			return sourceClass.cast(new SAXSource(new InputSource(new StringReader(xml))));
		}
		if (sourceClass.isAssignableFrom(StAXSource.class)) {
			try {
				return sourceClass.cast(new StAXSource(XMLInputFactory.newFactory()
						.createXMLStreamReader(new StringReader(xml))));
			} catch (Exception exception) {
				throw new SQLException("Failed to create StAXSource from SQLXML.", exception);
			}
		}
		throw new SQLException("Unsupported SQLXML Source type: " + sourceClass.getName());
	}

	@Override
	public <T extends Result> T setResult(Class<T> resultClass) throws SQLException {
		checkOpen();
		clearPendingResult();
		if (resultClass == null || resultClass.isAssignableFrom(StreamResult.class)) {
			pendingWriter = new StringWriter() {
				@Override
				public void close() {
					NebulaSQLXML.this.value = toString();
				}
			};
			StreamResult result = new StreamResult(pendingWriter);
			pendingResult = result;
			return resultClass == null ? (T) result : resultClass.cast(result);
		}
		if (resultClass.isAssignableFrom(DOMResult.class)) {
			DOMResult result = new DOMResult();
			pendingResult = result;
			return resultClass.cast(result);
		}
		if (resultClass.isAssignableFrom(SAXResult.class)) {
			XmlStringContentHandler handler = new XmlStringContentHandler();
			SAXResult result = new SAXResult(handler);
			pendingResult = result;
			return resultClass.cast(result);
		}
		if (resultClass.isAssignableFrom(StAXResult.class)) {
			try {
				pendingWriter = new StringWriter();
				pendingXmlStreamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(pendingWriter);
				StAXResult result = new StAXResult(pendingXmlStreamWriter);
				pendingResult = result;
				return resultClass.cast(result);
			} catch (Exception exception) {
				throw new SQLException("Failed to create StAXResult for SQLXML.", exception);
			}
		}
		throw new SQLException("Unsupported SQLXML Result type: " + resultClass.getName());
	}

	private void checkOpen() throws SQLException {
		if (freed) {
			throw new SQLException("SQLXML has been freed.");
		}
	}

	private void clearPendingResult() {
		pendingResult = null;
		pendingWriter = null;
		pendingXmlStreamWriter = null;
	}

	private void materializePendingResult() throws SQLException {
		if (pendingResult == null) {
			return;
		}
		try {
			if (pendingResult instanceof DOMResult) {
				Node node = ((DOMResult) pendingResult).getNode();
				value = node == null ? null : transformNode(node);
				} else if (pendingResult instanceof SAXResult) {
					value = ((XmlStringContentHandler) ((SAXResult) pendingResult).getHandler()).getXml();
				} else if (pendingResult instanceof StAXResult) {
					value = pendingWriter == null ? null : pendingWriter.toString();
				} else if (pendingWriter != null) {
				value = pendingWriter.toString();
			}
			clearPendingResult();
		} catch (Exception exception) {
			throw new SQLException("Failed to read SQLXML result content.", exception);
		}
	}

	private String transformNode(Node node) throws Exception {
		StringWriter writer = new StringWriter();
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.transform(new DOMSource(node), new StreamResult(writer));
		return writer.toString();
	}

	private static class XmlStringContentHandler extends DefaultHandler {
		private final StringBuilder builder = new StringBuilder();

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			String name = qName == null || qName.length() == 0 ? localName : qName;
			builder.append('<').append(name);
			for (int i = 0; attributes != null && i < attributes.getLength(); i++) {
				String attributeName = attributes.getQName(i);
				if (attributeName == null || attributeName.length() == 0) {
					attributeName = attributes.getLocalName(i);
				}
				builder.append(' ').append(attributeName).append("=\"")
						.append(escape(attributes.getValue(i))).append('"');
			}
			builder.append('>');
		}

		@Override
		public void endElement(String uri, String localName, String qName) {
			String name = qName == null || qName.length() == 0 ? localName : qName;
			builder.append("</").append(name).append('>');
		}

		@Override
		public void characters(char[] ch, int start, int length) {
			builder.append(escape(new String(ch, start, length)));
		}

		private String getXml() {
			return builder.toString();
		}

		private static String escape(String value) {
			if (value == null) {
				return "";
			}
			return value.replace("&", "&amp;")
					.replace("<", "&lt;")
					.replace(">", "&gt;")
					.replace("\"", "&quot;");
		}
	}
}
