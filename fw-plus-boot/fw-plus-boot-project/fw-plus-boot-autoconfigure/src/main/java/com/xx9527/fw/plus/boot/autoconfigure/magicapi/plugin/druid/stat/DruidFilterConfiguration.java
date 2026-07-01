package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.stat;

import com.alibaba.druid.filter.config.ConfigFilter;
import com.alibaba.druid.filter.encoding.EncodingConvertFilter;
import com.alibaba.druid.filter.logging.CommonsLogFilter;
import com.alibaba.druid.filter.logging.Log4j2Filter;
import com.alibaba.druid.filter.logging.Log4jFilter;
import com.alibaba.druid.filter.logging.Slf4jLogFilter;
import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author lihengming [89921218@qq.com]
 */
public class DruidFilterConfiguration {

    /**
     * 创建并配置StatFilter实例。
     * 该方法通过读取前缀为FILTER_STAT_PREFIX的配置属性来初始化StatFilter，
     * 并且只有在对应的enabled属性为true时才会创建此Bean。
     * 若容器中已存在同类型的Bean，则不会重复创建。
     *
     * @return 返回新创建的StatFilter实例
     */
    @Bean
    @ConfigurationProperties(FILTER_STAT_PREFIX)
    @ConditionalOnProperty(prefix = FILTER_STAT_PREFIX, name = "enabled")
    @ConditionalOnMissingBean
    public StatFilter statFilter() {
        return new StatFilter();
    }

    /**
     * 创建并配置ConfigFilter实例。
     * 该方法通过读取前缀为FILTER_CONFIG_PREFIX的配置属性来初始化ConfigFilter，
     * 并且只有在对应的enabled属性为true时才会创建此Bean。
     * 若容器中已存在同类型的Bean，则不会重复创建。
     *
     * @return 返回新创建的ConfigFilter实例
     */
    @Bean
    @ConfigurationProperties(FILTER_CONFIG_PREFIX)
    @ConditionalOnProperty(prefix = FILTER_CONFIG_PREFIX, name = "enabled")
    @ConditionalOnMissingBean
    public ConfigFilter configFilter() {
        return new ConfigFilter();
    }

    /**
     * 创建并配置EncodingConvertFilter实例。
     * 该方法通过读取前缀为FILTER_ENCODING_PREFIX的配置属性来初始化EncodingConvertFilter，
     * 并且只有在对应的enabled属性为true时才会创建此Bean。
     * 若容器中已存在同类型的Bean，则不会重复创建。
     *
     * @return 返回新创建的EncodingConvertFilter实例
     */
    @Bean
    @ConfigurationProperties(FILTER_ENCODING_PREFIX)
    @ConditionalOnProperty(prefix = FILTER_ENCODING_PREFIX, name = "enabled")
    @ConditionalOnMissingBean
    public EncodingConvertFilter encodingConvertFilter() {
        return new EncodingConvertFilter();
    }

    /**
     * 创建并配置Slf4jLogFilter实例。
     * 该方法通过读取前缀为FILTER_SLF4J_PREFIX的配置属性来初始化Slf4jLogFilter，
     * 并且只有在对应的enabled属性为true时才会创建此Bean。
     * 若容器中已存在同类型的Bean，则不会重复创建。
     *
     * @return 返回新创建的Slf4jLogFilter实例
     */
    @Bean
    @ConfigurationProperties(FILTER_SLF4J_PREFIX)
    @ConditionalOnProperty(prefix = FILTER_SLF4J_PREFIX, name = "enabled")
    @ConditionalOnMissingBean
    public Slf4jLogFilter slf4jLogFilter() {
        return new Slf4jLogFilter();
    }

    /**
     * 创建并配置Log4jFilter实例。
     * 该方法通过读取前缀为FILTER_LOG4J_PREFIX的配置属性来初始化Log4jFilter，
     * 并且只有在对应的enabled属性为true时才会创建此Bean。
     * 若容器中已存在同类型的Bean，则不会重复创建。
     *
     * @return 返回新创建的Log4jFilter实例
     */
    @Bean
    @ConfigurationProperties(FILTER_LOG4J_PREFIX)
    @ConditionalOnProperty(prefix = FILTER_LOG4J_PREFIX, name = "enabled")
    @ConditionalOnMissingBean
    public Log4jFilter log4jFilter() {
        return new Log4jFilter();
    }

    /**
     * 创建并配置Log4j2Filter实例。
     * 该方法通过读取前缀为FILTER_LOG4J2_PREFIX的配置属性来初始化Log4j2Filter，
     * 并且只有在对应的enabled属性为true时才会创建此Bean。
     * 若容器中已存在同类型的Bean，则不会重复创建。
     *
     * @return 返回新创建的Log4j2Filter实例
     */
    @Bean
    @ConfigurationProperties(FILTER_LOG4J2_PREFIX)
    @ConditionalOnProperty(prefix = FILTER_LOG4J2_PREFIX, name = "enabled")
    @ConditionalOnMissingBean
    public Log4j2Filter log4j2Filter() {
        return new Log4j2Filter();
    }

    /**
     * 创建并配置CommonsLogFilter实例。
     * 该方法通过读取前缀为FILTER_COMMONS_LOG_PREFIX的配置属性来初始化CommonsLogFilter，
     * 并且只有在对应的enabled属性为true时才会创建此Bean。
     * 若容器中已存在同类型的Bean，则不会重复创建。
     *
     * @return 返回新创建的CommonsLogFilter实例
     */
    @Bean
    @ConfigurationProperties(FILTER_COMMONS_LOG_PREFIX)
    @ConditionalOnProperty(prefix = FILTER_COMMONS_LOG_PREFIX, name = "enabled")
    @ConditionalOnMissingBean
    public CommonsLogFilter commonsLogFilter() {
        return new CommonsLogFilter();
    }

    /**
     * 创建并配置WallConfig实例。
     * 该方法通过读取前缀为FILTER_WALL_CONFIG_PREFIX的配置属性来初始化WallConfig，
     * 并且只有在对应的enabled属性为true时才会创建此Bean。
     * 若容器中已存在同类型的Bean，则不会重复创建。
     *
     * @return 返回新创建的WallConfig实例
     */
    @Bean
    @ConfigurationProperties(FILTER_WALL_CONFIG_PREFIX)
    @ConditionalOnProperty(prefix = FILTER_WALL_PREFIX, name = "enabled")
    @ConditionalOnMissingBean
    public WallConfig wallConfig() {
        return new WallConfig();
    }

    /**
     * 创建并配置WallFilter实例。
     * 该方法通过读取前缀为FILTER_WALL_PREFIX的配置属性来初始化WallFilter，
     * 并且只有在对应的enabled属性为true时才会创建此Bean。
     * 同时注入一个WallConfig实例作为其配置项。
     * 若容器中已存在同类型的Bean，则不会重复创建。
     *
     * @param wallConfig 注入的WallConfig配置对象
     * @return 返回新创建并设置了配置的WallFilter实例
     */
    @Bean
    @ConfigurationProperties(FILTER_WALL_PREFIX)
    @ConditionalOnProperty(prefix = FILTER_WALL_PREFIX, name = "enabled")
    @ConditionalOnMissingBean
    public WallFilter wallFilter(WallConfig wallConfig) {
        WallFilter filter = new WallFilter();
        filter.setConfig(wallConfig);
        return filter;
    }


    /**
     * Druid数据库连接池统计过滤器配置项前缀常量
     *
     * 该常量用于标识Druid数据源统计过滤器相关的配置属性前缀，
     * 通常用于从配置文件中读取相关统计监控参数。
     */
    private static final String FILTER_STAT_PREFIX = "magic-api.plugin.druid.filter.stat";

    /**
     * Druid过滤器配置的前缀常量
     *
     * 该常量用于标识Druid数据源过滤器配置的属性前缀，
     * 通常用于从配置文件中读取相关的过滤器配置信息。
     * 配置属性通常以该前缀开头，后跟具体的配置项名称。
     */
    private static final String FILTER_CONFIG_PREFIX = "magic-api.plugin.druid.filter.config";

    /**
     * Druid过滤器编码配置项前缀
     *
     * 该常量定义了Druid数据源过滤器中编码相关配置的前缀，
     * 用于标识和管理数据库连接过滤器中的字符编码设置。
     * 配置格式通常为：magic-api.plugin.druid.filter.encoding=编码值
     */
    private static final String FILTER_ENCODING_PREFIX = "magic-api.plugin.druid.filter.encoding";

    /**
     * SLF4J过滤器配置前缀常量
     *
     * 该常量用于标识Druid数据源插件中SLF4J日志过滤器的配置前缀，
     * 便于在配置文件中进行相关参数的统一管理和识别。
     */
    private static final String FILTER_SLF4J_PREFIX = "magic-api.plugin.druid.filter.slf4j";

    /**
     * Druid数据源Log4j过滤器配置前缀常量
     *
     * 该常量用于标识Druid数据源插件中Log4j过滤器的相关配置项前缀，
     * 便于在配置文件中统一管理和识别相关过滤器配置。
     */
    private static final String FILTER_LOG4J_PREFIX = "magic-api.plugin.druid.filter.log4j";

    /**
     * Druid数据源Log4j2过滤器配置前缀常量
     *
     * 该常量用于标识Druid数据源插件中Log4j2过滤器的相关配置项前缀，
     * 便于在配置文件中统一管理和识别相关过滤器配置。
     */
    private static final String FILTER_LOG4J2_PREFIX = "magic-api.plugin.druid.filter.log4j2";

    /**
     * Druid数据源CommonsLog过滤器配置前缀常量
     *
     * 该常量用于标识Druid数据源插件中CommonsLog过滤器的相关配置项前缀，
     * 便于在配置文件中统一管理和识别相关过滤器配置。
     */
    private static final String FILTER_COMMONS_LOG_PREFIX = "magic-api.plugin.druid.filter.commons-log";

    /**
     * Druid数据源Wall过滤器配置前缀常量
     *
     * 该常量用于标识Druid数据源插件中Wall过滤器相关的配置项前缀，
     * 便于在配置文件中统一管理和识别相关过滤器配置。
     */
    private static final String FILTER_WALL_PREFIX = "magic-api.plugin.druid.filter.wall";


    /**
     * Druid数据源Wall过滤器配置项前缀常量
     *
     * 该常量用于标识Druid数据源插件中Wall过滤器配置项的前缀，
     * 便于在配置文件中统一管理和识别相关Wall过滤器配置项。
     */
    private static final String FILTER_WALL_CONFIG_PREFIX = FILTER_WALL_PREFIX + ".config";



}