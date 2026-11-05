package com.xiongx9527.fw.plus.auth.social.enterprise;
import com.xiongx9527.fw.plus.auth.social.*;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 飞书 / LarkSuite（FEISHU / LARK）扫码登录授权。
 *
 * <p>标准 OAuth2：app_access_token 取应用令牌 → oidc/access_token 用 code 换用户令牌 →
 * user_info 取资料。LarkSuite 与飞书协议同构，仅 base 不同（open.larksuite.com）。</p>
 *
 * <p>配置项：clientId=App ID、clientSecret=App Secret。</p>
 */
public class FeishuSocialLoginProvider extends AbstractSocialLoginProvider {

    protected String defaultBase() {
        return "https://open.feishu.cn";
    }

    @Override
    public IdpType type() {
        return IdpType.FEISHU;
    }

    @Override
    public String authorizationUrl(IdpConfig config, AuthorizationUrlRequest request) {
        String redirectUri = request.getRedirectUri() != null ? request.getRedirectUri() : config.getRedirectUri();
        String appId = config.getClientId();
        // 授权端点：优先用配置中的 authorizeUrl（网页授权场景可指向自定义网关），否则默认飞书网页授权。
        String endpoint = config.getAuthorizeUrl();
        if (endpoint == null || endpoint.isEmpty()) {
            endpoint = base(config, defaultBase()) + "/open-apis/authen/v1/authorize";
        }
        StringBuilder url = new StringBuilder(endpoint);
        url.append(endpoint.contains("?") ? "&" : "?");
        url.append("app_id=").append(urlEncode(appId))
                .append("&redirect_uri=").append(urlEncode(redirectUri))
                .append("&response_type=code");
        if (request.getState() != null) {
            url.append("&state=").append(urlEncode(request.getState()));
        }
        return url.toString();
    }

    @Override
    public ClientInfo clientInfo(IdpConfig config) {
        ClientInfo info = new ClientInfo();
        info.setClientId(config.getClientId());
        info.setRedirectUri(config.getRedirectUri());
        return info;
    }

    @Override
    public ExternalIdentity resolveIdentity(IdpConfig config, IdpCallback callback) {
        String base = base(config, defaultBase());
        String appToken = fetchAppAccessToken(base, config.getClientId(), config.getClientSecret());
        String userToken = fetchUserAccessToken(base, appToken, callback.getCode());
        JSONObject infoRes = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/open-apis/authen/v1/user_info", authHeader(userToken)));
        JSONObject u = infoRes.getJSONObject("data");
        if (u == null) {
            throw new IllegalStateException("feishu login failed: user_info not found; resp=" + infoRes);
        }
        String userId = u.getStr("user_id");
        String unionId = u.getStr("union_id");
        String openId = u.getStr("open_id");
        String mobile = u.getStr("mobile");
        String email = first(u.getStr("email"), u.getStr("enterprise_email"));
        String name = first(u.getStr("name"), userId, mobile, openId, unionId);
        String account = first(email, userId, mobile, openId, unionId);
        return new ExternalIdentity(first(userId, unionId, openId), name, email, first(userId, unionId, openId), null);
    }

    private String fetchAppAccessToken(String base, String appId, String appSecret) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("app_id", appId);
        body.put("app_secret", appSecret);
        JSONObject res = JSONUtil.parseObj(SocialHttpUtils.postJson(
                base + "/open-apis/auth/v3/app_access_token/internal", JSONUtil.toJsonStr(body), null));
        String token = first(res.getStr("app_access_token"), res.getStr("tenant_access_token"));
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("feishu login failed: app_access_token not found; resp=" + res);
        }
        return token;
    }

    private String fetchUserAccessToken(String base, String appToken, String code) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        JSONObject res = JSONUtil.parseObj(SocialHttpUtils.postJson(
                base + "/open-apis/authen/v1/oidc/access_token", JSONUtil.toJsonStr(body), authHeader(appToken)));
        JSONObject data = res.getJSONObject("data");
        String token = data == null ? null : data.getStr("access_token");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("feishu login failed: user access_token not found; resp=" + res);
        }
        return token;
    }

    protected static Map<String, String> authHeader(String token) {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + token);
        return headers;
    }
}
