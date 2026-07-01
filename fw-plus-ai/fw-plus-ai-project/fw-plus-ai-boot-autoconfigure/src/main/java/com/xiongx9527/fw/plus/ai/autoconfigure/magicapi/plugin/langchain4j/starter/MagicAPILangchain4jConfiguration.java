package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.LangChain4jModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.service.Langchain4jDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.service.Langchain4jDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web.MagicLangchain4jController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

@ConditionalOnProperty(prefix = "magic-api.plugin.langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"dev.langchain4j.model.chat.ChatLanguageModel",
        "dev.langchain4j.model.chat.StreamingChatLanguageModel",
        "dev.langchain4j.model.embedding.EmbeddingModel",
        "dev.langchain4j.model.image.ImageModel",
        "dev.langchain4j.model.language.LanguageModel",
        "dev.langchain4j.model.language.StreamingLanguageModel"})
@EnableConfigurationProperties(MagicAPILangchain4jConfig.class)
@Import(MagicAPILangchain4jModelConfiguration.class)
@Configuration
public class MagicAPILangchain4jConfiguration implements MagicPluginConfiguration {

        /** MagicAPI配置属性 */
    private final MagicAPIProperties properties;

    /**
     * MagicAPILangchain4jConfiguration构造函数
     * @param properties MagicAPI配置属性对象
     */
    public MagicAPILangchain4jConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }


    /**
     * 创建一个 MagicDynamicLangchain4jDataSource 实例，用于动态管理 Langchain4j 数据源。
     * 该 Bean 仅在当前上下文中不存在同名 Bean 时才会被创建。
     *
     * @return MagicDynamicLangchain4jDataSource 实例
     */
    @Bean
    public MagicDynamicLangchain4jDataSource magicDynamicLangchain4jDataSource() {
        return new MagicDynamicLangchain4jDataSource();
    }

    /**
     * 创建 Langchain4jModule 实例，依赖于已定义的 MagicDynamicLangchain4jDataSource，
     * 并注入各种模型映射（如图像模型、嵌入模型、语言模型等）。
     * 此 Bean 的初始化依赖于 magicDynamicLangchain4jDataSource。
     *
     * @param magicDynamiclangchain4jDataSource 动态数据源实例
     * @return Langchain4jModule 实例（当前实现返回 null）
     */
    @Bean
    @DependsOn(value={"magicDynamicLangchain4jDataSource"})
    public Langchain4jModule langchain4jModule(MagicDynamicLangchain4jDataSource magicDynamiclangchain4jDataSource,
                                               @Autowired(required = false) LangChain4jModelInfo langChain4jModelInfo) {
        return new Langchain4jModule(magicDynamiclangchain4jDataSource,langChain4jModelInfo);
    }

    /**
     * 创建 Langchain4jDataSourceInfoMagicResourceStorage 实例，
     * 用于存储和管理 Langchain4j 数据源相关信息。
     *
     * @return Langchain4jDataSourceInfoMagicResourceStorage 实例
     */
    @Bean
    public Langchain4jDataSourceInfoMagicResourceStorage langchain4jDataSourceInfoMagicResourceStorage() {
        return new Langchain4jDataSourceInfoMagicResourceStorage();
    }

    /**
     * 创建 Langchain4jDataSourceMagicDynamicRegistry 实例，
     * 用于注册和管理动态数据源。该 Bean 仅在当前上下文中不存在同名 Bean 时才会被创建。
     *
     * @param langchain4jDataSourceInfoMagicResourceStorage 数据源信息存储实例
     * @param magicDynamicLangchain4jDataSource             动态数据源实例
     * @return Langchain4jDataSourceMagicDynamicRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public Langchain4jDataSourceMagicDynamicRegistry langchain4jDataSourceMagicDynamicRegistry(Langchain4jDataSourceInfoMagicResourceStorage langchain4jDataSourceInfoMagicResourceStorage,
                                                                                               MagicDynamicLangchain4jDataSource magicDynamicLangchain4jDataSource) {
        return new Langchain4jDataSourceMagicDynamicRegistry(langchain4jDataSourceInfoMagicResourceStorage,magicDynamicLangchain4jDataSource);
    }

    /**
     * 获取插件信息
     *
     * @return 插件信息实例
     */
    @Override
    public Plugin plugin() {
        return new Plugin("langchain4j", "MagicLangchain4j", "magic-langchain4j.1.0.0.iife.js");
    }

    /**
     * 注册控制器，将 MagicLangchain4jController 添加到路由映射中。
     *
     * @return MagicControllerRegister 控制器注册器实例
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicLangchain4jController(configuration));
    }


}
