package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.starter;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.modules.FreemarkerModule;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.service.FreemarkerMagicDynamicRegistry;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.service.FtlInfoMagicResourceStorage;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.web.MagicFreemarkerController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

@ConditionalOnProperty(prefix = "magic-api.plugin.freemarker", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name={"freemarker.template.Template"})
@Configuration
@Import({StaticResourceConfiguration.class})
public class MagicAPIFreemarkerConfiguration implements MagicPluginConfiguration {

    /**
     * 获取FreeMarker插件实例
     *
     * 此方法重写自父类，用于提供一个特定于当前环境或应用的FreeMarker插件实例
     * 选择FreeMarker作为模板引擎，因为它在处理模板和数据分离时提供了高度的灵活性和效率
     *
     * @return Plugin 返回一个Plugin对象，该对象表示一个FreeMarker插件实例
     *         该插件具有以下属性：
     *         - 名称："freemarker"
     *         - 标识："MagicFreemarker"
     *         - 脚本："magic-freemarker.1.0.0.iife.js"，表示插件的入口JavaScript文件
     */
    @Override
    public Plugin plugin() {
        return new Plugin("freemarker", "MagicFreemarker","magic-freemarker.1.0.0.iife.js");
    }

    /**
     * 重写方法以提供自定义的控制器注册逻辑
     * 此方法返回一个控制器注册对象，该对象负责在系统中注册特定的控制器
     * 在本例中，它注册了一个基于Freemarker模板引擎的控制器
     *
     * @return MagicControllerRegister 实现对象，用于注册控制器
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        // 返回一个Lambda表达式，实现了MagicControllerRegister接口
        // 该表达式接收一个映射对象和一个配置对象作为参数
        // 并通过调用映射对象的registerController方法来注册一个新的MagicFreemarkerController
        // 这种设计允许灵活地将特定的控制器与配置关联起来
        return (mapping, configuration) -> mapping.registerController(new MagicFreemarkerController(configuration));
    }

    /**
     * 创建FtlInfoMagicResourceStorage的Bean实例
     * 该方法使用Spring框架的@Bean注解，用于声明一个Bean
     * @ConditionalOnMissingBean注解确保只有在当前上下文中不存在相同类型的Bean时，才会创建一个新的Bean实例
     *
     * @return 新创建的FtlInfoMagicResourceStorage实例
     */
    @Bean
    @ConditionalOnMissingBean
    public FtlInfoMagicResourceStorage ftlInfoMagicResourceStorage() {
        return new FtlInfoMagicResourceStorage();
    }


    /**
     * 创建FreemarkerMagicDynamicRegistry bean实例
     * 当容器中不存在FreemarkerMagicDynamicRegistry类型的bean时，创建并注册该bean
     *
     * @param ftlInfoMagicResourceStorage 模板信息资源存储器，用于管理模板资源
     * @return 返回新创建的FreemarkerMagicDynamicRegistry实例
     */
    @Bean
    @ConditionalOnMissingBean
    public FreemarkerMagicDynamicRegistry freemarkerMagicDynamicRegistry(FtlInfoMagicResourceStorage ftlInfoMagicResourceStorage) {
        return new FreemarkerMagicDynamicRegistry(ftlInfoMagicResourceStorage);
    }

    /**
     * 创建FreemarkerModule bean实例
     *
     * @return 返回新创建的FreemarkerModule实例
     */
    @Bean
    public FreemarkerModule freemarkerModule() {
        return new FreemarkerModule();
    }


}
