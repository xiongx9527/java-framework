package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.web.reactive.context.ConfigurableReactiveWebEnvironment;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

public class MagicAPIWebFluxImportFilter implements AutoConfigurationImportFilter, EnvironmentAware {

    private static final String MAGIC_API_AUTO_CONFIGURATION =
            "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration";

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean[] match(String[] autoConfigurationClasses,
                           AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            String candidate = autoConfigurationClasses[i];
            matches[i] = !(environment instanceof ConfigurableReactiveWebEnvironment
                    && MAGIC_API_AUTO_CONFIGURATION.equals(candidate));
        }
        return matches;
    }
}
