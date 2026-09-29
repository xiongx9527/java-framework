package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * storage 校验契约（spec「key 校验」「provider 短名表校验」）：key 规则、provider 必填与短名表、
 * key 唯一性（同 id 更新自身放行、不同 id 同 key 拒绝）。
 */
class Langchain4jModelResourceStorageValidationTest {

    private Langchain4jChatModelMagicResourceStorage storage;
    private MagicResourceService magicResourceService;
    private Langchain4jModelFactoryRegistry factories;

    @BeforeEach
    void setUp() {
        factories = new Langchain4jModelFactoryRegistry();
        factories.register(new com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory
                .openai.Langchain4jOpenAiModelFactory());
        storage = new Langchain4jChatModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.CHAT_MODEL));
        magicResourceService = mock(MagicResourceService.class);
        storage.setMagicResourceService(magicResourceService);
    }

    @Test
    void shouldRejectBlankKey() {
        Langchain4jChatModelInfo info = chatModel("  ");

        assertThatThrownBy(() -> storage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key不能为空");
    }

    @Test
    void shouldRejectIllegalKeyCharacters() {
        Langchain4jChatModelInfo info = chatModel("bad key!");

        assertThatThrownBy(() -> storage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key只能包含字母、数字、下划线、点与中划线");
    }

    @Test
    void shouldRejectMissingProvider() {
        Langchain4jChatModelInfo info = chatModel("openai-main");
        info.setProvider(null);

        assertThatThrownBy(() -> storage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider不能为空");
    }

    @Test
    void shouldRejectUnknownProviderAndListAvailableOnes() {
        Langchain4jChatModelInfo info = chatModel("openai-main");
        info.setProvider("not-exist");

        assertThatThrownBy(() -> storage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider 'not-exist' 不受支持")
                .hasMessageContaining("openai");
    }

    @Test
    void shouldRejectDuplicateKeyFromOtherEntity() {
        Langchain4jChatModelInfo existing = chatModel("openai-main");
        existing.setId("other-id");
        when(magicResourceService.listFiles(anyString()))
                .thenReturn(Collections.singletonList((MagicEntity) existing));

        Langchain4jChatModelInfo info = chatModel("openai-main");
        info.setId("new-id");

        assertThatThrownBy(() -> storage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key 'openai-main' 已存在");
    }

    @Test
    void shouldAllowUpdatingSameEntity() {
        Langchain4jChatModelInfo existing = chatModel("openai-main");
        existing.setId("same-id");
        when(magicResourceService.listFiles(anyString()))
                .thenReturn(Arrays.asList((MagicEntity) existing, existing));

        Langchain4jChatModelInfo info = chatModel("openai-main");
        info.setId("same-id");
        info.setUpdateTime(2L);

        assertThatCode(() -> storage.validate(info)).doesNotThrowAnyException();
    }

    @Test
    void shouldPassValidationWithoutResourceServiceBound() {
        // 未绑定资源服务时跳过唯一性校验，其余校验（key 规则、provider 短名表）照常执行
        Langchain4jChatModelMagicResourceStorage unbound = new Langchain4jChatModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.CHAT_MODEL));
        unbound.validate(chatModel("openai-main"));
    }

    private static Langchain4jChatModelInfo chatModel(String key) {
        Langchain4jChatModelInfo info = new Langchain4jChatModelInfo();
        info.setId(UUID_NAME(key));
        info.setKey(key);
        info.setName(key);
        info.setProvider("openai");
        info.setEnabled(true);
        info.setCreateTime(1L);
        info.setUpdateTime(1L);
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("api-key", "sk-test");
        options.put("model-name", "gpt-4o-mini");
        info.setOptions(options);
        return info;
    }

    private static String UUID_NAME(String key) {
        return "id-" + key;
    }
}
