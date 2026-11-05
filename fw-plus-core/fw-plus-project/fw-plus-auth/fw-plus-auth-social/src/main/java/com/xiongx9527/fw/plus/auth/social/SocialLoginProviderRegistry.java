package com.xiongx9527.fw.plus.auth.social;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import com.xiongx9527.fw.plus.auth.social.enterprise.SocialOrgTreeProvider;

/**
 * 按 {@link IdpType} 注册并查找 {@link SocialLoginProvider}。
 */
public class SocialLoginProviderRegistry {

    private final Map<IdpType, SocialLoginProvider> providers = new EnumMap<IdpType, SocialLoginProvider>(IdpType.class);

    public void register(SocialLoginProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider must not be null");
        }
        providers.put(provider.type(), provider);
    }

    public SocialLoginProvider get(IdpType type) {
        return providers.get(type);
    }

    /**
     * 取得支持组织用户树的 provider（企业微信/钉钉/飞书/Lark/LDAP）。不支持时返回 {@code null}。
     */
    public SocialOrgTreeProvider getOrgTreeProvider(IdpType type) {
        SocialLoginProvider p = providers.get(type);
        return p instanceof SocialOrgTreeProvider ? (SocialOrgTreeProvider) p : null;
    }

    public boolean supports(IdpType type) {
        return providers.containsKey(type);
    }

    public Set<IdpType> types() {
        return Collections.unmodifiableSet(providers.keySet());
    }

    public Collection<SocialLoginProvider> all() {
        return Collections.unmodifiableCollection(providers.values());
    }
}
