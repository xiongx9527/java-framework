package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.web;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class MagicVectorController extends MagicController implements MagicExceptionHandler {

    private final AiEmbeddingStoreFactory embeddingStoreFactory;

    private final List<AiEmbeddingStoreAdapter> providerAdapters;

    public MagicVectorController(MagicConfiguration configuration) {
        this(configuration, null, null);
    }

    public MagicVectorController(MagicConfiguration configuration,
                                 AiEmbeddingStoreFactory embeddingStoreFactory,
                                 List<AiEmbeddingStoreAdapter> providerAdapters) {
        super(configuration);
        this.embeddingStoreFactory = embeddingStoreFactory;
        this.providerAdapters = providerAdapters;
    }

    @RequestMapping("/vector/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody AiEmbeddingStoreDefinition definition) {
        try {
            validate(definition);
            getEmbeddingStoreFactory().create(definition, getProviderAdapters());
            return new JsonBean<>("ok");
        } catch (IllegalArgumentException e) {
            log.error("测试 Vector 数据源参数无效，数据源 key: {}",
                    definition != null ? definition.getKey() : "unknown");
            return failure(e.getMessage());
        } catch (Exception e) {
            log.error("测试 Vector 数据源失败，数据源 key: {}, causeType: {}",
                    definition != null ? definition.getKey() : "unknown", e.getClass().getName());
            return failure("向量数据源测试失败");
        }
    }

    private void validate(AiEmbeddingStoreDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("vector datasource must not be null");
        }
        validateRequired(definition.getKey(), "key");
        validateRequired(definition.getProvider(), "provider");
    }

    private void validateRequired(String value, String field) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private AiEmbeddingStoreFactory getEmbeddingStoreFactory() {
        if (embeddingStoreFactory != null) {
            return embeddingStoreFactory;
        }
        return SpringUtils.getBean(AiEmbeddingStoreFactory.class);
    }

    private List<AiEmbeddingStoreAdapter> getProviderAdapters() {
        if (providerAdapters != null) {
            return providerAdapters;
        }
        Map<String, AiEmbeddingStoreAdapter> adapters = SpringUtils.getBeansOfType(AiEmbeddingStoreAdapter.class);
        return adapters == null ? Collections.<AiEmbeddingStoreAdapter>emptyList() :
                new ArrayList<AiEmbeddingStoreAdapter>(adapters.values());
    }

    private JsonBean<Object> failure(String message) {
        String failureMessage = message == null || message.trim().length() == 0 ? "操作失败" : message;
        return new JsonBean<Object>(-1, failureMessage, null);
    }
}
