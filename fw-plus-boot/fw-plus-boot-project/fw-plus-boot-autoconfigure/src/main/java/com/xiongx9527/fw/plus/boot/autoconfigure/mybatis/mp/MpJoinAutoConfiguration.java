package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp;

import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.github.yulichang.autoconfigure.MybatisPlusJoinProperties;
import com.github.yulichang.autoconfigure.conditional.JoinSqlInjectorCondition;
import com.github.yulichang.autoconfigure.consumer.MybatisPlusJoinIfExistsConsumer;
import com.github.yulichang.autoconfigure.consumer.MybatisPlusJoinPropertiesConsumer;
import com.github.yulichang.config.ConfigProperties;
import com.github.yulichang.config.MPJInterceptorConfig;
import com.github.yulichang.extension.mapping.config.MappingConfig;
import com.github.yulichang.injector.MPJSqlInjector;
import com.github.yulichang.interceptor.MPJInterceptor;
import com.github.yulichang.toolkit.SpringContentUtils;
import com.github.yulichang.wrapper.enums.IfExistsSqlKeyWordEnum;
import com.github.yulichang.wrapper.interfaces.MBiPredicate;
import com.xiongx9527.fw.plus.mp.base.MpSqlInjector;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@ConditionalOnClass(name = {
        "org.apache.ibatis.session.SqlSessionFactory",
        "org.mybatis.spring.SqlSessionFactoryBean",
        "com.github.yulichang.autoconfigure.MybatisPlusJoinProperties"
})
@EnableConfigurationProperties({MpJoinProperties.class})
@ConditionalOnProperty(prefix = "fw.plus.mp-join", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MpJoinAutoConfiguration {
    private final MybatisPlusJoinProperties properties;

    public MpJoinAutoConfiguration(MpJoinProperties properties,
                                   ObjectProvider<MybatisPlusJoinPropertiesConsumer> propertiesConsumers,
                                   ObjectProvider<MybatisPlusJoinIfExistsConsumer> ifExistsConsumers) {
        MybatisPlusJoinProperties mybatisPlusJoinProperties = properties;
        this.properties = Optional.ofNullable(propertiesConsumers.getIfAvailable()).map(c -> c.config(mybatisPlusJoinProperties)).orElse(mybatisPlusJoinProperties);
        ConfigProperties.banner = this.properties.getBanner();
        ConfigProperties.tableAlias = this.properties.getTableAlias();
        ConfigProperties.joinPrefix = this.properties.getJoinPrefix();
        ConfigProperties.logicDelType = this.properties.getLogicDelType();
        ConfigProperties.subQueryAlias = this.properties.getSubQueryAlias();
        ConfigProperties.subTableLogic = this.properties.getSubTableLogic();
        ConfigProperties.mappingMaxCount = this.properties.getMappingMaxCount();
        ConfigProperties.Convert.IfExists = this.properties.getIfExists();
        ConfigProperties.ifExists = Optional.ofNullable(ifExistsConsumers.getIfAvailable())
                .map(m -> (MBiPredicate<Object, IfExistsSqlKeyWordEnum>) m)
                .orElse((val, key) -> ConfigProperties.Convert.IfExists.test(val));
        info("mybatis plus join properties config complete");
    }

    @Bean
    @ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
    public MPJInterceptor mpjInterceptor() {
        // mybatis-plus-join 1.5.9 起移除了手动版本设置入口（MyBatis-Plus 3.5.17 提供 MybatisPlusVersion，插件自行探测版本）
        return new MPJInterceptor();
    }

    @Bean
    @Order(Integer.MIN_VALUE)
    @ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
    public MPJInterceptorConfig mpjInterceptorConfig(List<SqlSessionFactory> sqlSessionFactoryList, MPJInterceptor mpjInterceptor) {
        // mybatis-plus-join 1.5.9 构造器签名调整为 (SqlSessionFactory 列表, MPJInterceptor, banner)
        return new MPJInterceptorConfig(sqlSessionFactoryList, mpjInterceptor, this.properties.getBanner());
    }

    @Bean
    @JoinSqlInjectorCondition
    @Order(Integer.MIN_VALUE + 1)
    @ConditionalOnBean({ISqlInjector.class})
    @ConditionalOnMissingBean(MpSqlInjector.class)
    @ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
    public MpSqlInjector mpjSqlInjector(ISqlInjector sqlInjector) {
        info("mybatis plus join SqlInjector init");
        return new MPJSqlInjector(sqlInjector);
    }

    @Primary
    @Bean
    @Order(Integer.MIN_VALUE)
    @ConditionalOnMissingBean({ISqlInjector.class})
    @ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
    public MPJSqlInjector mpjSqlInjectorOnMiss() {
        info("mybatis plus join SqlInjector init");
        return new MPJSqlInjector();
    }

    private static void info(String info) {
        if (ConfigProperties.banner) {
            log.info(info);
        }
    }

    @Configuration
    @Order(Integer.MIN_VALUE)
    @ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
    public static class MPJMappingConfig implements ApplicationListener<ApplicationReadyEvent> {
        public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
            MappingConfig.init();
        }
    }

    @Configuration
    @ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
    public static class MPJSpringContext implements SpringContentUtils.SpringContext, BeanFactoryPostProcessor, ApplicationContextAware {
        private ApplicationContext applicationContext;
        private ListableBeanFactory listableBeanFactory;

        public <T> T getBean(Class<T> clazz) {
            return this.getBeanFactory().getBean(clazz);
        }

        public <T> void getBeansOfType(Class<T> clazz) {
            this.getBeanFactory().getBeansOfType(clazz);
        }

        private ListableBeanFactory getBeanFactory() {
            return (ListableBeanFactory) (this.applicationContext == null ? this.listableBeanFactory : this.applicationContext);
        }

        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = applicationContext;
            SpringContentUtils.setSpringContext(this);
        }

        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            this.listableBeanFactory = beanFactory;
            SpringContentUtils.setSpringContext(this);
            try {
                String[] names = beanFactory.getBeanNamesForType(SqlSessionFactory.class);

                if (ObjectUtils.isEmpty(names)) {
                    return;
                }

                for (String name : names) {
                    BeanDefinition definition = beanFactory.getBeanDefinition(name);
                    info(String.format("MPJ SqlSessionFactory bean definition: %s factoryBeanName: %s factoryMethodName: %s source: %s", name, definition.getFactoryBeanName(), definition.getFactoryMethodName(), definition.getSource()));
                }
            } catch (BeansException e) {
                log.debug("Failed to log MPJ SqlSessionFactory bean definitions", e);
            }
        }
    }
}
