package com.xiongx9527.fw.plus.auth.social;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

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
