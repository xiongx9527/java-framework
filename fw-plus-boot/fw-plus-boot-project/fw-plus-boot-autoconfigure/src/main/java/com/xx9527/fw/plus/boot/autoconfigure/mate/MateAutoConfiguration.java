package com.xx9527.fw.plus.boot.autoconfigure.mate;
import com.xx9527.fw.plus.boot.autoconfigure.mate.webmvc.MateWebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "fw.plus.mate", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@Import({MateEasyTransAutoConfiguration.class, MateWebMvcAutoConfiguration.class})
public class MateAutoConfiguration {
}
