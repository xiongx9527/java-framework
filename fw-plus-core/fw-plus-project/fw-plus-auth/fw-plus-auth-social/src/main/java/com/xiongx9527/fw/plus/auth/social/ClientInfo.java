package com.xiongx9527.fw.plus.auth.social;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 提供给前端的客户端信息（如企业微信 corpid/agentid、钉钉 appid 等）。
 */
public class ClientInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String corpId;
    private String agentId;
    private String clientId;
    private String redirectUri;
    private String apiBase;
    private Map<String, String> extra = new LinkedHashMap<>();

    public String extra(String key) {
        String value = extra.get(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String getCorpId() { return corpId; }
    public void setCorpId(String corpId) { this.corpId = corpId; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    public String getApiBase() { return apiBase; }
    public void setApiBase(String apiBase) { this.apiBase = apiBase; }
    public Map<String, String> getExtra() { return extra; }
    public void setExtra(Map<String, String> extra) { this.extra = extra == null ? new LinkedHashMap<String, String>() : extra; }
}
