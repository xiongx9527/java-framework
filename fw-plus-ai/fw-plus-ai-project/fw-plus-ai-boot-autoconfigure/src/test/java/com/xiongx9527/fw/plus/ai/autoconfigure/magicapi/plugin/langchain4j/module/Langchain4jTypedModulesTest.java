package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jEmbeddingModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jEmbeddingStoreModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jLanguageModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingChatModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingLanguageModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jTokenizerModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.script.functions.DynamicAttribute;
import org.ssssssss.magicapi.modules.DynamicModule;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * 七类脚本模块（每类一个，脚本按 key 直接取模型实例）：chatModel['qwen-main'] 等写法；
 * 未配置 key 时抛中文异常并列出该类型已配置的 key；无默认数据源概念，getDynamicModule 返回自身。
 */
class Langchain4jTypedModulesTest {

    @Test
    void shouldExposeModuleNamesPerType() {
        assertThat(annotationValue(Langchain4jChatModelModule.class)).isEqualTo("langchain4jChatModel");
        assertThat(annotationValue(Langchain4jStreamingChatModelModule.class)).isEqualTo("langchain4jStreamingChatModel");
        assertThat(annotationValue(Langchain4jLanguageModelModule.class)).isEqualTo("langchain4jLanguageModel");
        assertThat(annotationValue(Langchain4jStreamingLanguageModelModule.class)).isEqualTo("langchain4jStreamingLanguageModel");
        assertThat(annotationValue(Langchain4jEmbeddingModelModule.class)).isEqualTo("langchain4jEmbeddingModel");
        assertThat(annotationValue(Langchain4jTokenizerModule.class)).isEqualTo("langchain4jTokenizer");
        assertThat(annotationValue(Langchain4jEmbeddingStoreModule.class)).isEqualTo("langchain4jEmbeddingStore");
    }

    @Test
    void shouldImplementDynamicAttributeAndModuleContracts() {
        for (Class<?> type : Arrays.asList(Langchain4jChatModelModule.class,
                Langchain4jStreamingChatModelModule.class, Langchain4jLanguageModelModule.class,
                Langchain4jStreamingLanguageModelModule.class, Langchain4jEmbeddingModelModule.class,
                Langchain4jTokenizerModule.class, Langchain4jEmbeddingStoreModule.class)) {
            assertThat(DynamicAttribute.class).isAssignableFrom(type);
            assertThat(DynamicModule.class).isAssignableFrom(type);
        }
    }

    @Test
    void shouldReturnConfiguredInstanceByKey() {
        MagicDynamicLangchain4jChatModelDataSource holder = new MagicDynamicLangchain4jChatModelDataSource();
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        holder.put("qwen-main", model);

        Langchain4jChatModelModule module = new Langchain4jChatModelModule(holder);

        assertThat(module.getDynamicAttribute("qwen-main")).isSameAs(model);
        assertThat(module.getDynamicModule(null)).as("无默认数据源概念，返回模块自身").isSameAs(module);
    }

    @Test
    void shouldExplainMissingKeyWithConfiguredOnes() {
        MagicDynamicLangchain4jChatModelDataSource holder = new MagicDynamicLangchain4jChatModelDataSource();
        holder.put("qwen-main", mock(ChatLanguageModel.class));
        holder.put("openai-backup", mock(ChatLanguageModel.class));

        Langchain4jChatModelModule module = new Langchain4jChatModelModule(holder);

        assertThatThrownBy(() -> module.getDynamicAttribute("absent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("langchain4j-chat-model")
                .hasMessageContaining("absent")
                .hasMessageContaining("qwen-main")
                .hasMessageContaining("openai-backup");
    }

    @Test
    void shouldReturnEmbeddingModelInstanceByKey() {
        MagicDynamicLangchain4jEmbeddingModelDataSource holder = new MagicDynamicLangchain4jEmbeddingModelDataSource();
        EmbeddingModel model = mock(EmbeddingModel.class, withSettings());
        holder.put("qwen-embed", model);

        Langchain4jEmbeddingModelModule module = new Langchain4jEmbeddingModelModule(holder);

        assertThat(module.getDynamicAttribute("qwen-embed")).isSameAs(model);
        Set<String> keys = new LinkedHashSet<>(Arrays.asList("qwen-embed"));
        assertThat(holder.keys()).isEqualTo(keys.stream().collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    @Test
    void shouldExposeEmptyDefaultMappingsForConsole() {
        MagicDynamicLangchain4jChatModelDataSource holder = new MagicDynamicLangchain4jChatModelDataSource();
        holder.put("qwen-main", mock(ChatLanguageModel.class));

        Langchain4jChatModelModule module = new Langchain4jChatModelModule(holder);

        assertThat(module.defaultMappings()).as("无静态默认数据源，defaultMappings 为空").isEmpty();
    }

    @Test
    void shouldRejectSetDynamicAttributeInScripts() {
        MagicDynamicLangchain4jChatModelDataSource holder = new MagicDynamicLangchain4jChatModelDataSource();
        holder.put("qwen-main", mock(ChatLanguageModel.class));

        Langchain4jChatModelModule module = new Langchain4jChatModelModule(holder);

        assertThatThrownBy(() -> module.setDynamicAttribute("qwen-main", mock(ChatLanguageModel.class)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("脚本不支持设置模型实例");
    }

    private static String annotationValue(Class<?> type) {
        MagicModule annotation = type.getAnnotation(MagicModule.class);
        assertThat(annotation).as("%s 应标注 @MagicModule", type.getName()).isNotNull();
        return annotation.value();
    }
}
