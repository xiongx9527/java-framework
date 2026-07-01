package com.xiongx9527.fw.plus.boot.autoconfigure.mp;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.xiongx9527.fw.plus.mp.datachange.DataChangeHandler;
import com.xiongx9527.fw.plus.mp.datachange.properties.DataChangeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import java.util.Map;

/**
 * 数据变更监听配置类
 * 配置 fw.plus.mp-datachangemonitor.enabled = true 开启数据变更监听功能
 */
@Slf4j
@ConditionalOnClass({com.xiongx9527.fw.plus.mp.datachange.DataChangeInterceptor.class})
//@ConditionalOnBean({SqlSessionFactory.class})
@AutoConfigureAfter(MybatisPlusAutoConfiguration.class)
@ConfigurationPropertiesScan(basePackageClasses = {com.xiongx9527.fw.plus.mp.datachange.properties.DataChangeProperties.class})
@ConditionalOnProperty(prefix = "fw.plus.mp-datachange", name = "enabled", havingValue = "true",matchIfMissing = true)
@Lazy(false)
public class MpDataChangeAutoConfiguration {

    /**
     * 配置DataChangeMonitor属性
     *
     * 该方法通过Spring的@Bean注解定义了一个bean，确保在Spring容器中创建并管理该bean的实例
     * 使用@ConditionalOnMissingBean注解，仅当容器中没有相同类型的bean存在时，才创建此bean，避免了bean的重复创建
     * @ConfigurationProperties注解用于将配置文件中以"fw.plus.mp-datachange"为前缀的属性绑定到DataChangeMonitorProperties对象中
     *
     * @return 返回配置好的DataChangeMonitorProperties实例
     */
    @Bean
    @ConditionalOnClass({DataChangeProperties.class})
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "fw.plus.mp-datachange")
    public com.xiongx9527.fw.plus.mp.datachange.properties.DataChangeProperties dataChangeProperties() {
        return new com.xiongx9527.fw.plus.mp.datachange.properties.DataChangeProperties();
    }


    /**
     * 创建并配置一个数据变更拦截器
     *
     * 此方法在Spring框架中定义了一个主要的（@Primary）Bean，表示这个Bean在多个同类Bean存在时将被优先注入
     * 它使用了条件注解（@ConditionalOnBean），只有当特定的类（DataChangeProperties和DataChangeHandler）作为Bean存在于应用上下文中时，才会创建这个Bean
     *
     * @param dataChangeProperties 数据变更的配置属性，用于定制拦截器的行为
     * @param dataChangeHandlerMap 一个包含所有数据变更处理器的映射，键为处理器的名称，值为处理器实例
     * @return 返回一个配置好的数据变更拦截器实例，用于在数据变更时执行相应的处理逻辑
     */
    @Primary
    @Bean
    @ConditionalOnBean({DataChangeProperties.class, DataChangeHandler.class})
    public com.xiongx9527.fw.plus.mp.datachange.DataChangeInterceptor dataChangeMonitorInterceptor(com.xiongx9527.fw.plus.mp.datachange.properties.DataChangeProperties dataChangeProperties,
                                                                                                  Map<String, DataChangeHandler> dataChangeHandlerMap) {
        return new com.xiongx9527.fw.plus.mp.datachange.DataChangeInterceptor(dataChangeProperties,dataChangeHandlerMap);
    }


}
