package com.xiongx9527.fw.plus.ai.autoconfigure.spring.adapter;

import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.spring.adapter.SpringAiModelAdapter;
import com.xiongx9527.fw.plus.ai.spring.adapter.SpringAiModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.spring.adapter.SpringAiProviderBinding;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class SpringAiModelAdapterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpringAiBeanCatalog springAiBeanCatalog(ListableBeanFactory beanFactory) {
        return new SpringAiBeanCatalog(beanFactory);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.ai.chat.model.ChatModel")
    @ConditionalOnMissingBean(name = "springAiChatModelProviderAdapter")
    public SpringAiModelProviderAdapter springAiChatModelProviderAdapter(SpringAiBeanCatalog catalog,
            List<SpringAiProviderBinding> bindings) {
        return new BeanBackedSpringAiModelProviderAdapter(SpringAiModelAdapter.FRAMEWORK, AiModelType.CHAT,
                catalog, bindings);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.ai.embedding.EmbeddingModel")
    @ConditionalOnMissingBean(name = "springAiEmbeddingModelProviderAdapter")
    public SpringAiModelProviderAdapter springAiEmbeddingModelProviderAdapter(SpringAiBeanCatalog catalog,
            List<SpringAiProviderBinding> bindings) {
        return new BeanBackedSpringAiModelProviderAdapter(SpringAiModelAdapter.FRAMEWORK, AiModelType.EMBEDDING,
                catalog, bindings);
    }

    @Bean
    @ConditionalOnMissingBean(name = "springAiModelAdapter")
    public AiModelAdapter springAiModelAdapter(List<SpringAiModelProviderAdapter> providers) {
        return new SpringAiModelAdapter(providers);
    }
}
