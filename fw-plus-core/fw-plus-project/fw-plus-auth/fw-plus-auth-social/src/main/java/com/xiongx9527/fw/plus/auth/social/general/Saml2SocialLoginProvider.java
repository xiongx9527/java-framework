package com.xiongx9527.fw.plus.auth.social.general;
import com.xiongx9527.fw.plus.auth.social.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.w3c.dom.Document;

import cn.hutool.core.codec.Base64;

/**
 * SAML2（GENERAL）HTTP-POST / HTTP-Redirect 绑定：签发 AuthnRequest 跳转，回调解码断言并提取属性。
 *
 * <p>配置项：authorizeUrl（IdP SSO 地址）、redirectUri（ACS URL）、extra(saml-issuer)。
 * 回调参数取 {@code SAMLResponse}（base64，必要时 zlib inflate）。</p>
 *
 * <p>断言签名校验（防伪造登录）强烈建议开启：接入方应在 {@code IdpConfig} 上挂载证书并完成验签后再调用
 * resolveIdentity；本实现默认只做解码与属性提取。</p>
 */
public class Saml2SocialLoginProvider extends AbstractSocialLoginProvider {

    @Override
    public IdpType type() {
        return IdpType.SAML2;
    }

    @Override
    public String authorizationUrl(IdpConfig config, AuthorizationUrlRequest request) {
        String requestXml = authnRequest(config);
        String encoded = Base64.encode(deflate(requestXml));
        StringBuilder url = new StringBuilder(config.getAuthorizeUrl());
        url.append(config.getAuthorizeUrl().contains("?") ? "&" : "?");
        url.append("SAMLRequest=").append(urlEncode(encoded));
        if (request.getState() != null) {
            url.append("&RelayState=").append(urlEncode(request.getState()));
        }
        return url.toString();
    }

    @Override
    public ExternalIdentity resolveIdentity(IdpConfig config, IdpCallback callback) {
        String samlResponse = callback.parameter("SAMLResponse");
        if (samlResponse == null) {
            samlResponse = callback.getCode();
        }
        if (samlResponse == null) {
            throw new IllegalStateException("saml response missing");
        }
        String xml = decode(samlResponse);
        Document doc = XmlClaimExtractor.parse(xml);
        String nameId = XmlClaimExtractor.textByLocalName(doc, "NameID");
        Map<String, String> attrs = XmlClaimExtractor.namedAttributes(doc);

        String account = attrs.get("uid");
        if (account == null) {
            account = attrs.get("urn:oid:0.9.2342.19200300.100.1.1");
        }
        if (account == null) {
            account = nameId;
        }
        if (account == null || account.trim().isEmpty()) {
            throw new IllegalStateException("saml assertion has no subject identifier");
        }
        String email = attrs.get("email");
        if (email == null) {
            email = attrs.get("urn:oid:0.9.2342.19200300.100.1.3");
        }
        String name = attrs.get("cn");
        if (name == null) {
            name = attrs.get("displayName");
        }
        String externalId = nameId != null ? nameId : account;
        return new ExternalIdentity("saml:" + account, name, email, externalId, toObjectMap(attrs));
    }

    private static String authnRequest(IdpConfig config) {
        String issuer = config.extra("saml-issuer");
        if (issuer == null) {
            issuer = config.getClientId() == null ? "fw-plus" : config.getClientId();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<samlp:AuthnRequest xmlns:samlp=").append('"')
                .append("urn:oasis:names:tc:SAML:2.0:protocol").append('"')
                .append(" xmlns:saml=").append('"')
                .append("urn:oasis:names:tc:SAML:2.0:assertion").append('"')
                .append(" ID=").append('"').append('_').append(java.util.UUID.randomUUID().toString().replace("-", "")).append('"')
                .append(" Version=").append('"').append("2.0").append('"')
                .append(" IssueInstant=").append('"').append(java.time.Instant.now().toString()).append('"')
                .append(" Destination=").append('"').append(config.getAuthorizeUrl()).append('"')
                .append(" ProtocolBinding=").append('"').append("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST").append('"');
        if (config.getRedirectUri() != null && !config.getRedirectUri().isEmpty()) {
            sb.append(" AssertionConsumerServiceURL=").append('"').append(config.getRedirectUri()).append('"');
        }
        sb.append("><saml:Issuer>").append(issuer).append("</saml:Issuer></samlp:AuthnRequest>");
        return sb.toString();
    }

    private static byte[] deflate(String xml) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DeflaterOutputStream out = new DeflaterOutputStream(buffer, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
            out.write(xml.getBytes(StandardCharsets.UTF_8));
            out.finish();
            out.close();
            return buffer.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("build saml request failed: " + e.getMessage(), e);
        }
    }

    private static String decode(String samlResponse) {
        byte[] decoded = Base64.decode(samlResponse);
        try (InflaterInputStream iis = new InflaterInputStream(
                new ByteArrayInputStream(decoded), new Inflater(true))) {
            return new String(readAll(iis), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new String(decoded, StandardCharsets.UTF_8);
        }
    }

    private static byte[] readAll(InflaterInputStream in) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) >= 0) {
            buf.write(chunk, 0, n);
        }
        return buf.toByteArray();
    }

    private static Map<String, Object> toObjectMap(Map<String, String> src) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, String> e : src.entrySet()) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }
}
