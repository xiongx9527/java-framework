package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.openai.Langchain4jOpenAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jChatModelMagicResourceStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.event.EventAction;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.model.Group;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文件事件生命周期：走 magic-api 的事件入口 {@code processEvent}，验证
 * 「新增/保存 → 实例可用」「删除 → 实例消失」「停用 → 只驱逐」。
 *
 * <p>{@code processEvent} 是 protected 且本测试与其同包，可直接驱动，等价于
 * {@code @EventListener} 的 {@code onFileEvent} 路径（后者只是转调它）。</p>
 */
class Langchain4jFileEventLifecycleTest {

    private MagicDynamicLangchain4jChatModelDataSource holder;
    private Langchain4jChatModelMagicDynamicRegistry registry;

    @BeforeEach
    void setUp() {
        holder = new MagicDynamicLangchain4jChatModelDataSource();
        Langchain4jModelFactoryRegistry factories = new Langchain4jModelFactoryRegistry();
        factories.register(new Langchain4jOpenAiModelFactory());
        registry = new Langchain4jChatModelMagicDynamicRegistry(
                new Langchain4jChatModelMagicResourceStorage(factories.providers(Langchain4jModelTypes.CHAT_MODEL)),
                holder, factories);
    }

    @Test
    void shouldExposeInstanceOnCreateAndSaveEvents() {
        Langchain4jChatModelInfo info = chatModelInfo("openai-main");

        registry.onFileEvent(new FileEvent(Langchain4jModelTypes.CHAT_MODEL.type(), EventAction.CREATE, info));
        assertThat(holder.get("openai-main")).as("新增事件后实例可用").isNotNull();

        info.setUpdateTime(2L);
        registry.onFileEvent(new FileEvent(Langchain4jModelTypes.CHAT_MODEL.type(), EventAction.SAVE, info));
        assertThat(holder.get("openai-main")).as("保存事件后实例可用").isNotNull();
    }

    @Test
    void shouldDropInstanceOnDeleteEvent() {
        Langchain4jChatModelInfo info = chatModelInfo("openai-main");
        registry.onFileEvent(new FileEvent(Langchain4jModelTypes.CHAT_MODEL.type(), EventAction.CREATE, info));
        assertThat(holder.keys()).contains("openai-main");

        registry.onFileEvent(new FileEvent(Langchain4jModelTypes.CHAT_MODEL.type(), EventAction.DELETE, info));

        assertThat(holder.keys()).as("删除事件后实例必须消失").doesNotContain("openai-main");
        assertThat(holder.get("openai-main")).isNull();
        assertThat(registry.mappings()).as("删除事件后映射必须清空").isEmpty();
    }

    @Test
    void shouldOnlyEvictWhenDisabledIsSaved() {
        Langchain4jChatModelInfo info = chatModelInfo("openai-main");
        registry.onFileEvent(new FileEvent(Langchain4jModelTypes.CHAT_MODEL.type(), EventAction.CREATE, info));

        info.setEnabled(false);
        info.setUpdateTime(3L);
        registry.onFileEvent(new FileEvent(Langchain4jModelTypes.CHAT_MODEL.type(), EventAction.SAVE, info));

        assertThat(holder.keys()).as("停用后不再持有实例").doesNotContain("openai-main");
    }

    @Test
    void shouldNotResurrectInstanceOnGroupDeleteEvent() {
        Langchain4jChatModelInfo info = chatModelInfo("openai-main");
        registry.onFileEvent(new FileEvent(Langchain4jModelTypes.CHAT_MODEL.type(), EventAction.CREATE, info));

        Group group = new Group();
        group.setId("g1");
        group.setPath("/");
        registry.onGroupEvent(new GroupEvent(Langchain4jModelTypes.CHAT_MODEL.type(), EventAction.DELETE,
                group, Collections.<MagicEntity>singletonList(info)));

        assertThat(holder.keys()).as("分组删除后实例必须消失").doesNotContain("openai-main");
    }

    private static Langchain4jChatModelInfo chatModelInfo(String key) {
        Langchain4jChatModelInfo info = new Langchain4jChatModelInfo();
        info.setId(key);
        info.setKey(key);
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
}
