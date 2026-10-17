package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.*;
import com.baomidou.dynamic.datasource.strategy.DynamicDataSourceStrategy;
import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.DruidDynamicDataSourceConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.DynamicDataSourceAopConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.properties.DefaultDataSourceProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 动态数据源核心自动配置类
 *
 * @author TaoYu Kanyuxia
 * @see DynamicDataSourceProvider
 * @see DynamicDataSourceStrategy
 * @see DynamicRoutingDataSource
 * @since 1.0.0
 */
@Slf4j
@ConditionalOnClass({DynamicDataSourceProperties.class})
@AutoConfigureBefore(value = DataSourceAutoConfiguration.class, name = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure",
        "com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration"})
//com.baomidou.dynamic.datasource.spring.boot.autoconfigure
//@AutoConfigureBefore(name = "com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration")
@EnableConfigurationProperties(DefaultDataSourceProperty.class)
@Import({DruidDynamicDataSourceConfiguration.class,
        DynamicDataSourceCreatorAutoConfiguration.class,
        DynamicDataSourceAopConfiguration.class,
        DynamicDataSourceAssistConfiguration.class})
@ConditionalOnProperty(prefix = DynamicDataSourceProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class DynamicDataSourceAutoConfiguration implements InitializingBean {

    private static final String DEFAULT_PRIMARY = "master";

    /**
     * 动态数据源配置属性实例
     * 用于存储和管理动态数据源的核心配置信息
     */
    private final DynamicDataSourceProperties properties;

    //private final DynamicDataSourceProperties defaultDataSourceProperty;

    /**
     * 数据源属性自定义器列表
     * 用于存储多个数据源属性的自定义配置处理器，可以在运行时动态调整数据源配置
     */
    private final List<DynamicDataSourcePropertiesCustomizer> dataSourcePropertiesCustomizers;

    /**
     * 动态数据源自动配置类
     *
     * @param properties 动态数据源属性配置
     * @param defaultDataSourceProperty 默认数据源属性配置
     * @param dataSourcePropertiesCustomizers 数据源属性自定义器列表
     */
    public DynamicDataSourceAutoConfiguration(
            DynamicDataSourceProperties properties,
            DefaultDataSourceProperty defaultDataSourceProperty,
            ObjectProvider<List<DynamicDataSourcePropertiesCustomizer>> dataSourcePropertiesCustomizers) {
        // 当属性配置和默认数据源配置都不为空且URL不为空时，进行数据源配置初始化
        if (ObjectUtils.isNotEmpty(properties) && ObjectUtils.isNotEmpty(defaultDataSourceProperty)
                && StringUtils.isNotBlank(defaultDataSourceProperty.getUrl())) {
            // 如果数据源映射为空，则创建新的LinkedHashMap
            if (null == properties.getDatasource()) {
                properties.setDatasource(new LinkedHashMap<>());
            }
            String primary = resolvePrimary(properties);
            // 如果主数据源不存在于数据源映射中，则将默认数据源添加为主数据源
            if (!properties.getDatasource().containsKey(primary)) {
                properties.getDatasource().put(primary, defaultDataSourceProperty);
            }
        }
        this.properties = properties;
        this.dataSourcePropertiesCustomizers = dataSourcePropertiesCustomizers.getIfAvailable();
    }

    /**
     * 创建数据源Bean
     *
     * @param providers 动态数据源提供者列表
     * @return 配置好的数据源实例
     */
    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(List<DynamicDataSourceProvider> providers) {
        DynamicRoutingDataSource dataSource = new DynamicRoutingDataSource(providers);
        dataSource.setPrimary(properties.getPrimary());
        dataSource.setStrict(properties.getStrict());
        dataSource.setStrategy(properties.getStrategy());
        dataSource.setP6spy(properties.getP6spy());
        dataSource.setSeata(properties.getSeata());
        dataSource.setGraceDestroy(properties.getGraceDestroy());
        return dataSource;
    }

    /**
     * 属性设置完成后的回调方法，用于应用数据源属性自定义器
     */
    @Override
    public void afterPropertiesSet() {
        // 如果存在数据源属性自定义器，则依次应用到属性配置上
        if (!CollectionUtils.isEmpty(dataSourcePropertiesCustomizers)) {
            for (DynamicDataSourcePropertiesCustomizer customizer : dataSourcePropertiesCustomizers) {
                customizer.customize(properties);
            }
        }
    }

    private String resolvePrimary(DynamicDataSourceProperties properties) {
        if (StringUtils.isBlank(properties.getPrimary())) {
            return DEFAULT_PRIMARY;
        }
        return properties.getPrimary();
    }

}
