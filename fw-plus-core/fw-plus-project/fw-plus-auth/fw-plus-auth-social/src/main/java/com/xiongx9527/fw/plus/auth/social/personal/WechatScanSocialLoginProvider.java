package com.xiongx9527.fw.plus.auth.social.personal;
import com.xiongx9527.fw.plus.auth.social.*;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 个人微信扫码登录（WECHAT_SCAN，微信开放平台「网站应用」OAuth2 扫码）。
 *
 * <p>流程：authorize（snsapi_login）→ sns/oauth2/access_token 换 access_token + openid + unionid →
 * sns/userinfo 取昵称/头像。配置项：clientId=AppID、clientSecret=AppSecret。</p>
 */
public class WechatScanSocialLoginProvider extends AbstractSocialLoginProvider {

    private static final String AUTHORIZE = "https://open.weixin.qq.com/connect/qrconnect";
    private static final String TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String USERINFO_URL = "https://api.weixin.qq.com/sns/userinfo";

    @Override
    public IdpType type() {
        return IdpType.WECHAT_SCAN;
    }

    @Override
    public String authorizationUrl(IdpConfig config, AuthorizationUrlRequest request) {
        String redirectUri = request.getRedirectUri() != null ? request.getRedirectUri() : config.getRedirectUri();
        StringBuilder url = new StringBuilder(AUTHORIZE);
        url.append("?appid=").append(urlEncode(config.getClientId()))
                .append("&redirect_uri=").append(urlEncode(redirectUri))
                .append("&response_type=code")
                .append("&scope=").append(urlEncode(first(config.extra("scope"), "snsapi_login")));
        if (request.getState() != null) {
            url.append("&state=").append(urlEncode(request.getState()));
        }
        url.append("#wechat_redirect");
        return url.toString();
    }

    @Override
    public ExternalIdentity resolveIdentity(IdpConfig config, IdpCallback callback) {
        String userKey = "wechat-scan:user:" + config.getId();
        String accessToken;
        String openid;
        String unionid;
        CachedUserToken cached = tokenStore().getUserToken(userKey);
        if (cached != null && !cached.isExpired(60000L)) {
            accessToken = cached.getAccessToken();
            openid = cached.getOpenid();
            unionid = cached.getUnionid();
        } else {
            JSONObject token = exchangeToken(config, callback.getCode());
            accessToken = token.getStr("access_token");
            openid = token.getStr("openid");
            unionid = token.getStr("unionid");
            cacheUserToken(userKey, token);
        }
        JSONObject info = fetchUserInfo(config, userKey, accessToken, openid, unionid);
        String nickname = info.getStr("nickname");
        String account = first(unionid, openid);
        return new ExternalIdentity(account, nickname, null, first(unionid, openid), null);
    }

    private JSONObject exchangeToken(IdpConfig config, String code) {
        JSONObject token = JSONUtil.parseObj(SocialHttpUtils.get(
                TOKEN_URL + "?appid=" + urlEncode(config.getClientId())
                        + "&secret=" + urlEncode(config.getClientSecret())
                        + "&code=" + urlEncode(code)
                        + "&grant_type=authorization_code", null));
        if (token.getStr("access_token") == null) {
            throw new IllegalStateException("wechat scan login failed: " + token);
        }
        return token;
    }

    private void cacheUserToken(String userKey, JSONObject token) {
        Long expiresIn = token.getLong("expires_in");
        long expireAt = System.currentTimeMillis() + (expiresIn != null ? expiresIn : 7200L) * 1000L;
        tokenStore().putUserToken(userKey, new CachedUserToken(
                token.getStr("access_token"),
                token.getStr("refresh_token"),
                token.getStr("openid"),
                token.getStr("unionid"),
                expireAt));
    }

    private JSONObject fetchUserInfo(IdpConfig config, String userKey, String accessToken, String openid, String unionid) {
        JSONObject info = JSONUtil.parseObj(SocialHttpUtils.get(
                USERINFO_URL + "?access_token=" + urlEncode(accessToken)
                        + "&openid=" + urlEncode(openid), null));
        // access_token 失效（errcode 42001/40014/41001/42002/40001 等）：用 refresh_token 续期后重试一次
        if (isTokenExpired(info)) {
            CachedUserToken cached = tokenStore().getUserToken(userKey);
            if (cached != null && cached.getRefreshToken() != null) {
                JSONObject refreshed = refreshToken(config, cached.getRefreshToken());
                String newToken = refreshed.getStr("access_token");
                String newRefresh = refreshed.getStr("refresh_token");
                Long expiresIn = refreshed.getLong("expires_in");
                long expireAt = System.currentTimeMillis() + (expiresIn != null ? expiresIn : 7200L) * 1000L;
                tokenStore().putUserToken(userKey, new CachedUserToken(newToken, newRefresh, openid, unionid, expireAt));
                info = JSONUtil.parseObj(SocialHttpUtils.get(
                        USERINFO_URL + "?access_token=" + urlEncode(newToken)
                                + "&openid=" + urlEncode(openid), null));
            }
        }
        return info;
    }

    private boolean isTokenExpired(JSONObject info) {
        Object errcode = info.getByPath("errcode");
        if (errcode instanceof Number) {
            int code = ((Number) errcode).intValue();
            return code == 42001 || code == 40014 || code == 41001 || code == 42002 || code == 40001;
        }
        return false;
    }

    private JSONObject refreshToken(IdpConfig config, String refreshToken) {
        JSONObject res = JSONUtil.parseObj(SocialHttpUtils.get(
                TOKEN_URL + "?appid=" + urlEncode(config.getClientId())
                        + "&grant_type=refresh_token"
                        + "&refresh_token=" + urlEncode(refreshToken), null));
        if (res.getStr("access_token") == null) {
            throw new IllegalStateException("wechat scan refresh token failed: " + res);
        }
        return res;
    }
}
