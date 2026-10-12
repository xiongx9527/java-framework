package com.xiongx9527.fw.plus.ai.sample.agentscope.config;

import com.xiongx9527.fw.plus.ai.sample.agentscope.tool.SampleTools;
import io.agentscope.core.tool.Toolkit;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * 提供注册了样例工具的 {@link Toolkit}。
 *
 * <p>{@code AgentscopeAutoConfiguration} 默认注册的是空工具集的 {@code Toolkit}（并标注
 * {@code @ConditionalOnMissingBean}）。本类声明同类型的 {@code Toolkit} bean 后，自动配置的空
 * Toolkit 会退让，ReActAgent 最终注入的是这里注册了 {@link SampleTools} 的实例。
 *
 * <p>与自动配置保持一致，Toolkit 持有可变状态、非线程安全，故同样以 prototype 作用域暴露。
 */
@Configuration(proxyBeanMethods = false)
public class AgentscopeToolkitConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Toolkit agentscopeSampleToolkit(SampleTools sampleTools) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(sampleTools);
        return toolkit;
    }
}
