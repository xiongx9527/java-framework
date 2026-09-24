package com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge;

import com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.config.CentrifugoProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.service.CentrifugoConnectionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.service.CentrifugoPublishService;
import io.github.centrifugal.centrifuge.ConnectionTokenGetter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Centrifuge Starter 自动配置，只保留监听和发送能力。
 */
@Configuration
@ConditionalOnProperty(prefix = "fw.plus.centrifuge", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "io.github.centrifugal.centrifuge.Client")
@EnableConfigurationProperties(CentrifugoProperties.class)
public class CentrifugoAutoConfiguration implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    @Bean
    @ConditionalOnMissingBean
    public CentrifugoConnectionService centrifugoConnectionService(CentrifugoProperties properties) {
        ConnectionTokenGetter tokenGetter = null;
        String beanName = properties.getTokenGetterBeanName();
        if (beanName != null && applicationContext != null && applicationContext.containsBean(beanName)) {
            tokenGetter = applicationContext.getBean(beanName, ConnectionTokenGetter.class);
        }
        return new CentrifugoConnectionService(properties, tokenGetter);
    }

    @Bean
    @ConditionalOnMissingBean
    public CentrifugoPublishService centrifugoPublishService(CentrifugoProperties properties,
                                                             CentrifugoConnectionService connectionService) {
        return new CentrifugoPublishService(properties, connectionService);
    }
}
