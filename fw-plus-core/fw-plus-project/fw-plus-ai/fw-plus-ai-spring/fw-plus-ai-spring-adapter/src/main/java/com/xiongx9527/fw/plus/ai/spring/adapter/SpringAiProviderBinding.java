package com.xiongx9527.fw.plus.ai.spring.adapter;

import com.xiongx9527.fw.plus.ai.model.AiModelType;

public final class SpringAiProviderBinding {

    private final String framework;
    private final String provider;
    private final AiModelType type;
    private final String beanName;

    public SpringAiProviderBinding(String framework, String provider, AiModelType type, String beanName) {
        if (isBlank(framework)) {
            throw new IllegalArgumentException("framework cannot be blank");
        }
        if (isBlank(provider)) {
            throw new IllegalArgumentException("provider cannot be blank");
        }
        if (type != AiModelType.CHAT && type != AiModelType.EMBEDDING) {
            throw new IllegalArgumentException("type must be CHAT or EMBEDDING");
        }
        if (isBlank(beanName)) {
            throw new IllegalArgumentException("beanName cannot be blank");
        }
        this.framework = framework.trim().toLowerCase(java.util.Locale.ENGLISH);
        this.provider = provider.trim().toLowerCase(java.util.Locale.ENGLISH);
        this.type = type;
        this.beanName = beanName.trim();
    }

    public String getFramework() {
        return framework;
    }

    public String getProvider() {
        return provider;
    }

    public AiModelType getType() {
        return type;
    }

    public String getBeanName() {
        return beanName;
    }

    public boolean matches(String framework, String provider, AiModelType type) {
        return this.framework.equalsIgnoreCase(framework)
                && provider != null && this.provider.equalsIgnoreCase(provider.trim())
                && this.type == type;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
