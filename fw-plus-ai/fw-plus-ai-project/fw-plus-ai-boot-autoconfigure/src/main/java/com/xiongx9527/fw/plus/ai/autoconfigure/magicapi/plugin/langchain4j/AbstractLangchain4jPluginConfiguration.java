package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

/**
 * 七类模型资源插件配置的公共骨架：统一插件标识、前端产物声明与「产物缺失即降级」判定。
 *
 * <p>每类资源对应一个独立的 starter 子工程与其前端产物，因此各自声明一个插件标识；
 * 前端产物缺失时只按基础插件标识注册，服务端装配与脚本取用不受影响。</p>
 *
 * <p>连接测试 Controller 的路径形如 {@code /langchain4j/{type}/datasource/test}，
 * 一个 Controller 即可服务七类，故默认不注册 controller，由 {@link Langchain4jChatModelConfiguration}
 * 统一承载，避免同一路径按类型重复注册。</p>
 */
public abstract class AbstractLangchain4jPluginConfiguration implements MagicPluginConfiguration {

    private static final MagicControllerRegister EMPTY_REGISTRAR = (mapping, configuration) -> {
    };

    private final String pluginName;
    private final String displayName;
    private final String javascriptFilename;

    protected AbstractLangchain4jPluginConfiguration(String pluginName, String displayName,
            String javascriptFilename) {
        this.pluginName = pluginName;
        this.displayName = displayName;
        this.javascriptFilename = javascriptFilename;
    }

    /** 插件标识，如 {@code langchain4j-chat-model} */
    public String pluginName() {
        return pluginName;
    }

    /** 前端产物名，如 {@code magic-langchain4j-chat-model.1.0.0.iife.js} */
    public String javascriptFilename() {
        return javascriptFilename;
    }

    /** 前端产物在 classpath 中的约定路径 */
    public String frontendResource() {
        return "magic-editor/plugins/" + javascriptFilename;
    }

    @Override
    public Plugin plugin() {
        return getClass().getClassLoader().getResource(frontendResource()) == null
                ? new Plugin(pluginName)
                : new Plugin(pluginName, displayName, javascriptFilename);
    }

    @Override
    public MagicControllerRegister controllerRegister() {
        return EMPTY_REGISTRAR;
    }
}
