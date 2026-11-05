package com.xiongx9527.fw.plus.auth.social;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 身份提供方配置（与具体存储介质解耦，可由数据库、yaml 或配置中心提供）。
 */
public class IdpConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private IdpType type;
    private String displayName;
    private boolean enabled = true;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authorizeUrl;
    private String tokenUrl;
    private String userInfoUrl;
    private String logoutUrl;
    private Map<String, String> extra = new LinkedHashMap<>();

    /** 取扩展参数，去除首尾空白；为空或空串返回 null。 */
    public String extra(String key) {
        String value = extra.get(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public boolean hasExtra(String key) {
        return extra(key) != null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public IdpType getType() { return type; }
    public void setType(IdpType type) { this.type = type; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    public String getAuthorizeUrl() { return authorizeUrl; }
    public void setAuthorizeUrl(String authorizeUrl) { this.authorizeUrl = authorizeUrl; }
    public String getTokenUrl() { return tokenUrl; }
    public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }
    public String getUserInfoUrl() { return userInfoUrl; }
    public void setUserInfoUrl(String userInfoUrl) { this.userInfoUrl = userInfoUrl; }
    public String getLogoutUrl() { return logoutUrl; }
    public void setLogoutUrl(String logoutUrl) { this.logoutUrl = logoutUrl; }
    public Map<String, String> getExtra() { return extra; }
    public void setExtra(Map<String, String> extra) { this.extra = extra == null ? new LinkedHashMap<String, String>() : extra; }
}
