package com.xiongx9527.fw.plus.auth.social.enterprise;
import com.xiongx9527.fw.plus.auth.social.*;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业微信（WECHAT_WORK）扫码登录授权。
 *
 * <p>默认「网页授权登录」（在应用内打开网页场景）：gettoken 取应用 token → user/getuserinfo 用 code 换 UserId →
 * user/get 补全姓名/邮箱。配置项：clientId=corpid、clientSecret=corpsecret、
 * extra(agent_id)、extra(api_base 可选，私有化部署)。</p>
 */
public class WechatWorkSocialLoginProvider extends AbstractSocialLoginProvider implements SocialOrgTreeProvider {

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
        String corpid = config.getClientId();
        String corpsecret = config.getClientSecret();
        String code = callback.getCode();

        String token = getToken(base, config.getId(), corpid, corpsecret, false);
        JSONObject info = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/cgi-bin/user/getuserinfo?access_token=" + urlEncode(token)
                        + "&code=" + urlEncode(code), null));
        if (isTokenInvalid(info, 40014, 41001, 42001)) {
            // 从缓存拿到的 token 可能已失效：清缓存、从远程重取一次并重试
            token = getToken(base, config.getId(), corpid, corpsecret, true);
            info = JSONUtil.parseObj(SocialHttpUtils.get(
                    base + "/cgi-bin/user/getuserinfo?access_token=" + urlEncode(token)
                            + "&code=" + urlEncode(code), null));
        }
        checkResponse(info, "wecom user/getuserinfo");
        String userId = first(info.getStr("userid"), info.getStr("UserId"));
        if (userId == null || userId.isEmpty()) {
            throw new IllegalStateException("wecom login failed: userid not found; resp=" + info);
        }

        JSONObject detail = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/cgi-bin/user/get?access_token=" + urlEncode(token)
                        + "&userid=" + urlEncode(userId), null));
        if (isTokenInvalid(detail, 40014, 41001, 42001)) {
            token = getToken(base, config.getId(), corpid, corpsecret, true);
            detail = JSONUtil.parseObj(SocialHttpUtils.get(
                    base + "/cgi-bin/user/get?access_token=" + urlEncode(token)
                            + "&userid=" + urlEncode(userId), null));
        }
        checkResponse(detail, "wecom user/get");
        String name = first(detail.getStr("name"), detail.getStr("alias"), userId);
        return new ExternalIdentity(userId, name, detail.getStr("email"), userId, null);
    }

    private String getToken(String base, String configId, String corpid, String corpsecret, boolean force) {
        String key = "wecom:app:" + configId + ":" + corpid;
        if (!force) {
            String cached = tokenStore().getAppToken(key);
            if (cached != null) {
                return cached;
            }
        } else {
            tokenStore().evictAppToken(key);
        }
        JSONObject t = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/cgi-bin/gettoken?corpid=" + urlEncode(corpid)
                        + "&corpsecret=" + urlEncode(corpsecret), null));
        checkResponse(t, "wecom gettoken");
        String token = t.getStr("access_token");
        Long expiresIn = t.getLong("expires_in");
        tokenStore().putAppToken(key, token, expiresIn != null ? expiresIn : 7200L);
        return token;
    }

    @Override
    public boolean supports(IdpType type) {
        return type == IdpType.WECHAT_WORK;
    }

    @Override
    public List<SocialOrgNode> getUserTree(IdpConfig config) {
        String base = base(config, DEFAULT_BASE);
        String corpid = config.getClientId();
        String corpsecret = config.getClientSecret();
        String token = getToken(base, config.getId(), corpid, corpsecret, false);
        // 一次性拉取全部部门
        JSONObject deptResp = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/cgi-bin/department/list?access_token=" + urlEncode(token) + "&fetch_child=1", null));
        if (isTokenInvalid(deptResp, 40014, 41001, 42001)) {
            token = getToken(base, config.getId(), corpid, corpsecret, true);
            deptResp = JSONUtil.parseObj(SocialHttpUtils.get(
                    base + "/cgi-bin/department/list?access_token=" + urlEncode(token) + "&fetch_child=1", null));
        }
        checkResponse(deptResp, "wecom department/list");
        JSONArray deptArr = deptResp.getJSONArray("department");
        Map<String, SocialOrgNode> nodes = new LinkedHashMap<String, SocialOrgNode>();
        if (deptArr != null) {
            for (int i = 0; i < deptArr.size(); i++) {
                JSONObject d = deptArr.getJSONObject(i);
                SocialOrgNode n = new SocialOrgNode(String.valueOf(d.get("id")), d.getStr("name"),
                        String.valueOf(d.get("parentid")));
                nodes.put(n.getId(), n);
            }
        }
        // 逐部门拉取直属用户
        for (SocialOrgNode n : nodes.values()) {
            JSONObject uResp = JSONUtil.parseObj(SocialHttpUtils.get(
                    base + "/cgi-bin/user/list?access_token=" + urlEncode(token)
                            + "&department_id=" + urlEncode(n.getId()), null));
            checkResponse(uResp, "wecom user/list");
            JSONArray ul = uResp.getJSONArray("userlist");
            if (ul != null) {
                for (int i = 0; i < ul.size(); i++) {
                    JSONObject u = ul.getJSONObject(i);
                    n.addUser(new SocialOrgUser(u.getStr("userid"), u.getStr("name"), u.getStr("email")));
                }
            }
        }
        // 组装树
        List<SocialOrgNode> roots = new ArrayList<SocialOrgNode>();
        for (SocialOrgNode n : nodes.values()) {
            SocialOrgNode parent = nodes.get(n.getParentId());
            if (parent != null && parent != n) {
                parent.addChild(n);
            } else {
                roots.add(n);
            }
        }
        return roots;
    }

    @Override
    public SocialOrgNode getNode(IdpConfig config, String nodeId) {
        String base = base(config, DEFAULT_BASE);
        String corpid = config.getClientId();
        String corpsecret = config.getClientSecret();
        String token = getToken(base, config.getId(), corpid, corpsecret, false);
        JSONObject dResp = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/cgi-bin/department/get?access_token=" + urlEncode(token) + "&id=" + urlEncode(nodeId), null));
        if (isTokenInvalid(dResp, 40014, 41001, 42001)) {
            token = getToken(base, config.getId(), corpid, corpsecret, true);
            dResp = JSONUtil.parseObj(SocialHttpUtils.get(
                    base + "/cgi-bin/department/get?access_token=" + urlEncode(token)
                            + "&id=" + urlEncode(nodeId), null));
        }
        checkResponse(dResp, "wecom department/get");
        SocialOrgNode node = new SocialOrgNode(String.valueOf(dResp.get("id")), dResp.getStr("name"),
                String.valueOf(dResp.get("parentid")));
        JSONObject subResp = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/cgi-bin/department/list?access_token=" + urlEncode(token) + "&id=" + urlEncode(nodeId), null));
        checkResponse(subResp, "wecom department/list");
        JSONArray subs = subResp.getJSONArray("department");
        if (subs != null) {
            for (int i = 0; i < subs.size(); i++) {
                JSONObject d = subs.getJSONObject(i);
                node.addChild(new SocialOrgNode(String.valueOf(d.get("id")), d.getStr("name"), node.getId()));
            }
        }
        JSONObject uResp = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/cgi-bin/user/list?access_token=" + urlEncode(token)
                        + "&department_id=" + urlEncode(nodeId), null));
        checkResponse(uResp, "wecom user/list");
        JSONArray ul = uResp.getJSONArray("userlist");
        if (ul != null) {
            for (int i = 0; i < ul.size(); i++) {
                JSONObject u = ul.getJSONObject(i);
                node.addUser(new SocialOrgUser(u.getStr("userid"), u.getStr("name"), u.getStr("email")));
            }
        }
        return node;
    }
}
