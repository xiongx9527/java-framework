package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
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
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web.Langchain4jStartupRegistrar;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.utils.Mapping;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 装配结构测试：父配置只保留共享内容，七类资源各自一个顶层子 Configuration；
 * 子配置可独立导入，单类型装配不牵连其它类型；每类资源一个插件标识（对应各自的前端产物）。
 */
class Langchain4jDatasourceConfigurationTest {

    private final ApplicationContextRunner fullRunner = new ApplicationContextRunner()
            .withBean(MagicResourceService.class, () -> mock(MagicResourceService.class))
            .withUserConfiguration(Langchain4jDatasourceConfiguration.class);

    @Test
    void shouldRegisterSevenHolders() {
        fullRunner.run(context -> {
            assertThat(context).hasSingleBean(MagicDynamicLangchain4jChatModelDataSource.class);
            assertThat(context).hasSingleBean(MagicDynamicLangchain4jStreamingChatModelDataSource.class);
            assertThat(context).hasSingleBean(MagicDynamicLangchain4jLanguageModelDataSource.class);
            assertThat(context).hasSingleBean(MagicDynamicLangchain4jStreamingLanguageModelDataSource.class);
            assertThat(context).hasSingleBean(MagicDynamicLangchain4jEmbeddingModelDataSource.class);
            assertThat(context).hasSingleBean(MagicDynamicLangchain4jTokenizerDataSource.class);
            assertThat(context).hasSingleBean(MagicDynamicLangchain4jEmbeddingStoreDataSource.class);
        });
    }

    @Test
    void shouldRegisterSevenStoragesAndRegistries() {
        fullRunner.run(context -> {
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

            assertThat(context).hasSingleBean(Langchain4jModelFactoryRegistry.class);
            assertThat(context).hasSingleBean(Langchain4jStartupRegistrar.class);
        });
    }

    @Test
    void shouldImportSharedFactoryConfigAndSevenTypeSubConfigurations() {
        Import imported = Langchain4jDatasourceConfiguration.class.getAnnotation(Import.class);
        assertThat(imported).as("父装配必须用 @Import 引入子配置").isNotNull();

        List<Class<?>> children = Arrays.asList(imported.value());
        assertThat(children).as("共享工厂配置与七类资源的子配置一并引入")
                .contains(Langchain4jModelFactoryConfiguration.class,
                        Langchain4jChatModelConfiguration.class,
                        Langchain4jStreamingChatModelConfiguration.class,
                        Langchain4jLanguageModelConfiguration.class,
                        Langchain4jStreamingLanguageModelConfiguration.class,
                        Langchain4jEmbeddingModelConfiguration.class,
                        Langchain4jTokenizerConfiguration.class,
                        Langchain4jEmbeddingStoreConfiguration.class);
        for (Class<?> child : children) {
            assertThat(child.getEnclosingClass()).as("%s 应为顶层类", child.getName()).isNull();
            assertThat(child.getAnnotation(Configuration.class))
                    .as("%s 应标注 @Configuration", child.getName()).isNotNull();
            assertThat(child.getName()).contains("Langchain4j");
        }
    }

    @Test
    void shouldDeclareOnlyItsOwnBeansPerSubConfiguration() {
        new ApplicationContextRunner()
                .withBean(Langchain4jModelFactoryRegistry.class, Langchain4jModelFactoryRegistry::new)
                .withBean(MagicDynamicLangchain4jEmbeddingModelDataSource.class,
                        MagicDynamicLangchain4jEmbeddingModelDataSource::new)
                .withUserConfiguration(Langchain4jChatModelConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MagicDynamicLangchain4jChatModelDataSource.class);
                    assertThat(context).hasSingleBean(Langchain4jChatModelMagicResourceStorage.class);
                    assertThat(context).hasSingleBean(Langchain4jChatModelMagicDynamicRegistry.class);

                    assertThat(context).doesNotHaveBean(MagicDynamicLangchain4jEmbeddingStoreDataSource.class);
                    assertThat(context).doesNotHaveBean(Langchain4jEmbeddingStoreMagicDynamicRegistry.class);
                    assertThat(context).doesNotHaveBean(Langchain4jTokenizerMagicDynamicRegistry.class);
                });
    }

    @Test
    void shouldDeclareOnePluginIdentityPerType() {
        fullRunner.run(context -> {
            List<AbstractLangchain4jPluginConfiguration> plugins =
                    context.getBeansOfType(AbstractLangchain4jPluginConfiguration.class).values().stream()
                            .collect(Collectors.toList());

            assertThat(plugins).as("七类资源各一个插件配置").hasSize(7);
            assertThat(plugins.stream().map(AbstractLangchain4jPluginConfiguration::pluginName)
                    .collect(Collectors.toList()))
                    .as("插件标识与 datasource type 同源")
                    .containsExactlyInAnyOrder(
                            "langchain4j-chat-model",
                            "langchain4j-streaming-chat-model",
                            "langchain4j-language-model",
                            "langchain4j-streaming-language-model",
                            "langchain4j-embedding-model",
                            "langchain4j-tokenizer",
                            "langchain4j-embedding-store");
            assertThat(plugins.stream().map(AbstractLangchain4jPluginConfiguration::javascriptFilename)
                    .collect(Collectors.toList()))
                    .allMatch(name -> name.startsWith("magic-langchain4j-") && name.endsWith(".1.0.0.iife.js"));
        });
    }

    @Test
    void shouldDeclarePluginIdentityMatchingTypeAndDegradeWhenFrontendMissing() {
        fullRunner.run(context -> {
            for (AbstractLangchain4jPluginConfiguration configuration
                    : context.getBeansOfType(AbstractLangchain4jPluginConfiguration.class).values()) {
                Plugin plugin = configuration.plugin();
                assertThat(plugin.getName()).isEqualTo(configuration.pluginName());

                boolean present = configuration.getClass().getClassLoader()
                        .getResource(configuration.frontendResource()) != null;
                assertThat(plugin.getJavascriptFilename())
                        .as("%s 前端产物缺失时只按基础插件标识注册", configuration.pluginName())
                        .isEqualTo(present ? configuration.javascriptFilename() : null);
            }
        });
    }

    @Test
    void shouldRegisterControllerOnlyFromChatModelConfiguration() {
        fullRunner.run(context -> {
            for (AbstractLangchain4jPluginConfiguration configuration
                    : context.getBeansOfType(AbstractLangchain4jPluginConfiguration.class).values()) {
                Mapping mapping = mock(Mapping.class);
                MagicConfiguration magicConfiguration = mock(MagicConfiguration.class);

                configuration.controllerRegister().register(mapping, magicConfiguration);

                if (configuration instanceof Langchain4jChatModelConfiguration) {
                    verify(mapping).registerController(any());
                } else {
                    verify(mapping, never()).registerController(any());
                }
            }
        });
    }
}
