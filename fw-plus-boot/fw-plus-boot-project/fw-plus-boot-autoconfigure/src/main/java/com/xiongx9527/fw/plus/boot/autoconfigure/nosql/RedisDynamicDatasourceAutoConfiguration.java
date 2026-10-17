package com.xiongx9527.fw.plus.boot.autoconfigure.nosql;

import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.RedisMultiDataSourceRegister;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.client.RedisMultiSourceClient;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.condition.ConditionalOnExistingProperty;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.condition.ConditionalOnMissingProperty;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.config.DynamicRedisTemplateFactory;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.config.properties.RedisDataSourceProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.helper.ApplicationContextHelper;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.helper.DynamicRedisHelper;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.helper.RedisHelper;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.runner.RedisMultiSourceRegisterRunner;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.template.DynamicRedisTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.data.redis.autoconfigure.JedisClientConfigurationBuilderCustomizer;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.lang.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.xiongx9527.fw.plus.nosql.redis.constant.DynamicRedisConstants.DefaultRedisHelperName;
/**
 * redis动态数据源自动配置
 *
 * @author wenpan 2023/03/24 21:51
 */
@Configuration
//@ComponentScan({"org.enhance.redis.client"})
@EnableConfigurationProperties({RedisDataSourceProperties.class})
@ConditionalOnProperty(prefix = "fw.plus.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.springframework.data.redis.connection.RedisConnectionFactory",
"com.xiongx9527.fw.plus.nosql.redis.util.EnvironmentUtil"})
@Import({RedisHelper.class,
        RedisMultiDataSourceRegister.class, RedisMultiSourceRegisterRunner.class, RedisMultiSourceClient.class})
public class RedisDynamicDatasourceAutoConfiguration {

    /**
     * 创建并配置ApplicationContextHelper Bean实例
     *
     * @return ApplicationContextHelper Spring容器帮助类实例
     */
    @Bean
    public ApplicationContextHelper applicationContextHelper() {
        return new ApplicationContextHelper();
    }

    /**
     * 创建并配置一个RedisTemplate实例
     * 该方法通过Spring的依赖注入机制，条件性地创建一个RedisTemplate实例
     * 如果容器中已经存在RedisTemplate实例，则不会创建新的实例
     *
     * @param redisConnectionFactory Redis连接工厂，用于创建与Redis服务器的连接
     * @return 配置好的RedisTemplate实例，用于操作Redis数据库
     */
    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        buildRedisTemplate(redisTemplate, redisConnectionFactory);
        return redisTemplate;
    }

    /**
     * 创建并配置一个StringRedisTemplate实例
     * 该方法通过Spring的条件注解来决定是否执行，只有在当前上下文中没有StringRedisTemplate类型的Bean时才会创建
     *
     * @param redisConnectionFactory Redis连接工厂，用于创建和管理Redis的连接
     * @return 返回一个配置好的StringRedisTemplate实例
     */
    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        buildRedisTemplate(redisTemplate, redisConnectionFactory);
        return redisTemplate;
    }

    /**
     * 配置RedisTemplate以使用StringRedisSerializer进行序列化和反序列化操作
     * 此方法专注于设置RedisTemplate的序列化方式，确保所有类型的键和值都使用StringRedisSerializer
     * 这对于需要将所有数据作为字符串处理的场景特别有用
     *
     * @param redisTemplate        需要配置的RedisTemplate实例
     * @param redisConnectionFactory Redis连接工厂，用于创建和管理Redis连接
     */
    private static void buildRedisTemplate(RedisTemplate<String, String> redisTemplate,
                                           RedisConnectionFactory redisConnectionFactory) {
        // 创建一个StringRedisSerializer实例，用于后续的序列化设置
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 设置RedisTemplate的键序列化方式为StringRedisSerializer
        redisTemplate.setKeySerializer(stringRedisSerializer);
        // 设置RedisTemplate的字符串序列化方式为StringRedisSerializer
        redisTemplate.setStringSerializer(stringRedisSerializer);
        // 设置RedisTemplate的默认序列化方式为StringRedisSerializer
        redisTemplate.setDefaultSerializer(stringRedisSerializer);
        // 设置RedisTemplate的哈希键序列化方式为StringRedisSerializer
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        // 设置RedisTemplate的哈希值序列化方式为StringRedisSerializer
        redisTemplate.setHashValueSerializer(stringRedisSerializer);
        // 设置RedisTemplate的值序列化方式为StringRedisSerializer
        redisTemplate.setValueSerializer(stringRedisSerializer);
        // 设置RedisTemplate的连接工厂为提供的RedisConnectionFactory实例
        redisTemplate.setConnectionFactory(redisConnectionFactory);
    }


    /**
     * 注册Redis多数据源
     *
     * @param environment 环境变量，用于配置数据源
     * @return RedisMultiDataSourceRegister实例，用于管理多个Redis数据源
     *
     * 此方法在满足以下条件时会被调用：
     * 1. 容器中存在RedisMultiSourceClient类
     * 2. 容器中不存在RedisMultiDataSourceRegister类
     *
     * 这种条件性 bean 创建确保了在需要时才创建RedisMultiDataSourceRegister bean，
     * 并且避免了重复创建，节省了系统资源
     */
    @Bean
    @ConditionalOnBean(value = {RedisMultiSourceClient.class})
    @ConditionalOnMissingBean(value = RedisMultiDataSourceRegister.class)
    public RedisMultiDataSourceRegister redisMultiDataSourceRegistrar(Environment environment) {
        RedisMultiDataSourceRegister redisMultiDataSourceRegistrar = new RedisMultiDataSourceRegister();
        redisMultiDataSourceRegistrar.setEnvironment(environment);
        return redisMultiDataSourceRegistrar;
    }


    /**
     * 创建并配置RedisHelper bean
     *
     * 该方法通过注解配置了RedisHelper bean，使其成为Spring应用上下文中的一部分
     * RedisHelper是一个工具类，用于简化与Redis数据库的交互
     *
     * @param redisTemplate 一个用于操作Redis的模板对象，它被注入到RedisHelper中以执行实际的Redis操作
     * @return RedisHelper的实例，该实例使用提供的redisTemplate进行操作
     */
    @Primary
    @ConditionalOnStaticRedisHelper
    @Bean(name = {"redisHelper", DefaultRedisHelperName.DEFAULT, DefaultRedisHelperName.DEFAULT_REDIS_HELPER})
    public RedisHelper redisHelper(RedisTemplate<String, String> redisTemplate) {
        return new RedisHelper(redisTemplate);
    }

    /**
     * 默认数据源的动态redisHelper
     * 这些参数由 jedis 或 lettuce 客户端帮我们自动配置并注入到容器
     */
    @Primary
    @ConditionalOnDynamicRedisHelper
    @Bean(name = {"redisHelper", DefaultRedisHelperName.DEFAULT, DefaultRedisHelperName.DEFAULT_REDIS_HELPER})
    public RedisHelper dynamicRedisHelper(StringRedisTemplate redisTemplate,
                                          DataRedisProperties redisProperties,
                                          ObjectProvider<RedisSentinelConfiguration> sentinelConfiguration,
                                          ObjectProvider<RedisClusterConfiguration> clusterConfiguration,
                                          ObjectProvider<List<JedisClientConfigurationBuilderCustomizer>> jedisBuilderCustomizers,
                                          ObjectProvider<List<LettuceClientConfigurationBuilderCustomizer>> builderCustomizers) {
        // 构建动态RedisTemplate工厂
        DynamicRedisTemplateFactory<String, String> dynamicRedisTemplateFactory =
                new DynamicRedisTemplateFactory<>(redisProperties,
                        sentinelConfiguration.getIfAvailable(),
                        clusterConfiguration.getIfAvailable(),
                        jedisBuilderCustomizers.getIfAvailable(),
                        builderCustomizers.getIfAvailable());
        // ======================================================================================================
        // 这里在注入的时候默认值注入一个默认的redisTemplate，以及将这个redisTemplate放入到map中，该redisTemplate
        // 操作的是配置文件中使用spring.redis.database属性指定的db（若不显示指定，则使用的0号db）
        // 注入的时候值放入这个默认的的redisTemplate到map中，在使用的时候如果需要动态切换库那么会通过连接工厂
        // 重新去创建一个redisTemplate，并缓存到map中（懒加载模式），并不会一次性创建出多个redisTemplate然后缓存起来（连接很昂贵）
        // ======================================================================================================

        DynamicRedisTemplate<String, String> dynamicRedisTemplate = new DynamicRedisTemplate<>(dynamicRedisTemplateFactory);
        // 当不指定库时，默认使用的RedisTemplate来操作Redis(直接获取容器中的)
        dynamicRedisTemplate.setDefaultRedisTemplate(redisTemplate);
        Map<Object, RedisTemplate<String, String>> map = new HashMap<>(8);
        // 配置文件中指定使用几号db
        map.put(redisProperties.getDatabase(), redisTemplate);
        // 将redisTemplate缓存起来
        dynamicRedisTemplate.setRedisTemplates(map);

        return new DynamicRedisHelper(dynamicRedisTemplate);
    }



    /**
     * 配置并返回一个HashOperations对象，用于执行Redis中的Hash操作
     *
     * @param redisTemplate StringRedisTemplate对象，用于与Redis进行交互和操作
     * @return HashOperations<String, String, String>对象，支持Hash类型的Redis操作
     */
    @Bean
    public HashOperations<String, String, String> hashOperations(StringRedisTemplate redisTemplate) {
        return redisTemplate.opsForHash();
    }

    /**
     * 配置并返回一个用于操作String类型值的Redis模板对象
     * 该方法通过Spring的@Bean注解声明，使得Spring容器在初始化时自动调用此方法
     * 并将返回的对象作为Bean注入到Spring应用上下文中
     *
     * @param redisTemplate 由Spring框架自动注入的StringRedisTemplate对象
     *                      该对象是访问Redis数据库的模板，专门用于处理String类型的键和值
     * @return 返回一个ValueOperations<String, String>对象，用于执行Redis中String类型数据的操作
     */
    @Bean
    public ValueOperations<String, String> valueOperations(StringRedisTemplate redisTemplate) {
        return redisTemplate.opsForValue();
    }


    /**
     * 配置Redis列表操作模板
     *
     * 该方法通过Spring的@Bean注解定义了一个Bean，类型为ListOperations，用于操作Redis中的列表数据结构
     * 它利用StringRedisTemplate来创建和管理列表操作的实例
     *
     * @param redisTemplate 用于操作Redis的模板对象，通过该对象可以执行各种Redis命令
     * @return 返回一个ListOperations对象，专门用于操作Redis中的列表数据结构
     */
    @Bean
    public ListOperations<String, String> listOperations(StringRedisTemplate redisTemplate) {
        return redisTemplate.opsForList();
    }


    /**
     * 配置并返回一个用于操作Redis中Set数据结构的SetOperations对象
     * 此方法通过Spring的@Bean注解声明，使得该方法返回的对象能够作为Spring容器中的一个Bean
     * 这样做是为了方便在其他地方自动注入SetOperations对象，从而进行Redis Set的操作
     *
     * @param redisTemplate StringRedisTemplate对象，提供了对Redis数据库操作的各种方法的入口
     * @return SetOperations<String, String> 返回一个操作Redis Set数据结构的SetOperations对象
     */
    @Bean
    public SetOperations<String, String> setOperations(StringRedisTemplate redisTemplate) {
        return redisTemplate.opsForSet();
    }


    /**
     * 配置Bean以提供ZSet操作的实例
     * 该方法通过StringRedisTemplate创建一个ZSetOperations的实例，用于操作Redis中的有序集合（ZSet）
     *
     * @param redisTemplate StringRedisTemplate实例，用于访问Redis数据库
     * @return ZSetOperations<String, String> RedisTemplate中用于操作有序集合的实例
     */
    @Bean
    public ZSetOperations<String, String> zSetOperations(StringRedisTemplate redisTemplate) {
        return redisTemplate.opsForZSet();
    }

    @Documented
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Conditional(OnDynamicRedisHelperCondition.class)
    @interface ConditionalOnDynamicRedisHelper {
    }

    /**
     * dynamic.redis.dynamic-database=true 且 非集群模式下，则创建动态的 RedisHelper，可以切换 Redis DB.
     */
    private static class OnDynamicRedisHelperCondition extends AllNestedConditions {

        /**
         * 注册bean时生效
         */
        public OnDynamicRedisHelperCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        /**
         * 开启db动态切换
         */
        @ConditionalOnProperty(prefix = RedisDataSourceProperties.PREFIX,
                name = "dynamic-database", havingValue = "true", matchIfMissing = true)
        static class OnDynamicRedisHelper {
        }

        /**
         * 非集群配置
         */
        @ConditionalOnMissingProperty("spring.redis.cluster.nodes")
        static class OnSingleCluster {
        }
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Conditional(OnStaticRedisHelperCondition.class)
    @interface ConditionalOnStaticRedisHelper {

    }

    /**
     * dynamic.redis.dynamic-database=false或者集群模式下，则创建静态的 RedisHelper，禁止切换 Redis db
     */
    private static class OnStaticRedisHelperCondition extends AnyNestedCondition {

        public OnStaticRedisHelperCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        /**
         * 关闭db动态切换
         */
        @ConditionalOnProperty(prefix = RedisDataSourceProperties.PREFIX, name = "dynamic-database", havingValue = "false")
        static class OnStaticRedisHelper {
        }

        /**
         * 非集群模式
         * 存在spring.redis.cluster.nodes配置(即集群模式禁止切换db，Redis官方规定集群模式下只有db0)
         */
        @ConditionalOnExistingProperty(value = {"spring.redis.cluster.nodes"})
        static class OnRedisCluster {
        }
    }
}
