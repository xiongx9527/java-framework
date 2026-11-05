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
 * 飞书 / LarkSuite（FEISHU / LARK）扫码登录授权。
 *
 * <p>标准 OAuth2：app_access_token 取应用令牌 → oidc/access_token 用 code 换用户令牌 →
 * user_info 取资料。LarkSuite 与飞书协议同构，仅 base 不同（open.larksuite.com）。</p>
 *
 * <p>配置项：clientId=App ID、clientSecret=App Secret。</p>
 */
public class FeishuSocialLoginProvider extends AbstractSocialLoginProvider implements SocialOrgTreeProvider {

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
        String appId = config.getClientId();
        String appSecret = config.getClientSecret();
        String code = callback.getCode();

        String appToken = fetchAppAccessToken(base, config.getId(), appId, appSecret, false);
        String userToken = fetchUserAccessToken(base, appToken, code);
        JSONObject infoRes = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/open-apis/authen/v1/user_info", authHeader(userToken)));
        if (isTokenInvalid(infoRes, 99991663, 99991664, 99991668)) {
            // 从缓存拿到的 app token 可能已失效：清缓存、从远程重取 app token 并重新换取用户 token，重试一次
            appToken = fetchAppAccessToken(base, config.getId(), appId, appSecret, true);
            userToken = fetchUserAccessToken(base, appToken, code);
            infoRes = JSONUtil.parseObj(SocialHttpUtils.get(
                    base + "/open-apis/authen/v1/user_info", authHeader(userToken)));
        }
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

    private String fetchAppAccessToken(String base, String configId, String appId, String appSecret, boolean force) {
        String key = "feishu:app:" + configId + ":" + appId;
        if (!force) {
            String cached = tokenStore().getAppToken(key);
            if (cached != null) {
                return cached;
            }
        } else {
            tokenStore().evictAppToken(key);
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("app_id", appId);
        body.put("app_secret", appSecret);
        JSONObject res = JSONUtil.parseObj(SocialHttpUtils.postJson(
                base + "/open-apis/auth/v3/app_access_token/internal", JSONUtil.toJsonStr(body), null));
        String token = first(res.getStr("app_access_token"), res.getStr("tenant_access_token"));
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("feishu login failed: app_access_token not found; resp=" + res);
        }
        Long expire = res.getLong("expire");
        tokenStore().putAppToken(key, token, expire != null ? expire : 7200L);
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

    @Override
    public boolean supports(IdpType type) {
        return type == IdpType.FEISHU || type == IdpType.LARK;
    }

    @Override
    public List<SocialOrgNode> getUserTree(IdpConfig config) {
        String base = base(config, defaultBase());
        String appId = config.getClientId();
        String appSecret = config.getClientSecret();
        String appToken = fetchAppAccessToken(base, config.getId(), appId, appSecret, false);
        JSONObject probe = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/open-apis/contact/v3/departments?department_id_type=open_department_id"
                        + "&page_size=50&parent_department_id=" + urlEncode("0"), authHeader(appToken)));
        if (isTokenInvalid(probe, 99991663, 99991664, 99991668)) {
            appToken = fetchAppAccessToken(base, config.getId(), appId, appSecret, true);
        }
        SocialOrgNode root = new SocialOrgNode("0", "组织", null);
        fillFeishuNode(base, appToken, "0", root);
        List<SocialOrgNode> roots = new ArrayList<SocialOrgNode>();
        roots.add(root);
        return roots;
    }

    @Override
    public SocialOrgNode getNode(IdpConfig config, String nodeId) {
        String base = base(config, defaultBase());
        String appId = config.getClientId();
        String appSecret = config.getClientSecret();
        String appToken = fetchAppAccessToken(base, config.getId(), appId, appSecret, false);
        JSONObject dResp = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/open-apis/contact/v3/departments/" + urlEncode(nodeId)
                        + "?department_id_type=open_department_id", authHeader(appToken)));
        if (isTokenInvalid(dResp, 99991663, 99991664, 99991668)) {
            appToken = fetchAppAccessToken(base, config.getId(), appId, appSecret, true);
            dResp = JSONUtil.parseObj(SocialHttpUtils.get(
                    base + "/open-apis/contact/v3/departments/" + urlEncode(nodeId)
                            + "?department_id_type=open_department_id", authHeader(appToken)));
        }
        JSONObject dd = dResp.getJSONObject("data");
        SocialOrgNode node = new SocialOrgNode(nodeId, dd == null ? null : dd.getStr("name"), null);

        JSONObject subResp = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/open-apis/contact/v3/departments?department_id_type=open_department_id"
                        + "&page_size=50&parent_department_id=" + urlEncode(nodeId), authHeader(appToken)));
        JSONObject sdata = subResp.getJSONObject("data");
        JSONArray subs = sdata == null ? null : sdata.getJSONArray("items");
        if (subs != null) {
            for (int i = 0; i < subs.size(); i++) {
                JSONObject d = subs.getJSONObject(i);
                node.addChild(new SocialOrgNode(d.getStr("department_id"), d.getStr("name"), node.getId()));
            }
        }
        JSONObject uResp = JSONUtil.parseObj(SocialHttpUtils.get(
                base + "/open-apis/contact/v3/users?department_id_type=open_department_id"
                        + "&user_id_type=union_id&page_size=50&department_id=" + urlEncode(nodeId), authHeader(appToken)));
        JSONObject udata = uResp.getJSONObject("data");
        JSONArray ul = udata == null ? null : udata.getJSONArray("items");
        if (ul != null) {
            for (int i = 0; i < ul.size(); i++) {
                JSONObject u = ul.getJSONObject(i);
                node.addUser(new SocialOrgUser(u.getStr("user_id"), u.getStr("name"), u.getStr("email")));
            }
        }
        return node;
    }

    private void fillFeishuNode(String base, String appToken, String parentDeptId, SocialOrgNode node) {
        String deptUrl = base + "/open-apis/contact/v3/departments?department_id_type=open_department_id"
                + "&page_size=50&parent_department_id=" + urlEncode(parentDeptId);
        String pageToken = null;
        do {
            JSONObject dResp = JSONUtil.parseObj(SocialHttpUtils.get(
                    deptUrl + (pageToken != null ? "&page_token=" + urlEncode(pageToken) : ""), authHeader(appToken)));
            JSONObject dData = dResp.getJSONObject("data");
            JSONArray items = dData == null ? null : dData.getJSONArray("items");
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    JSONObject d = items.getJSONObject(i);
                    SocialOrgNode child = new SocialOrgNode(d.getStr("department_id"), d.getStr("name"), node.getId());
                    node.addChild(child);
                    fillFeishuNode(base, appToken, child.getId(), child);
                }
            }
            pageToken = nextPageToken(dData);
        } while (pageToken != null);

        String userUrl = base + "/open-apis/contact/v3/users?department_id_type=open_department_id"
                + "&user_id_type=union_id&page_size=50&department_id=" + urlEncode(parentDeptId);
        String upage = null;
        do {
            JSONObject uResp = JSONUtil.parseObj(SocialHttpUtils.get(
                    userUrl + (upage != null ? "&page_token=" + urlEncode(upage) : ""), authHeader(appToken)));
            JSONObject uData = uResp.getJSONObject("data");
            JSONArray uitems = uData == null ? null : uData.getJSONArray("items");
            if (uitems != null) {
                for (int i = 0; i < uitems.size(); i++) {
                    JSONObject u = uitems.getJSONObject(i);
                    node.addUser(new SocialOrgUser(u.getStr("user_id"), u.getStr("name"), u.getStr("email")));
                }
            }
            upage = nextPageToken(uData);
        } while (upage != null);
    }

    private static String nextPageToken(JSONObject data) {
        if (data == null || !Boolean.TRUE.equals(data.getBool("has_more"))) {
            return null;
        }
        return data.getStr("page_token");
    }
}
