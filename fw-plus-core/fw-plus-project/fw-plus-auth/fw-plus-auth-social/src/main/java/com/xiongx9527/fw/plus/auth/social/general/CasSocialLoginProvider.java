package com.xiongx9527.fw.plus.auth.social.general;
import com.xiongx9527.fw.plus.auth.social.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;

/**
 * CAS（GENERAL）协议：/login 授权跳转，/serviceValidate 校验 ticket 并解析 XML。
 *
 * <p>配置项：authorizeUrl（登录页）、tokenUrl（serviceValidate 地址）、redirectUri。
 * resolveIdentity 的回调参数取 {@code ticket}（或 code）与 {@code service}。</p>
 */
public class CasSocialLoginProvider extends AbstractSocialLoginProvider {

    @Override
    public IdpType type() {
        return IdpType.CAS;
    }

    @Override
    public String authorizationUrl(IdpConfig config, AuthorizationUrlRequest request) {
        String redirectUri = request.getRedirectUri() != null ? request.getRedirectUri() : config.getRedirectUri();
        StringBuilder url = new StringBuilder(config.getAuthorizeUrl());
        url.append(config.getAuthorizeUrl().contains("?") ? "?" : "?");
        url.append("service=").append(urlEncode(redirectUri));
        if (request.getState() != null) {
            url.append("&state=").append(urlEncode(request.getState()));
        }
        return url.toString();
    }

    @Override
    public ExternalIdentity resolveIdentity(IdpConfig config, IdpCallback callback) {
        String ticket = callback.getCode() != null ? callback.getCode() : callback.parameter("ticket");
        String service = first(callback.parameter("service"), config.getRedirectUri());
        String url = config.getTokenUrl()
                + (config.getTokenUrl().contains("?") ? "&" : "?")
                + "service=" + urlEncode(service)
                + "&ticket=" + urlEncode(ticket);
        String xml = SocialHttpUtils.get(url, null);
        Document doc = XmlClaimExtractor.parse(xml);
        String user = XmlClaimExtractor.textByLocalName(doc, "user");
        Map<String, String> attrs = XmlClaimExtractor.namedAttributes(doc);
        String email = attrs.get("email");
        String name = attrs.get("name");
        String externalId = user != null ? user : attrs.get("uid");
        if (externalId == null || externalId.trim().isEmpty()) {
            throw new IllegalStateException("cas identity has no user");
        }
        Map<String, Object> attrObj = toObjectMap(attrs);
        return new ExternalIdentity("cas:" + (user != null ? user : externalId), name, email, externalId, attrObj);
    }

    private static Map<String, Object> toObjectMap(Map<String, String> src) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, String> e : src.entrySet()) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }
}
