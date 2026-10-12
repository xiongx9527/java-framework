package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * reactive 环境下屏蔽 Magic-API 官方自动配置，改由本模块的 WebFlux 子配置接管控制器注册。
 *
 * <p>环境类型用字符串判定：本类写在 spring.factories 里，会先于条件评估被加载，
 * 不能以 Class 形式引用 webflux 类型，否则 classpath 无 spring-webflux 时加载即失败。</p>
 */
public class MagicAPIWebFluxImportFilter implements AutoConfigurationImportFilter, EnvironmentAware {

    private static final String MAGIC_API_AUTO_CONFIGURATION =
            "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration";

    private static final String REACTIVE_WEB_ENVIRONMENT =
            "org.springframework.boot.web.reactive.context.ConfigurableReactiveWebEnvironment";

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean[] match(String[] autoConfigurationClasses,
                           AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean reactive = isReactiveWebEnvironment();
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            String candidate = autoConfigurationClasses[i];
            matches[i] = !(reactive && MAGIC_API_AUTO_CONFIGURATION.equals(candidate));
        }
        return matches;
    }

    /**
     * 判定当前是否 reactive web 环境：类名与其实现接口都参与比对，避免
     * 只匹配运行时具体类名（如 StandardReactiveWebEnvironment）而漏判。
     */
    private boolean isReactiveWebEnvironment() {
        if (environment == null) {
            return false;
        }
        for (Class<?> type = environment.getClass(); type != null; type = type.getSuperclass()) {
            if (REACTIVE_WEB_ENVIRONMENT.equals(type.getName())) {
                return true;
            }
            for (Class<?> itf : type.getInterfaces()) {
                if (REACTIVE_WEB_ENVIRONMENT.equals(itf.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
