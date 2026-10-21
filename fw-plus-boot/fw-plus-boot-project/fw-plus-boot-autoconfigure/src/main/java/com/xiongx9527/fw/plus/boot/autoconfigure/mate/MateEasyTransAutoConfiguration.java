package com.xiongx9527.fw.plus.boot.autoconfigure.mate;

import com.fhs.cache.service.BothCacheService;
import com.fhs.cache.service.RedisCacheService;
import com.fhs.common.constant.TransConfig;
import com.fhs.common.spring.SpringContextUtil;
import com.fhs.core.trans.convert.Convert;
import com.fhs.core.trans.util.ConvertUtil;
import com.fhs.trans.advice.ReleaseTransCacheAdvice;
import com.fhs.trans.ds.DataSourceSetter;
import com.fhs.trans.listener.TransMessageListener;
import com.fhs.trans.service.impl.*;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.easytrans.service.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;

/**
 * 翻译自动配置
 */
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "fw.plus.mate.easy-trans", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"com.xiongx9527.fw.plus.easytrans.service.MateEasyTransTransService"})
@Configuration(proxyBeanMethods = false)
public class MateEasyTransAutoConfiguration implements InitializingBean , BeanFactoryPostProcessor {
    //@Value("${fw.plus.mate.easy-trans.is-enable-redis:false}")
    //private boolean isEnableRedis;
    @Value("${fw.plus.mate.easy-trans.multiple-data-sources:false}")
    private boolean multipleDataSources;

    @Autowired(required = false)
    private DataSourceSetter dataSourceSetter;

    /**
     * 创建并配置MateEasyTransTransService的Bean
     *
     * 该方法通过Spring的@Bean注解定义了一个Bean，使得MateEasyTransTransService类的实例
     * 可以在Spring容器中作为依赖被注入到其他Bean中这个服务类主要用于处理
     * EasyTrans的传输服务，可能包括数据传输、转换等操作
     *
     * @return MateEasyTransTransService 返回配置的MateEasyTransTransService实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MateEasyTransTransService transService() {
        return new MateEasyTransTransService();
    }

    /**
     * 创建并配置一个MateEasyTransResponseBodyAdvice bean
     *
     * MateEasyTransResponseBodyAdvice是一个处理响应体的Advice，用于在响应体被序列化到HTTP响应之前进行处理
     * 这种Advice通常用于添加额外的信息到响应中，或者对响应体进行转换
     *
     * @return MateEasyTransResponseBodyAdvice的实例，用于处理响应体
     */
    //@Bean
    //public MateEasyTransResponseBodyAdvice mateEasyTransResponseBodyAdvice(TransService transService) {
    //    return new MateEasyTransResponseBodyAdvice(transService);
    //}

    /**
     * 创建并配置TransService的Bean
     * 该方法由Spring框架调用，用于实例化TransService对象，并将其作为Bean纳入Spring容器管理
     *
     * @return TransService的实例，用于提供翻译相关服务
     */


    /**
     * 创建AutoTransService的Bean实例
     *
     * @return AutoTransService的实例
     *
     * 使用@Bean注解来声明一个Bean，以便在Spring容器中管理该Bean的生命周期和依赖关系
     * 使用@DependsOn注解指定该Bean依赖于springContextUtil，确保springContextUtil在AutoTransService初始化之前被创建
     * 这样做是为了确保所有依赖的组件都已经准备好，可以避免初始化错误或依赖注入的问题
     */
    @Bean
    @ConditionalOnMissingBean
    @DependsOn({"springContextUtil"})
    public MateEasyTransAutoTransService autoTransService() {
        return new MateEasyTransAutoTransService();
    }


    /**
     * 配置bothCacheService作为Bean，用于缓存服务
     * 此方法说明了如何创建和配置一个缓存服务实例，该实例可以与Redis缓存服务一起工作
     *
     * @param redisCacheService Redis缓存服务实例，用于与Redis进行交互可能为null，因为它被标记为@Autowired(required = false)
     * @return 返回一个配置好的MateEasyTransBothCacheService实例，它使用了提供的Redis缓存服务
     */
    @Bean
    @ConditionalOnMissingBean
    public MateEasyTransBothCacheService bothCacheService(@Autowired(required = false) RedisCacheService redisCacheService) {
        return new MateEasyTransBothCacheService(redisCacheService);
    }

    /**
     * 创建并配置一个MateEasyTransEnumTransService类型的Bean
     * 此Bean用于提供枚举转换服务，是轻量级分布式事务解决方案的一部分
     *
     * @param bothCacheService 一个BothCacheService类型的参数，用于缓存操作
     * @param redisCacheService 一个可选的RedisCacheService类型的参数，用于Redis缓存操作
     * @return MateEasyTransEnumTransService实例，用于枚举转换操作
     */
    @Bean
    @ConditionalOnMissingBean
    public MateEasyTransDictionaryTransService dictionaryTransService(BothCacheService<String> bothCacheService,
                                                                      @Autowired(required = false) RedisCacheService<String> redisCacheService) {
        return new MateEasyTransDictionaryTransService(bothCacheService,redisCacheService);
    }

    /**
     * 创建并配置一个ReleaseTransCacheAdvice bean
     *
     * ReleaseTransCacheAdvice是一个处理释放传输缓存的顾问类，用于在特定操作后清理或更新缓存
     * 这个方法定义了一个bean，使其可以在应用程序中注入和使用
     *
     * @return 新建的ReleaseTransCacheAdvice对象实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ReleaseTransCacheAdvice releaseTransCacheAdvice() {
        return new ReleaseTransCacheAdvice();
    }

    /**
     * 创建并配置枚举转换服务的Bean
     * 此方法定义了一个Bean，用于在应用程序中提供枚举转换的服务
     * 主要用途是自动转换枚举类型的属性，根据配置好的转换规则
     *
     * @return MateEasyTransEnumTransService 枚举转换服务的实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MateEasyTransEnumTransService enumTransService() {
        return new MateEasyTransEnumTransService();
    }


    /**
     * 创建并配置MateEasyTransBooleanTransService作为Spring框架的Bean
     * 此方法定义了一个Bean，使得Spring框架能够自动检测并管理该Bean的生命周期
     *
     * @return MateEasyTransBooleanTransService的新实例，提供业务逻辑的实现
     */
    @Bean
    @ConditionalOnMissingBean
    public MateEasyTransBooleanTransService booleanTransService() {
        return new MateEasyTransBooleanTransService();
    }

    /**
     * 创建并配置一个SimpleTransService实例
     * 此方法在满足特定条件时被调用，即容器中存在指定的SimpleTransDiver bean时
     * 通过此方法，我们定义了如何构造和初始化SimpleTransService实例，
     * 同时确保了所需的依赖项（SimpleTransDiver）已被满足
     *
     * @param dirver SimpleTransService的依赖项，用于处理事务
     * @return 返回一个初始化完毕的SimpleTransService实例
     */
    @Bean
    @ConditionalOnMissingBean
    @Primary
    @ConditionalOnBean({SimpleTransService.SimpleTransDiver.class})
    public MateEasyTransSimpleTransService simpleTransService(SimpleTransService.SimpleTransDiver dirver) {
        // 创建SimpleTransService实例
        MateEasyTransSimpleTransService result = new MateEasyTransSimpleTransService();
        // 注册传入的事务处理驱动器
        result.regsiterTransDiver(dirver);
        // 返回初始化完毕的实例
        return result;
    }

    /**
     * 创建一个负载均衡的RestTemplate实例
     * 该方法在没有现有RestTemplate bean的情况下被调用
     * 使用RestTemplateBuilder进行构建，确保了构建过程的灵活性和可配置性
     *
     * @param builder Spring提供的RestTemplateBuilder实例，用于构建RestTemplate
     * @return 返回一个负载均衡的RestTemplate实例，用于执行HTTP请求
     */
    /**
    @Bean
    @LoadBalanced
    @ConditionalOnMissingBean({RestTemplate.class})
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
    **/


    /**
     * 创建并配置MateEasyTransTransCacheManager实例，用于管理事务缓存。
     * 该缓存管理器整合了RPC事务服务、简单事务服务和Redis缓存服务，
     * 以提供全面的事务处理和缓存功能。
     *
     * @param rpcTransService RPC事务服务实例，可选。
     * @param simpleTransService 简单事务服务实例，可选。
     * @param redisCacheService Redis缓存服务实例，用于缓存事务相关数据，可选。
     * @return MateEasyTransTransCacheManager实例，用于管理事务缓存。
     */
    @Bean
    @ConditionalOnMissingBean
    public MateEasyTransTransCacheManager transCacheManager(@Autowired(required = false) MateEasyTransRpcTransService rpcTransService,
                                                            @Autowired(required = false) MateEasyTransSimpleTransService simpleTransService,
                                                            @Autowired(required = false) RedisCacheService<String> redisCacheService) {
        return new MateEasyTransTransCacheManager(rpcTransService, simpleTransService, redisCacheService);
    }

    /**
     * 创建并配置RpcTransService实例
     * 该方法在SimpleTransService.SimpleTransDiver和RestTemplate都可用时执行
     *
     * @param dirver SimpleTransService的驱动器，用于处理传输逻辑
     * @param restTemplate 用于发起HTTP请求的模板类
     * @return 返回一个配置好的RpcTransService实例
     */
    @Bean
    @ConditionalOnBean({SimpleTransService.SimpleTransDiver.class,RestTemplate.class})
    public MateEasyTransRpcTransService rpcTransService(SimpleTransService.SimpleTransDiver dirver, RestTemplate restTemplate) {
        // 创建RpcTransService实例
        MateEasyTransRpcTransService result = new MateEasyTransRpcTransService();
        // 注册传输驱动器
        result.regsiterTransDiver(dirver);
        // 设置REST模板
        result.setRestTemplate(restTemplate);
        // 返回配置好的实例
        return result;
    }

    /**
     * 创建MateEasyTransProxyController的Bean实例
     * 该方法在SimpleTransService.SimpleTransDiver类存在时被条件性地执行
     *
     * @param dirver SimpleTransService.SimpleTransDiver类型的参数，用于控制传输驱动
     * @return 返回MateEasyTransProxyController的实例，用于处理传输代理控制
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({SimpleTransService.SimpleTransDiver.class})
    public MateEasyTransProxyController transProxyController(SimpleTransService.SimpleTransDiver dirver) {
        MateEasyTransProxyController result = new MateEasyTransProxyController();
        result.setSimpleTransDiver(dirver);
        return result;
    }

    /**
     * 创建并配置一个MateEasyTransTransMethodArgAop实例
     *
     * @param transService 事务管理服务，用于处理分布式事务
     * @return MateEasyTransTransMethodArgAop的实例，用于拦截和处理方法参数
     */
    @Bean
    @ConditionalOnMissingBean
    public MateEasyTransTransMethodArgAop mateEasyTransTransMethodArgAop(TransService transService) {
        return new MateEasyTransTransMethodArgAop(transService);
    }

    /**
     * 创建并配置一个MateEasyTransTransMethodResultAop实例
     *
     * @param transService 翻译服务，用于处理翻译相关操作
     * @return MateEasyTransTransMethodResultAop实例，用于处理翻译方法的结果
     */
    @Bean
    @ConditionalOnMissingBean
    public MateEasyTransTransMethodResultAop transMethodResultAop(TransService transService) {
        return new MateEasyTransTransMethodResultAop(transService);
    }



    /**
     * 创建并配置SpringContextUtil的Bean
     *
     * 该方法通过{@link Bean}注解定义了一个Bean，名称为"springContextUtil"，
     * 这使得Spring容器能够管理和使用该工具类实例
     *
     * @return 返回SpringContextUtil的一个实例，用于获取ApplicationContext
     */
    @Bean({"springContextUtil"})
    public SpringContextUtil springContextUtil() {
        return new SpringContextUtil();
    }

    /**
     * 自动注入Convert实例到ConvertUtil
     *
     * @param convert Convert实例，用于格式转换
     */
    @Autowired(
        required = false
    )
    public void setConvertUtil(Convert convert) {
        ConvertUtil.setConvert(convert);
    }




    /**
     * 创建并配置一个TransMessageListener bean
     *
     * 该方法无需外部参数，通过new关键字直接实例化TransMessageListener对象
     * 主要作用是监听和处理转换消息，这通常用于需要监听特定消息或事件
     * 并执行相应处理的场景中
     *
     * @return TransMessageListener 实例，用于监听和处理消息
     */
    @Bean
    @ConditionalOnMissingBean
    public TransMessageListener transMessageListener() {
        return new TransMessageListener();
    }

    /**
     * 创建并配置一个Redis消息监听器容器
     *
     * 此方法用于构建一个RedisMessageListenerContainer实例，该实例用于监听指定主题的消息并进行处理
     * 它需要一个RedisConnectionFactory来建立与Redis的连接，以及一个MessageListenerAdapter来处理接收到的消息
     *
     * @param connectionFactory Redis连接工厂，用于创建与Redis的连接
     * @param listenerAdapter 消息监听器适配器，用于处理接收到的消息
     * @return 返回配置好的RedisMessageListenerContainer实例
     */
    @ConditionalOnProperty(
            name = {"fw.plus.mate.easy-trans.is-enable-redis"},
            havingValue = "true"
    )
    @Bean
    @ConditionalOnMissingBean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
        // 创建RedisMessageListenerContainer实例
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        // 设置连接工厂
        container.setConnectionFactory(connectionFactory);
        // 添加消息监听器，监听符合"trans"模式的主题
        container.addMessageListener(listenerAdapter, new PatternTopic("trans"));
        // 创建一个字符串类型的Redis序列化器
        StringRedisSerializer seria = new StringRedisSerializer();
        // 设置主题序列化方式
        container.setTopicSerializer(seria);
        // 返回配置好的RedisMessageListenerContainer实例
        return container;
    }

    /**
     * 创建并配置一个MessageListenerAdapter实例
     * 该实例用于监听和处理Redis消息队列中的消息
     *
     * @param receiver TransMessageListener的实例，用于处理接收到的消息
     * @param redisTemplate RedisTemplate的实例，提供对Redis的操作支持
     * @return 返回配置好的MessageListenerAdapter实例
     */
    @ConditionalOnProperty(
            name = {"fw.plus.mate.easy-trans.is-enable-redis"},
            havingValue = "true"
    )
    @Bean
    @ConditionalOnMissingBean
    public MessageListenerAdapter listenerAdapter(TransMessageListener receiver, RedisTemplate redisTemplate) {
        // 创建MessageListenerAdapter实例，并指定消息处理方法为handelMsg
        MessageListenerAdapter result = new MessageListenerAdapter(receiver, "handelMsg");
        // 设置序列化方式，以确保消息的正确解析和存储
        result.setSerializer(redisTemplate.getValueSerializer());
        return result;
    }

    /**
     * 创建并配置RedisCacheService实例
     * 该方法在满足特定条件时被调用，即配置属性"fw.plus.mate.easy-trans.is-enable-redis"的值为"true"
     *
     * @param redisTemplate 用于与Redis数据库交互的模板类，提供对Redis数据存储的基本操作
     * @param autoTransService 自动事务管理服务，用于处理事务相关逻辑
     * @return 返回一个配置好的RedisCacheService实例，用于缓存操作
     */
    @ConditionalOnProperty(
            name = {"fw.plus.mate.easy-trans.is-enable-redis"},
            havingValue = "true"
    )
    @Bean
    @ConditionalOnMissingBean
    public MateEasyTransRedisCacheService redisCacheService(RedisTemplate redisTemplate, MateEasyTransAutoTransService autoTransService) {
        // 创建RedisCacheService实例
        MateEasyTransRedisCacheService redisCacheService = new MateEasyTransRedisCacheService();

        // 设置RedisTemplate，用于缓存服务与Redis数据库的交互
        redisCacheService.setRedisTemplate(redisTemplate);
        // 同时设置StrRedisTemplate，可能用于处理字符串类型的缓存操作
        redisCacheService.setStrRedisTemplate(redisTemplate);

        // 将RedisCacheService实例设置到AutoTransService中，用于事务管理服务的缓存操作
        autoTransService.setRedisTransCache(redisCacheService);

        // 返回配置好的RedisCacheService实例
        return redisCacheService;
    }


    /**
     * 在所有属性设置完成后执行的方法
     *
     * 此方法主要用于配置事务的多数据源支持如果当前环境需要使用多个数据源，
     * 但没有提供用于切换数据源的dataSourceSetter，将抛出异常提醒用户需要设置
     *
     * @throws IllegalArgumentException 如果开启了多数据源支持，但未提供dataSourceSetter
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 设置是否有多数据源
        TransConfig.MULTIPLE_DATA_SOURCES = this.multipleDataSources;
        // 检查是否开启了多数据源支持
        if (TransConfig.MULTIPLE_DATA_SOURCES && null == this.dataSourceSetter) {
            // 如果开启了多数据源支持，但没有提供dataSourceSetter，抛出异常
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.mate.easytrans.dataSourceSetterRequired"));
        } else {
            // 如果没有开启多数据源支持，或者提供了dataSourceSetter，进行配置
            TransConfig.dataSourceSetter = this.dataSourceSetter;
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }
}
