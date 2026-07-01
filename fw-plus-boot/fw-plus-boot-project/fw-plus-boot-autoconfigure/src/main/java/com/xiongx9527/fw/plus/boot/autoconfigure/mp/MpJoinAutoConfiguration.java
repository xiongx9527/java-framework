package com.xiongx9527.fw.plus.boot.autoconfigure.mp;


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
import com.xiongx9527.fw.plus.common.util.OrikaUtils;
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
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.Optional;

@Slf4j
//@EnableAutoConfiguration(exclude = {com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration.class})
/**
@Configuration(
        proxyBeanMethods = false
)
 **/
@ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
//@ConditionalOnSingleCandidate(DataSource.class)
@EnableConfigurationProperties({MpJoinProperties.class})
@ConditionalOnProperty(prefix = "fw.plus.mp-join", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MpJoinAutoConfiguration {
    private final MybatisPlusJoinProperties properties;

    public MpJoinAutoConfiguration(MpJoinProperties properties,
                                   ObjectProvider<MybatisPlusJoinPropertiesConsumer> propertiesConsumers,
                                   ObjectProvider<MybatisPlusJoinIfExistsConsumer> IfExistsConsumers) {
        MybatisPlusJoinProperties mybatisPlusJoinProperties = OrikaUtils.convert(properties,MybatisPlusJoinProperties.class);
        this.properties = Optional.ofNullable(propertiesConsumers.getIfAvailable()).map(c -> c.config(mybatisPlusJoinProperties)).orElse(mybatisPlusJoinProperties);
        ConfigProperties.banner = this.properties.getBanner();
        ConfigProperties.msCache = this.properties.isMsCache();
        ConfigProperties.tableAlias = this.properties.getTableAlias();
        ConfigProperties.joinPrefix = this.properties.getJoinPrefix();
        ConfigProperties.logicDelType = this.properties.getLogicDelType();
        ConfigProperties.subQueryAlias = this.properties.getSubQueryAlias();
        ConfigProperties.subTableLogic = this.properties.getSubTableLogic();
        ConfigProperties.mappingMaxCount = this.properties.getMappingMaxCount();
        ConfigProperties.Convert.IfExists = this.properties.getIfExists();
        ConfigProperties.ifExists = Optional.ofNullable(IfExistsConsumers.getIfAvailable())
                .map(m -> (MBiPredicate<Object, IfExistsSqlKeyWordEnum>) m)
                .orElse((val, key) -> ConfigProperties.Convert.IfExists.test(val));
        info("mybatis plus join properties config complete");
    }

    /**
     * 创建并配置MPJInterceptor拦截器
     *
     * 该方法通过Spring的@Bean注解定义了一个Bean，即MPJInterceptor实例
     * 使用@ConditionalOnClass注解确保只有在类路径中存在SqlSessionFactory和SqlSessionFactoryBean类时才创建这个Bean
     * 这使得拦截器的创建变得条件化，增强了组件的灵活性和可配置性
     *
     * @return 返回配置好的MPJInterceptor实例
     */
    @Bean
    @ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
    public MPJInterceptor mpjInterceptor() {
        return new MPJInterceptor();
    }

    /**
     * 创建并配置MPJInterceptorConfig实例
     *
     * 该方法是一个Spring的Bean定义方法，用于创建并配置一个MPJInterceptorConfig实例
     * 它使用了多个注解来定义Bean的特性：
     *
     * @Bean：告诉Spring这个方法会创建一个Bean实例
     * @Order(Integer.MIN_VALUE)：定义Bean的加载顺序，确保它在其他Bean之前加载
     * @ConditionalOnClass：当项目中存在SqlSessionFactory和SqlSessionFactoryBean类时，才创建这个Bean
     *
     * @param sqlSessionFactoryList 一个包含所有SqlSessionFactory的列表，用于MPJInterceptorConfig的初始化
     * @return 返回配置好的MPJInterceptorConfig实例
     */
    @Bean
    @Order(Integer.MIN_VALUE)
    @ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
    public MPJInterceptorConfig mpjInterceptorConfig(List<SqlSessionFactory> sqlSessionFactoryList) {
        // 创建MPJInterceptorConfig实例，传入SqlSessionFactory列表和配置中的banner信息
        return new MPJInterceptorConfig(sqlSessionFactoryList, this.properties.getBanner());
    }

    /**
     * 创建并配置MyBatis Plus Join SQL注入器
     *
     * 该方法通过Spring的Bean注解标记，确保在Spring启动时创建并初始化MPJSqlInjector实例
     * 使用Primary注解指定该Bean为主要的候选者，以便在多个同类Bean存在时被优先选择
     * JoinSqlInjectorCondition注解用于条件判断，确保在特定条件下才创建该Bean
     * Order注解设置优先级为最低，确保该Bean在所有其他Bean之前被创建和初始化
     * ConditionalOnBean注解确保只有在ISqlInjector类存在时才创建该Bean
     * ConditionalOnClass注解确保只有在SqlSessionFactory和SqlSessionFactoryBean类存在时才创建该Bean
     *
     * @param sqlInjector ISqlInjector接口的实现，用于自定义SQL注入逻辑
     * @return 返回配置好的MPJSqlInjector实例
     */
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

    /**
     * 创建MPJSqlInjector实例的Bean定义方法
     * 该方法在特定条件满足时，会自动创建并注册一个MPJSqlInjector的Bean实例
     *
     * @return MPJSqlInjector实例，用于处理特定的SQL注入逻辑
     */
    @Primary
    @Bean
    // 设置Bean的加载顺序，确保该Bean在所有其他Bean之前加载
    @Order(Integer.MIN_VALUE)
    // 当项目中缺少ISqlInjector接口的实现时，才创建该Bean
    //@ConditionalOnMissingBean({ISqlInjector.class})
    // 当项目中存在SqlSessionFactory和SqlSessionFactoryBean类时，才创建该Bean
    @ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
    public MPJSqlInjector mpjSqlInjectorOnMiss() {
        // 输出日志信息，表明MPJSqlInjector正在初始化
        info("mybatis plus join SqlInjector init");
        // 创建并返回一个新的MPJSqlInjector实例
        return new MPJSqlInjector();
    }

    /**
     * 根据配置属性决定是否打印信息
     * 此方法检查ConfigProperties中的banner属性，如果为true，则在日志中打印出提供的信息
     * 主要用于控制程序中的信息输出，以便根据需要启用或禁用信息提示
     *
     * @param info 要打印的信息内容
     */
    private static void info(String info) {
        if (ConfigProperties.banner) {
            log.info(info);
        }
    }

    @Configuration
    @Order(Integer.MIN_VALUE)
    @ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
    public static class MPJMappingConfig implements ApplicationListener<ApplicationReadyEvent> {
        public MPJMappingConfig() {
        }

        /**
         * 当应用准备就绪时执行特定操作
         * 该方法响应ApplicationReadyEvent事件，表示应用已经启动并准备就绪
         * 主要用于初始化那些需要在应用完全启动后才能执行的操作
         *
         * @param applicationReadyEvent 应用准备就绪事件，提供应用环境等信息
         */
        public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
            // 初始化MappingConfig，这通常包括加载或设置一些必要的配置
            // 之所以在这里初始化，是因为可能需要应用完全启动后才能访问到所有必要的资源或服务
            MappingConfig.init();
        }
    }

    @Configuration
    @ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
    public static class MPJSpringContext implements SpringContentUtils.SpringContext, BeanFactoryPostProcessor, ApplicationContextAware {
        private ApplicationContext applicationContext;
        private ListableBeanFactory listableBeanFactory;

        public MPJSpringContext() {
        }

        /**
         * 根据给定的类类型获取一个指定类型的Bean实例
         * 该方法利用泛型来实现类型安全的Bean实例获取，避免了类型转换的错误
         *
         * @param clazz 指定的类类型，用于确定返回的Bean实例的类型
         * @return 返回指定类型的Bean实例，具体的类型由clazz参数决定
         */
        public <T> T getBean(Class<T> clazz) {
            return (T)this.getBeanFactory().getBean(clazz);
        }

        /**
         * 根据指定类型获取所有符合条件的Bean实例
         * 此方法利用泛型来提高方法的通用性和灵活性，允许调用者指定感兴趣的Bean类型
         * 它转发调用到getBeanFactory()方法返回的Bean工厂中的getBeansOfType方法，体现了依赖委托的设计模式
         *
         * @param <T> 泛型参数，表示要获取的Bean的类型
         * @param clazz 指定的类型Class对象，用于过滤Bean实例
         */
        public <T> void getBeansOfType(Class<T> clazz) {
            this.getBeanFactory().getBeansOfType(clazz);
        }

        /**
         * 获取Bean工厂
         *
         * 此方法旨在提供对ListableBeanFactory实例的访问该方法首先检查applicationContext是否为空，
         * 如果为空，则返回listableBeanFactory实例；否则，返回applicationContext实例这样做的目的是确保
         * 在ApplicationContext可用时，优先使用ApplicationContext提供的更丰富的功能，同时在ApplicationContext
         * 缺乏时，仍能通过ListableBeanFactory访问基本功能
         *
         * @return ListableBeanFactory 返回ListableBeanFactory实例，具体返回applicationContext或listableBeanFactory，
         *         取决于applicationContext是否为空
         */
        private ListableBeanFactory getBeanFactory() {
            return (ListableBeanFactory)(this.applicationContext == null ? this.listableBeanFactory : this.applicationContext);
        }

        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = applicationContext;
            SpringContentUtils.setSpringContext(this);
        }

        /**
         * 在Spring的Bean工厂初始化完成后执行后处理
         * 此方法主要用于获取SqlSessionFactory类型的Bean定义，并记录相关信息
         *
         * @param beanFactory Spring的可配置Bean工厂
         * @throws BeansException 当处理Bean时发生错误
         */
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            // 设置当前类的listableBeanFactory属性为传入的Bean工厂
            this.listableBeanFactory = beanFactory;
            // 设置Spring上下文以便在SpringContentUtils中使用
            SpringContentUtils.setSpringContext(this);
            try {
                // 获取所有SqlSessionFactory类型Bean的名称
                String[] names = beanFactory.getBeanNamesForType(SqlSessionFactory.class);

                // 遍历所有SqlSessionFactory类型的Bean名称
                for(String name : names) {
                    // 获取当前Bean的定义
                    BeanDefinition definition = beanFactory.getBeanDefinition(name);
                    // 记录SqlSessionFactory Bean定义的信息
                    info(String.format("MPJ SqlSessionFactory bean definition: %s factoryBeanName: %s factoryMethodName: %s source: %s", name, definition.getFactoryBeanName(), definition.getFactoryMethodName(), definition.getSource()));
                }
            } catch (Throwable e) {
                // 异常处理：此处未处理异常，可根据实际需要添加日志记录或抛出异常
            }
        }
    }
}
