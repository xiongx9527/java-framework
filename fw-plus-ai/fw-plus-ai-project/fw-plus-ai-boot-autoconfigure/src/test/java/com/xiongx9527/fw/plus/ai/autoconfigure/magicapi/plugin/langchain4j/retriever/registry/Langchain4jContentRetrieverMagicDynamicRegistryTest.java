package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverProviderRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.Langchain4jContentRetrieverInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.MagicDynamicLangchain4jContentRetrieverDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.resource.Langchain4jContentRetrieverMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * content-retriever 注册器：构建成功才落库、停用只清理、构建失败不注册。
 */
class Langchain4jContentRetrieverMagicDynamicRegistryTest {

    private MagicDynamicLangchain4jContentRetrieverDataSource retrievers;
    private Langchain4jContentRetrieverMagicResourceStorage storage;

    @BeforeEach
    void setUp() {
        retrievers = new MagicDynamicLangchain4jContentRetrieverDataSource();
        storage = new Langchain4jContentRetrieverMagicResourceStorage(Collections.singleton("stub"));
    }

    @Test
    void shouldHoldInstanceAfterSuccessfulRegistration() {
        assertThat(registry(query -> Collections.emptyList()).register(info(true))).isTrue();
        assertThat(retrievers.get("kb-main")).isNotNull();
    }

    @Test
    void shouldOnlyEvictWhenDisabled() {
        Langchain4jContentRetrieverInfo info = info(false);

        assertThat(registry(query -> Collections.emptyList()).register(info)).isTrue();
        assertThat(retrievers.keys()).isEmpty();
    }

    @Test
    void shouldNotRegisterWhenBuildFails() {
        ContentRetrieverProvider failing = new ContentRetrieverProvider() {
            @Override
            public String provider() {
                return "stub";
            }

            @Override
            public ContentRetriever build(Langchain4jOptionsReader options, ContentRetrieverContext context) {
                throw new IllegalArgumentException("缺少被引用的向量库");
            }
        };

        assertThat(registry(failing).register(info(true))).isFalse();
        assertThat(retrievers.keys()).isEmpty();
    }

    @Test
    void shouldRejectProviderOutsideTheTableOnSave() {
        Langchain4jContentRetrieverInfo info = info(true);
        info.setProvider("no-such-provider");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> storage.validate(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("可用 provider");
    }

    private Langchain4jContentRetrieverMagicDynamicRegistry registry(ContentRetriever instance) {
        return registry(new ContentRetrieverProvider() {
            @Override
            public String provider() {
                return "stub";
            }

            @Override
            public ContentRetriever build(Langchain4jOptionsReader options, ContentRetrieverContext context) {
                return instance;
            }
        });
    }

    private Langchain4jContentRetrieverMagicDynamicRegistry registry(ContentRetrieverProvider provider) {
        ContentRetrieverProviderRegistry providers = new ContentRetrieverProviderRegistry();
        providers.register(provider);
        ContentRetrieverContext context = new ContentRetrieverContext(null, null, null, retrievers, null, null);
        return new Langchain4jContentRetrieverMagicDynamicRegistry(storage, retrievers, providers, context, null);
    }

    private static Langchain4jContentRetrieverInfo info(boolean enabled) {
        Langchain4jContentRetrieverInfo info = new Langchain4jContentRetrieverInfo();
        info.setId("kb-main");
        info.setKey("kb-main");
        info.setProvider("stub");
        info.setEnabled(enabled);
        info.setOptions(new LinkedHashMap<String, Object>());
        info.setCreateTime(1L);
        info.setUpdateTime(1L);
        return info;
    }
}
