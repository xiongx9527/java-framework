package com.xiongx9527.fw.plus.auth.social;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 身份提供方回调参数。
 */
public class IdpCallback implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String state;
    private Map<String, String> parameters = new LinkedHashMap<>();

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Map<String, String> getParameters() { return parameters; }
    public void setParameters(Map<String, String> parameters) { this.parameters = parameters == null ? new LinkedHashMap<String, String>() : parameters; }

    public String parameter(String key) {
        String value = parameters.get(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
