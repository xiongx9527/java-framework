package com.xiongx9527.fw.plus.auth.social.enterprise;
import com.xiongx9527.fw.plus.auth.social.*;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 钉钉（DINGTALK）企业内部应用网页扫码登录（DTFrameLogin, scope=openid）走新版 OAuth2：
 * userAccessToken 换用户令牌 → contact/users/me 取 unionId → 内部 token 经
 * topapi/v2/user/getbyunionid 换 userid 并补全资料。
 *
 * <p>配置项：clientId=AppKey、clientSecret=AppSecret、redirectUri。
 * 旧版 sns 流程仅适用于「网站应用」，企业内部应用会报 40001，故采用新版 OAuth2。</p>
 */
public class DingTalkSocialLoginProvider extends AbstractSocialLoginProvider {

    private static final String OAUTH_BASE = "https://api.dingtalk.com";

    @Override
    public IdpType type() {
        return IdpType.DINGTALK;
    }

    @Override
    public String authorizationUrl(IdpConfig config, AuthorizationUrlRequest request) {
        String redirectUri = request.getRedirectUri() != null ? request.getRedirectUri() : config.getRedirectUri();
        // 授权端点：优先用配置中的 authorizeUrl（如网页授权 oapi.dingtalk.com/connect/oauth2/authorize），
        // 否则默认网页授权登录（在应用内打开网页场景）。
        String endpoint = config.getAuthorizeUrl();
        if (endpoint == null || endpoint.isEmpty()) {
            endpoint = "https://oapi.dingtalk.com/connect/oauth2/authorize";
        }
        boolean oapi = endpoint.contains("oapi.dingtalk.com/connect/oauth2/authorize");
        String clientParam = oapi ? "appid" : "client_id";
        StringBuilder url = new StringBuilder(endpoint);
        url.append(endpoint.contains("?") ? "&" : "?");
        url.append("response_type=code")
                .append("&").append(clientParam).append("=").append(urlEncode(config.getClientId()))
                .append("&redirect_uri=").append(urlEncode(redirectUri))
                .append("&scope=openid");
        if (request.getState() != null) {
            url.append("&state=").append(urlEncode(request.getState()));
        }
        if (oapi && url.indexOf("#") < 0) {
            url.append("#/");
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
        String userToken = fetchUserAccessToken(config, callback.getCode());
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("x-acs-dingtalk-access-token", userToken);
        JSONObject me = JSONUtil.parseObj(SocialHttpUtils.get(OAUTH_BASE + "/v1.0/contact/users/me", headers));
        String unionId = me.getStr("unionId");
        String nick = me.getStr("nick");
        String email = me.getStr("email");
        String userId = null;
        String name = nick;
        String detailEmail = email;
        if (unionId != null && !unionId.isEmpty()) {
            String token = fetchAppToken(config);
            JSONObject byUnion = JSONUtil.parseObj(SocialHttpUtils.postJson(
                    "https://oapi.dingtalk.com/topapi/v2/user/getbyunionid?access_token=" + urlEncode(token),
                    JSONUtil.toJsonStr(mapOf("unionid", unionId)), null));
            checkResponse(byUnion, "dingtalk getbyunionid");
            JSONObject result = byUnion.getJSONObject("result");
            if (result != null) {
                userId = result.getStr("userid");
            }
            if (userId != null && !userId.isEmpty()) {
                JSONObject user = JSONUtil.parseObj(SocialHttpUtils.postJson(
                        "https://oapi.dingtalk.com/topapi/v2/user/get?access_token=" + urlEncode(token),
                        JSONUtil.toJsonStr(mapOf("userid", userId, "language", "zh_CN")), null));
                checkResponse(user, "dingtalk user/get");
                JSONObject u = user.getJSONObject("result");
                if (u != null) {
                    name = first(u.getStr("name"), nick);
                    detailEmail = first(u.getStr("email"), email);
                }
            }
        }
        String account = first(detailEmail, userId, unionId, nick);
        return new ExternalIdentity(first(userId, unionId), name, detailEmail, first(userId, unionId), null);
    }

    private String fetchUserAccessToken(IdpConfig config, String code) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("clientId", config.getClientId());
        body.put("clientSecret", config.getClientSecret());
        body.put("code", code);
        body.put("grantType", "authorization_code");
        JSONObject res = JSONUtil.parseObj(SocialHttpUtils.postJson(
                OAUTH_BASE + "/v1.0/oauth2/userAccessToken", JSONUtil.toJsonStr(body), null));
        String token = res.getStr("accessToken");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("dingtalk oauth login failed: userAccessToken not found; resp=" + res);
        }
        return token;
    }

    private String fetchAppToken(IdpConfig config) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("appKey", config.getClientId());
        body.put("appSecret", config.getClientSecret());
        JSONObject res = JSONUtil.parseObj(SocialHttpUtils.postJson(
                OAUTH_BASE + "/v1.0/oauth2/accessToken", JSONUtil.toJsonStr(body), null));
        String token = res.getStr("accessToken");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("dingtalk app token failed; resp=" + res);
        }
        return token;
    }

    private static Map<String, Object> mapOf(String k1, Object v1) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put(k1, v1);
        return m;
    }

    private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }
}
