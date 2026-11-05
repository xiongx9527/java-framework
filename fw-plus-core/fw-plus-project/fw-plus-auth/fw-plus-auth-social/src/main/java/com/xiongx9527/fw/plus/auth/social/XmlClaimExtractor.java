package com.xiongx9527.fw.plus.auth.social;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * 轻量 XML 声明提取（CAS serviceValidate / SAML2 断言共用）。禁用 DOCTYPE 与外部实体，防 XXE。
 */
public final class XmlClaimExtractor {

    private XmlClaimExtractor() {
    }

    public static Document parse(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setExpandEntityReferences(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("xml parse failed: " + e.getMessage(), e);
        }
    }

    public static String textByLocalName(Document doc, String localName) {
        NodeList list = doc.getElementsByTagNameNS("*", localName);
        if (list.getLength() == 0) {
            list = doc.getElementsByTagName(localName);
        }
        if (list.getLength() > 0) {
            return text(list.item(0));
        }
        return null;
    }

    public static Map<String, String> namedAttributes(Document doc) {
        Map<String, String> attrs = new LinkedHashMap<String, String>();
        NodeList attributes = doc.getElementsByTagNameNS("*", "Attribute");
        for (int i = 0; i < attributes.getLength(); i++) {
            Element el = (Element) attributes.item(i);
            String name = el.getAttribute("Name");
            if (name == null || name.isEmpty()) {
                continue;
            }
            NodeList values = el.getElementsByTagNameNS("*", "AttributeValue");
            if (values.getLength() > 0) {
                attrs.put(name, text(values.item(0)));
            }
        }
        NodeList casAttrs = doc.getElementsByTagNameNS("*", "attributes");
        for (int i = 0; i < casAttrs.getLength(); i++) {
            NodeList children = casAttrs.item(i).getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node c = children.item(j);
                if (c.getNodeType() == Node.ELEMENT_NODE) {
                    String name = c.getLocalName();
                    if (!attrs.containsKey(name)) {
                        String val = text(c);
                        if (val != null) {
                            attrs.put(name, val);
                        }
                    }
                }
            }
        }
        return attrs;
    }

    private static String text(Node node) {
        StringBuilder sb = new StringBuilder();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node c = children.item(i);
            if (c.getNodeType() == Node.TEXT_NODE) {
                sb.append(c.getNodeValue());
            }
        }
        return sb.length() == 0 ? null : sb.toString().trim();
    }
}
