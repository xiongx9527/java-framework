package com.xiongx9527.fw.plus.ai.autoconfigure.spring.adapter;

import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.spring.adapter.SpringAiAlibabaModelAdapter;
import com.xiongx9527.fw.plus.ai.spring.adapter.SpringAiModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.spring.adapter.SpringAiProviderBinding;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "fw.plus.ai.spring.alibaba", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@ConditionalOnClass(name = "com.alibaba.cloud.ai.dashscope.api.DashScopeApi")
@Conditional(SpringAiAlibabaModelAvailableCondition.class)
public class SpringAiAlibabaModelAdapterAutoConfiguration {

    @Bean
    @ConditionalOnClass(name = "com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel")
    @ConditionalOnMissingBean(name = "springAiAlibabaChatModelProviderAdapter")
    public SpringAiModelProviderAdapter springAiAlibabaChatModelProviderAdapter(SpringAiBeanCatalog catalog,
            List<SpringAiProviderBinding> bindings) {
        return new BeanBackedSpringAiModelProviderAdapter(SpringAiAlibabaModelAdapter.FRAMEWORK, AiModelType.CHAT,
                catalog, bindings);
    }

    @Bean
    @ConditionalOnClass(name = "com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel")
    @ConditionalOnMissingBean(name = "springAiAlibabaEmbeddingModelProviderAdapter")
    public SpringAiModelProviderAdapter springAiAlibabaEmbeddingModelProviderAdapter(SpringAiBeanCatalog catalog,
            List<SpringAiProviderBinding> bindings) {
        return new BeanBackedSpringAiModelProviderAdapter(SpringAiAlibabaModelAdapter.FRAMEWORK,
                AiModelType.EMBEDDING, catalog, bindings);
    }

    @Bean
    @ConditionalOnMissingBean(name = "springAiAlibabaModelAdapter")
    public AiModelAdapter springAiAlibabaModelAdapter(List<SpringAiModelProviderAdapter> providers) {
        return new SpringAiAlibabaModelAdapter(providers);
    }
}
