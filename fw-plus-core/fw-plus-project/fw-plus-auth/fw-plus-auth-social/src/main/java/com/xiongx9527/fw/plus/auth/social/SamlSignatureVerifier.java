package com.xiongx9527.fw.plus.auth.social;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

/**
 * SAML2 断言签名校验（失败关闭）。
 *
 * <p>未配置 IdP 证书、响应未签名、签名不匹配、断言过期或 audience 不匹配时一律拒绝，
 * 否则任何人都能伪造一条断言登录任意账号。</p>
 */
public final class SamlSignatureVerifier {

    private SamlSignatureVerifier() {
    }

    public static void verify(String xml, String certText, String audience) {
        if (certText == null || certText.trim().isEmpty()) {
            throw new IllegalStateException(
                    "saml idp certificate is not configured; refusing to accept an unverified assertion");
        }
        Document doc = parse(xml);
        registerIds(doc);
        NodeList signatures = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (signatures.getLength() == 0) {
            throw new IllegalStateException("saml response is not signed");
        }
        PublicKey publicKey = publicKey(certText);
        boolean valid = false;
        for (int i = 0; i < signatures.getLength() && !valid; i++) {
            DOMValidateContext context = new DOMValidateContext(publicKey, signatures.item(i));
            context.setProperty("javax.xml.crypto.dsig.cacheReference", Boolean.TRUE);
            // 显式关闭 secure validation：SAML2 生态 rsa-sha1 仍主流，且已按 IdP 证书固定校验，
            // 避免验签结果随 JDK 版本漂移（JDK17 默认判 SHA-1 无效）。
            context.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE);
            try {
                XMLSignature signature = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(context);
                valid = signature.validate(context);
            } catch (Exception e) {
                valid = false;
            }
        }
        if (!valid) {
            throw new IllegalStateException("saml assertion signature is invalid");
        }
        assertNotExpired(doc);
        assertAudience(doc, audience);
    }

    private static String stripPrologJunk(String xml) {
        if (xml == null || xml.isEmpty()) {
            return xml;
        }
        if (xml.charAt(0) == '\uFEFF') {
            xml = xml.substring(1);
        }
        int decl = xml.indexOf("<?xml");
        if (decl > 0) {
            xml = xml.substring(decl);
        }
        return xml;
    }

    private static Document parse(String xml) {
        xml = stripPrologJunk(xml);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("parse saml response failed: " + e.getMessage(), e);
        }
    }

    private static void registerIds(Document doc) {
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            for (String name : new String[]{"ID", "Id", "id"}) {
                if (element.hasAttribute(name)) {
                    element.setIdAttribute(name, true);
                    break;
                }
            }
        }
    }

    private static void assertNotExpired(Document doc) {
        String notOnOrAfter = attributeValue(doc, "NotOnOrAfter");
        if (notOnOrAfter == null) {
            return;
        }
        Instant deadline;
        try {
            deadline = Instant.parse(notOnOrAfter.trim());
        } catch (Exception e) {
            throw new IllegalStateException("saml assertion has an invalid NotOnOrAfter: " + notOnOrAfter);
        }
        if (Instant.now().isAfter(deadline)) {
            throw new IllegalStateException("saml assertion expired at " + notOnOrAfter);
        }
    }

    private static void assertAudience(Document doc, String expected) {
        if (expected == null || expected.trim().isEmpty()) {
            return;
        }
        NodeList audiences = doc.getElementsByTagNameNS("*", "Audience");
        if (audiences.getLength() == 0) {
            throw new IllegalStateException("saml assertion has no Audience restriction");
        }
        for (int i = 0; i < audiences.getLength(); i++) {
            if (expected.trim().equals(audiences.item(i).getTextContent().trim())) {
                return;
            }
        }
        throw new IllegalStateException("saml assertion audience does not match " + expected);
    }

    private static String attributeValue(Document doc, String attribute) {
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            if (element.hasAttribute(attribute)) {
                return element.getAttribute(attribute);
            }
        }
        return null;
    }

    private static PublicKey publicKey(String text) {
        String base64 = text
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        byte[] der;
        try {
            der = Base64.getDecoder().decode(base64);
        } catch (Exception e) {
            throw new IllegalStateException("saml idp certificate is not valid base64: " + e.getMessage(), e);
        }
        try {
            return CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der)).getPublicKey();
        } catch (Exception notCertificate) {
            try {
                return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
            } catch (Exception e) {
                throw new IllegalStateException("saml idp certificate/public key is invalid: "
                        + e.getMessage(), e);
            }
        }
    }
}
