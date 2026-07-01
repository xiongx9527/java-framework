package com.xx9527.fw.plus.cloud.autoconfigure.magicapi.plugin;

import com.xx9527.fw.plus.cloud.autoconfigure.magicapi.plugin.eureka.starter.MagicAPIEurekaConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({MagicAPIEurekaConfiguration.class
})
public class MagicApiPluginAutoConfiguration {
}
