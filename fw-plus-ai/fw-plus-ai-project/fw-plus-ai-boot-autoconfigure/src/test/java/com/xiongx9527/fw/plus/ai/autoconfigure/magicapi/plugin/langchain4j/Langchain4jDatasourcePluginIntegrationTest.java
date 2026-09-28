package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jTokenizerDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jChatModelMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jEmbeddingModelMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jEmbeddingStoreMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jLanguageModelMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jStreamingChatModelMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jStreamingLanguageModelMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jTokenizerMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jChatModelMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jEmbeddingModelMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jEmbeddingStoreMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jLanguageModelMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jStreamingChatModelMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jStreamingLanguageModelMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jTokenizerMagicResourceStorage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 插件装配集成测试：真实拉起 {@link Langchain4jDatasourceConfiguration}，验证七类资源
 * （存储 / 实例持有者 / 动态注册器）齐备，并通过注册器驱动「文件创建 → 按 key 可取」
 * 与「文件删除 → 实例消失」的完整链路。
 *
 * <p>注册器的 {@code register}/{@code unregister} 即 magic-api 文件事件（新增/更新、删除）的
 * 处理路径，因此直接驱动即可覆盖资源变更后的可用性语义。</p>
 */
class Langchain4jDatasourcePluginIntegrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(MagicResourceService.class, () -> {
                MagicResourceService service = mock(MagicResourceService.class);
                when(service.listFiles(anyString())).thenReturn(Collections.emptyList());
                return service;
            })
            .withUserConfiguration(Langchain4jDatasourceConfiguration.class);

    @Test
    void shouldExposeAllSevenResourceKinds() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(Langchain4jModelFactoryRegistry.class);

            assertThat(context).hasSingleBean(MagicDynamicLangchain4jChatModelDataSource.class);
            assertThat(context).hasSingleBean(MagicDynamicLangchain4jStreamingChatModelDataSource.class);
            assertThat(context).hasSingleBean(MagicDynamicLangchain4jLanguageModelDataSource.class);
            assertThat(context).hasSingleBean(MagicDynamicLangchain4jStreamingLanguageModelDataSource.class);
            assertThat(context).hasSingleBean(MagicDynamicLangchain4jEmbeddingModelDataSource.class);
            assertThat(context).hasSingleBean(MagicDynamicLangchain4jTokenizerDataSource.class);
            assertThat(context).hasSingleBean(MagicDynamicLangchain4jEmbeddingStoreDataSource.class);

            assertThat(context).hasSingleBean(Langchain4jChatModelMagicResourceStorage.class);
            assertThat(context).hasSingleBean(Langchain4jStreamingChatModelMagicResourceStorage.class);
            assertThat(context).hasSingleBean(Langchain4jLanguageModelMagicResourceStorage.class);
            assertThat(context).hasSingleBean(Langchain4jStreamingLanguageModelMagicResourceStorage.class);
            assertThat(context).hasSingleBean(Langchain4jEmbeddingModelMagicResourceStorage.class);
            assertThat(context).hasSingleBean(Langchain4jTokenizerMagicResourceStorage.class);
            assertThat(context).hasSingleBean(Langchain4jEmbeddingStoreMagicResourceStorage.class);

            assertThat(context).hasSingleBean(Langchain4jChatModelMagicDynamicRegistry.class);
            assertThat(context).hasSingleBean(Langchain4jStreamingChatModelMagicDynamicRegistry.class);
            assertThat(context).hasSingleBean(Langchain4jLanguageModelMagicDynamicRegistry.class);
            assertThat(context).hasSingleBean(Langchain4jStreamingLanguageModelMagicDynamicRegistry.class);
            assertThat(context).hasSingleBean(Langchain4jEmbeddingModelMagicDynamicRegistry.class);
            assertThat(context).hasSingleBean(Langchain4jTokenizerMagicDynamicRegistry.class);
            assertThat(context).hasSingleBean(Langchain4jEmbeddingStoreMagicDynamicRegistry.class);
        });
    }

    @Test
    void shouldRegisterOnCreateEventAndEvictOnDeleteEvent() {
        runner.run(context -> {
            Langchain4jChatModelMagicDynamicRegistry registry =
                    context.getBean(Langchain4jChatModelMagicDynamicRegistry.class);
            MagicDynamicLangchain4jChatModelDataSource holder =
                    context.getBean(MagicDynamicLangchain4jChatModelDataSource.class);
            assertThat(holder.keys()).isEmpty();

            Langchain4jChatModelInfo info = chatModelInfo("openai-main");

            // 文件新增/更新事件
            assertThat(registry.register(info)).isTrue();
            assertThat(holder.get("openai-main")).isNotNull();
            assertThat(holder.keys()).contains("openai-main");

            // 文件删除事件
            assertThat(registry.unregister(info)).isTrue();
            assertThat(holder.keys()).doesNotContain("openai-main");
            assertThat(holder.get("openai-main")).isNull();
        });
    }

    @Test
    void shouldNotCreateInstanceForDisabledResource() {
        runner.run(context -> {
            Langchain4jChatModelMagicDynamicRegistry registry =
                    context.getBean(Langchain4jChatModelMagicDynamicRegistry.class);
            MagicDynamicLangchain4jChatModelDataSource holder =
                    context.getBean(MagicDynamicLangchain4jChatModelDataSource.class);

            Langchain4jChatModelInfo info = chatModelInfo("openai-off");
            info.setEnabled(false);

            assertThat(registry.register(info)).isTrue();
            assertThat(holder.keys()).doesNotContain("openai-off");
        });
    }

    @Test
    void shouldFailPluginRegistrationButKeepServerSideAssemblyWhenFrontendMissing() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            // 每类资源一个插件配置：产物缺失时各自只按基础标识注册，服务端装配不受影响
            for (AbstractLangchain4jPluginConfiguration configuration
                    : context.getBeansOfType(AbstractLangchain4jPluginConfiguration.class).values()) {
                boolean frontendPresent = configuration.getClass().getClassLoader()
                        .getResource(configuration.frontendResource()) != null;
                assertThat(configuration.plugin().getJavascriptFilename())
                        .isEqualTo(frontendPresent ? configuration.javascriptFilename() : null);
            }
            assertThat(context).hasSingleBean(MagicDynamicLangchain4jChatModelDataSource.class);
        });
    }

    @Test
    void shouldDeclareOnePluginConfigurationBeanPerType() {
        runner.run(context -> {
            List<String> pluginNames =
                    context.getBeansOfType(AbstractLangchain4jPluginConfiguration.class).values().stream()
                            .map(AbstractLangchain4jPluginConfiguration::pluginName)
                            .sorted()
                            .collect(Collectors.toList());

            assertThat(pluginNames).as("七类资源各一个插件配置，magic-api 逐个注册 controller")
                    .containsExactly(
                            "langchain4j-chat-model",
                            "langchain4j-embedding-model",
                            "langchain4j-embedding-store",
                            "langchain4j-language-model",
                            "langchain4j-streaming-chat-model",
                            "langchain4j-streaming-language-model",
                            "langchain4j-tokenizer");
        });
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
