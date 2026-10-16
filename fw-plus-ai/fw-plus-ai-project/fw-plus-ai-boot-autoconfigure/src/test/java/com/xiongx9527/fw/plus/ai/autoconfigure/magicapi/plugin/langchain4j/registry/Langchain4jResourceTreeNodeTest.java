package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.openai.Langchain4jOpenAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jChatModelMagicResourceStorage;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.event.EventAction;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 控制台资源树列表节点：{@code MagicResourceController} 用 {@code listFiles → simple()}
 * 生成树节点，用 {@code defaultMappings()} 追加无文件的默认实例。
 *
 * <p>两处都必须满足 magic-editor 前端 {@code magic-data-resource} 的取值假设：</p>
 *
 * <ul>
 *   <li>节点 {@code name}、{@code key} 均非 null —— 过滤逻辑 {@code S.name.indexOf(...)}
 *       在 null 上抛 TypeError 会让整个面板停止渲染；</li>
 *   <li>{@code defaultMappings()} 只含无文件条目 —— 文件条目已由 {@code listFiles} 列出，
 *       再叠加 {@code mappings()} 会出现同一 JSON 重复 3 次（核心按 id/mappingKey 双键登记，
 *       {@code mappings()} 遍历 values 本身就会翻倍）。</li>
 * </ul>
 */
class Langchain4jResourceTreeNodeTest {

    @Test
    void simpleNodeCarriesNameAndKeyForModelInfo() {
        Langchain4jChatModelInfo info = new Langchain4jChatModelInfo();
        info.setId("gemma4");
        info.setName("gemma4");
        info.setKey("gemma4");
        info.setProvider("openai");
        info.setEnabled(true);
        info.setOptions(new LinkedHashMap<String, Object>());

        MagicEntity simple = info.simple();

        assertThat(simple).isInstanceOf(Langchain4jChatModelInfo.class);
        assertThat(simple.getName()).as("节点 name 必须随 simple() 带出").isEqualTo("gemma4");
        assertThat(((Langchain4jChatModelInfo) simple).getKey()).as("过滤逻辑还会读 S.key").isEqualTo("gemma4");
        assertThat(((Langchain4jChatModelInfo) simple).getProvider()).isEqualTo("openai");
        assertThat(((Langchain4jChatModelInfo) simple).isEnabled()).isTrue();
    }

    @Test
    void defaultMappingsMustNotRepeatFileBackedEntries() {
        Langchain4jModelFactoryRegistry factories = new Langchain4jModelFactoryRegistry();
        factories.register(new Langchain4jOpenAiModelFactory());
        Langchain4jChatModelMagicDynamicRegistry registry = new Langchain4jChatModelMagicDynamicRegistry(
                new Langchain4jChatModelMagicResourceStorage(factories.providers(Langchain4jModelTypes.CHAT_MODEL)),
                new MagicDynamicLangchain4jChatModelDataSource(), factories, providerOf(null));

        Langchain4jChatModelInfo info = new Langchain4jChatModelInfo();
        info.setId("gemma4");
        info.setName("gemma4");
        info.setKey("gemma4");
        info.setProvider("openai");
        info.setEnabled(true);
        info.setCreateTime(1L);
        info.setUpdateTime(1L);
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("api-key", "sk-test");
        options.put("model-name", "gpt-4o-mini");
        info.setOptions(options);

        registry.onFileEvent(new FileEvent(Langchain4jModelTypes.CHAT_MODEL.type(), EventAction.CREATE, info));

        assertThat(registry.mappings()).as("文件实体已登记").isNotEmpty();
        assertThat(registry.defaultMappings())
                .as("容器无 Spring 默认实例时 defaultMappings 应为空：文件条目由控制器 listFiles 提供")
                .isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static <T> org.springframework.beans.factory.ObjectProvider<T> providerOf(T instance) {
        org.springframework.beans.factory.ObjectProvider<T> provider =
                org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(instance);
        return provider;
    }
}
