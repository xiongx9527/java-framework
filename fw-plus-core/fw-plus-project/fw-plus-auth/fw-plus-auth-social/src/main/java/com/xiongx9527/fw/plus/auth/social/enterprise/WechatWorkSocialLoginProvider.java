package com.xiongx9527.fw.plus.auth.social.enterprise;
import com.xiongx9527.fw.plus.auth.social.*;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 企业微信（WECHAT_WORK）扫码登录授权。
 *
 * <p>默认「网页授权登录」（在应用内打开网页场景）：gettoken 取应用 token → user/getuserinfo 用 code 换 UserId →
 * user/get 补全姓名/邮箱。配置项：clientId=corpid、clientSecret=corpsecret、
 * extra(agent_id)、extra(api_base 可选，私有化部署)。</p>
 */
public class WechatWorkSocialLoginProvider extends AbstractSocialLoginProvider {

    private static final String DEFAULT_BASE = "https://qyapi.weixin.qq.com";

    @Override
    public IdpType type() {
        return IdpType.WECHAT_WORK;
    }

    @Override
    public String authorizationUrl(IdpConfig config, AuthorizationUrlRequest request) {
        String corpid = config.getClientId();
        String agentId = config.extra("agent_id");
        String redirectUri = request.getRedirectUri() != null ? request.getRedirectUri() : config.getRedirectUri();
        String scope = first(config.extra("scope"), "snsapi_base");
        // 授权端点：优先用配置中的 authorizeUrl（如企业微信「网页授权登录」open.weixin.qq.com/connect/oauth2/authorize），
        // 否则默认网页授权登录（在应用内打开网页场景）。
        String endpoint = config.getAuthorizeUrl();
        if (endpoint == null || endpoint.isEmpty()) {
            endpoint = "https://open.weixin.qq.com/connect/oauth2/authorize";
        }
        StringBuilder url = new StringBuilder(endpoint);
        url.append(endpoint.contains("?") ? "&" : "?");
        url.append("appid=").append(urlEncode(corpid))
                .append("&redirect_uri=").append(urlEncode(redirectUri))
                .append("&response_type=code")
                .append("&scope=").append(urlEncode(scope));
        // 扫码登录需要 agentid；网页授权无需
        if (agentId != null && endpoint.contains("qrconnect")) {
            url.append("&agentid=").append(urlEncode(agentId));
        }
        if (request.getState() != null) {
            url.append("&state=").append(urlEncode(request.getState()));
        }
        // 微信网页授权要求以 #wechat_redirect 结尾
        if (endpoint.contains("open.weixin.qq.com/connect/oauth2/authorize") && url.indexOf("#") < 0) {
            url.append("#wechat_redirect");
        }
        return url.toString();
    }

    @Override
    public ClientInfo clientInfo(IdpConfig config) {
        ClientInfo info = new ClientInfo();
        info.setCorpId(config.getClientId());
        info.setAgentId(config.extra("agent_id"));
        info.setRedirectUri(config.getRedirectUri());
        info.setApiBase(config.extra("api_base"));
        return info;
    }

    @Override
    public ExternalIdentity resolveIdentity(IdpConfig config, IdpCallback callback) {
        String base = base(config, DEFAULT_BASE);
        String token = getToken(base, config.getClientId(), config.getClientSecret());
        JSONObject info = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/cgi-bin/user/getuserinfo?access_token=" + urlEncode(token)
                        + "&code=" + urlEncode(callback.getCode()), null));
        checkResponse(info, "wecom user/getuserinfo");
        String userId = first(info.getStr("userid"), info.getStr("UserId"));
        if (userId == null || userId.isEmpty()) {
            throw new IllegalStateException("wecom login failed: userid not found; resp=" + info);
        }
        JSONObject detail = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/cgi-bin/user/get?access_token=" + urlEncode(token)
                        + "&userid=" + urlEncode(userId), null));
        checkResponse(detail, "wecom user/get");
        String name = first(detail.getStr("name"), detail.getStr("alias"), userId);
        return new ExternalIdentity(userId, name, detail.getStr("email"), userId, null);
    }

    private String getToken(String base, String corpid, String corpsecret) {
        JSONObject t = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/cgi-bin/gettoken?corpid=" + urlEncode(corpid)
                        + "&corpsecret=" + urlEncode(corpsecret), null));
        checkResponse(t, "wecom gettoken");
        return t.getStr("access_token");
    }
}
