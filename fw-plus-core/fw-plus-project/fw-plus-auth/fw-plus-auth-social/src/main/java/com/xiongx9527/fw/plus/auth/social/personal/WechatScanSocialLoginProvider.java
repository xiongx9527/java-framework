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
        String code = callback.getCode();
        JSONObject token = JSONUtil.parseObj(SocialHttpUtils.get(
                TOKEN_URL + "?appid=" + urlEncode(config.getClientId())
                        + "&secret=" + urlEncode(config.getClientSecret())
                        + "&code=" + urlEncode(code)
                        + "&grant_type=authorization_code", null));
        if (token.getStr("access_token") == null) {
            throw new IllegalStateException("wechat scan login failed: " + token);
        }
        String accessToken = token.getStr("access_token");
        String openid = token.getStr("openid");
        String unionid = token.getStr("unionid");
        JSONObject info = JSONUtil.parseObj(SocialHttpUtils.get(
                USERINFO_URL + "?access_token=" + urlEncode(accessToken)
                        + "&openid=" + urlEncode(openid), null));
        String nickname = info.getStr("nickname");
        String account = first(unionid, openid);
        return new ExternalIdentity(account, nickname, null, first(unionid, openid), null);
    }
}
