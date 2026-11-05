package com.xiongx9527.fw.plus.auth.social;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 解析出的外部身份（不可变）。
 */
public class ExternalIdentity implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String account;
    private final String name;
    private final String email;
    private final String externalId;
    private final Map<String, Object> attributes;

    public ExternalIdentity(String account, String name, String email, String externalId, Map<String, Object> attributes) {
        this.account = account;
        this.name = name;
        this.email = email;
        this.externalId = externalId;
        this.attributes = attributes == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(attributes));
    }

    public ExternalIdentity(String account, String externalId) {
        this(account, null, null, externalId, null);
    }

    public String getAccount() { return account; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getExternalId() { return externalId; }
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public String toString() {
        return "ExternalIdentity{account='" + account + "', name='" + name + "', email='" + email
                + "', externalId='" + externalId + "'}";
    }
}
