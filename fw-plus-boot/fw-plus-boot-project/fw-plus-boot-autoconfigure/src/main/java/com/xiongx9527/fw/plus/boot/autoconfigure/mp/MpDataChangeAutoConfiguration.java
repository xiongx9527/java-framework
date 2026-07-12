package com.xiongx9527.fw.plus.boot.autoconfigure.mp;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.xiongx9527.fw.plus.mp.datachange.DataChangeInterceptor;
import com.xiongx9527.fw.plus.mp.datachange.DataChangeHandler;
import com.xiongx9527.fw.plus.mp.datachange.properties.DataChangeProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Map;

@ConditionalOnClass({DataChangeInterceptor.class, DataChangeProperties.class})
@AutoConfigureAfter(MybatisPlusAutoConfiguration.class)
@ConditionalOnProperty(prefix = "fw.plus.mp-datachange", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MpDataChangeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "fw.plus.mp-datachange")
    public DataChangeProperties dataChangeProperties() {
        return new DataChangeProperties();
    }

    @Primary
    @Bean
    @ConditionalOnBean({DataChangeProperties.class, DataChangeHandler.class})
    @ConditionalOnMissingBean
    public DataChangeInterceptor dataChangeMonitorInterceptor(DataChangeProperties dataChangeProperties,
                                                              Map<String, DataChangeHandler> dataChangeHandlerMap) {
        return new DataChangeInterceptor(dataChangeProperties, dataChangeHandlerMap);
    }
}
