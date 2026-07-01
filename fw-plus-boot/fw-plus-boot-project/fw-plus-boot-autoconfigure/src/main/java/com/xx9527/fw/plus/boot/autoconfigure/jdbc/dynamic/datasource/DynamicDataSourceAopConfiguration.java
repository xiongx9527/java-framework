package com.xx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationAdvisor;
import com.baomidou.dynamic.datasource.aop.DynamicDataSourceAnnotationInterceptor;
import com.baomidou.dynamic.datasource.aop.DynamicLocalTransactionInterceptor;
import com.baomidou.dynamic.datasource.processor.DsProcessor;
import com.baomidou.dynamic.datasource.processor.DsSpelExpressionProcessor;
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDatasourceAopProperties;
import com.baomidou.dynamic.datasource.strategy.DynamicDataSourceStrategy;
import com.xx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsHeaderProcessor;
import com.xx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsSessionProcessor;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.context.expression.BeanFactoryResolver;

/**
 * 动态数据源核心自动配置类
 *
 * @author TaoYu Kanyuxia
 * @see DynamicDataSourceProvider
 * @see DynamicDataSourceStrategy
 * @see DynamicRoutingDataSource
 * @since 1.0.0
 */
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Configuration(proxyBeanMethods = false)
public class DynamicDataSourceAopConfiguration {

    /**
     * 动态数据源配置属性对象
     *
     * 该属性对象包含了动态数据源的所有配置信息，
     * 用于初始化和管理多个数据源的连接配置。
     */
    private final DynamicDataSourceProperties properties;


    /**
     * 动态数据源AOP配置类的构造函数
     *
     * @param properties 动态数据源属性配置对象，用于初始化数据源相关配置
     */
    public DynamicDataSourceAopConfiguration(DynamicDataSourceProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建并配置动态数据源属性对象。
     * <p>
     * 该方法用于初始化 {@link DynamicDataSourceProperties} 实例，并将其注册为 Spring 容器中的 Bean，
     * 以便其他组件可以使用这些配置信息来管理多个数据源的行为。
     *
     * @return 初始化完成的 {@link DynamicDataSourceProperties} 对象实例
     */
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    public static DynamicDataSourceProperties dynamicDataSourceProperties() {
        return new DynamicDataSourceProperties();
    }

    /**
     * 创建并配置数据源处理器链。
     * <p>
     * 此方法构建一个处理链，依次包括 Header 处理器、Session 处理器以及 SpEL 表达式处理器。
     * 这些处理器将按照顺序被调用来解析当前应使用的数据源标识。
     *
     * @param beanFactory Spring 的 BeanFactory，用于支持 SpEL 表达式中引用容器内的 Bean
     * @return 配置好的数据源处理器链起始节点（headerProcessor）
     */
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    @ConditionalOnMissingBean
    public DsProcessor dsProcessor(BeanFactory beanFactory) {
        DsProcessor headerProcessor = new DsHeaderProcessor();
        DsProcessor sessionProcessor = new DsSessionProcessor();
        DsSpelExpressionProcessor spelExpressionProcessor = new DsSpelExpressionProcessor();
        spelExpressionProcessor.setBeanResolver(new BeanFactoryResolver(beanFactory));
        headerProcessor.setNextProcessor(sessionProcessor);
        sessionProcessor.setNextProcessor(spelExpressionProcessor);
        return headerProcessor;
    }

    /**
     * 创建并配置基于注解的动态数据源 AOP 切面顾问。
     * <p>
     * 当配置允许启用 AOP 功能时，此方法会创建一个切面顾问，它通过拦截带有 {@link DS} 注解的方法或类，
     * 来实现运行期动态切换数据源的功能。
     *
     * @param dsProcessor 数据源处理器链，负责解析目标数据源名称
     * @return 配置好的 AOP 顾问对象
     */
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    @ConditionalOnProperty(prefix = DynamicDataSourceProperties.PREFIX + ".aop", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Advisor dynamicDatasourceAnnotationAdvisor(DsProcessor dsProcessor) {
        DynamicDatasourceAopProperties aopProperties = properties.getAop();
        DynamicDataSourceAnnotationInterceptor interceptor = new DynamicDataSourceAnnotationInterceptor(aopProperties.getAllowedPublicOnly(), dsProcessor);
        DynamicDataSourceAnnotationAdvisor advisor = new DynamicDataSourceAnnotationAdvisor(interceptor, DS.class);
        advisor.setOrder(aopProperties.getOrder());
        return advisor;
    }

    /**
     * 创建并配置本地事务管理的 AOP 切面顾问。
     * <p>
     * 在未启用 Seata 分布式事务的情况下，此方法提供一个本地事务控制的切面实现，
     * 拦截标记了 {@link DSTransactional} 注解的方法以进行本地事务管理。
     *
     * @return 配置好的本地事务管理 AOP 顾问对象
     */
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    @ConditionalOnProperty(prefix = DynamicDataSourceProperties.PREFIX, name = "seata", havingValue = "false", matchIfMissing = true)
    public Advisor dynamicTransactionAdvisor() {
        DynamicDatasourceAopProperties aopProperties = properties.getAop();
        DynamicLocalTransactionInterceptor interceptor = new DynamicLocalTransactionInterceptor(aopProperties.getAllowedPublicOnly());
        return new DynamicDataSourceAnnotationAdvisor(interceptor, DSTransactional.class);
    }

}

