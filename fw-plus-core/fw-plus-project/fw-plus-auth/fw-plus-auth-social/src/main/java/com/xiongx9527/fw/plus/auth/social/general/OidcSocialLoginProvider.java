package com.xiongx9527.fw.plus.auth.social.general;
import com.xiongx9527.fw.plus.auth.social.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OIDC（GENERAL）授权码流程：授权码换 token → 解析 id_token 声明 → 缺失资料时用 access_token 补 userinfo。
 *
 * <p>配置项：authorizeUrl / tokenUrl / userInfoUrl / clientId / clientSecret / redirectUri /
 * extra(scope, 默认 openid profile email)。</p>
 *
 * <p>严格 id_token 签名校验（RS256 + JWKS）默认交由接入方在 {@code IdpConfig} 上挂载的校验器完成；
 * 本实现在缺省情况下解码声明，接入生产环境应开启签名校验。</p>
 */
public class OidcSocialLoginProvider extends AbstractSocialLoginProvider {

    @Override
    public IdpType type() {
        return IdpType.OIDC;
    }

    @Override
    public String authorizationUrl(IdpConfig config, AuthorizationUrlRequest request) {
        String redirectUri = request.getRedirectUri() != null ? request.getRedirectUri() : config.getRedirectUri();
        StringBuilder url = new StringBuilder(config.getAuthorizeUrl());
        url.append(config.getAuthorizeUrl().contains("?") ? "&" : "?");
        url.append("response_type=code")
                .append("&client_id=").append(urlEncode(config.getClientId()))
                .append("&redirect_uri=").append(urlEncode(redirectUri))
                .append("&scope=").append(urlEncode(first(config.extra("scope"), "openid profile email")))
                .append("&nonce=").append(urlEncode(first(request.getState(), "nonce")));
        if (request.getState() != null) {
            url.append("&state=").append(urlEncode(request.getState()));
        }
        return url.toString();
    }

    @Override
    public ExternalIdentity resolveIdentity(IdpConfig config, IdpCallback callback) {
        Map<String, String> form = new LinkedHashMap<String, String>();
        form.put("grant_type", "authorization_code");
        form.put("code", callback.getCode() == null ? "" : callback.getCode());
        form.put("client_id", config.getClientId());
        form.put("client_secret", config.getClientSecret());
        form.put("redirect_uri", first(callback.parameter("redirect_uri"), config.getRedirectUri()));
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Accept", "application/json");
        String body = SocialHttpUtils.postForm(config.getTokenUrl(), form, headers);
        Map<String, Object> tokenResp = SocialHttpUtils.parseJsonMap(body);
        String idToken = (String) tokenResp.get("id_token");
        String accessToken = (String) tokenResp.get("access_token");

        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        if (idToken != null && !idToken.isEmpty()) {
            // 严格 RS256 + JWKS/证书 验签（iss/aud/exp/nonce），拒绝伪造 token
            claims = JwtRsaVerifier.verify(idToken, config, callback.getState());
        }

        String sub = str(claims, "sub");
        String email = str(claims, "email");
        String name = first(str(claims, "name"), str(claims, "preferred_username"));

        if (sub == null || email == null || name == null) {
            String userInfoUrl = config.getUserInfoUrl();
            if (accessToken != null && userInfoUrl != null && !userInfoUrl.isEmpty()) {
                Map<String, String> auth = new LinkedHashMap<String, String>();
                auth.put("Authorization", "Bearer " + accessToken);
                Map<String, Object> u = SocialHttpUtils.parseJsonMap(SocialHttpUtils.get(userInfoUrl, auth));
                if (sub == null) {
                    sub = str(u, "sub");
                }
                if (email == null) {
                    email = str(u, "email");
                }
                if (name == null) {
                    name = first(str(u, "name"), str(u, "preferred_username"));
                }
                if (claims.isEmpty()) {
                    claims.putAll(u);
                }
            }
        }
        if (sub == null || sub.isEmpty()) {
            throw new IllegalStateException("oidc identity has no subject");
        }
        return new ExternalIdentity(sub, name, email, sub, claims);
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m == null ? null : m.get(k);
        return v == null ? null : String.valueOf(v);
    }
}
