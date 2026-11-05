package com.xiongx9527.fw.plus.auth.social;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 构造授权 URL 的请求。
 */
public class AuthorizationUrlRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String state;
    private String redirectUri;
    private Map<String, String> extra = new LinkedHashMap<>();

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    public Map<String, String> getExtra() { return extra; }
    public void setExtra(Map<String, String> extra) { this.extra = extra == null ? new LinkedHashMap<String, String>() : extra; }
}
