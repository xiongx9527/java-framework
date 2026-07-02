package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.MagicDynamicRedisDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.lang.reflect.Field;

@Slf4j
@MagicModule("redis")
public class RedisModule extends org.ssssssss.magicapi.redis.RedisModule implements DynamicAttribute<RedisModule, RedisModule>, DynamicModule<RedisModule> {

    /**
     * 动态Redis数据源管理器
     * 用于管理和获取动态创建的Redis连接工厂
     */
    private final MagicDynamicRedisDataSource magicDynamicRedisDataSource;

    /**
     * RedisTemplate字段的反射缓存
     * 用于缓存通过反射获取的父类redisTemplate字段，避免重复进行反射操作，提高性能
     * 使用volatile关键字保证多线程环境下的可见性
     */
    private volatile Field redisTemplateField = null;

    @Value("${spring.redis.host:}")
    private String host;
    @Value("${spring.redis.port:}")
    private String port;
    @Value("${spring.redis.username:}")
    private String usermame;
    @Value("${spring.redis.password:}")
    private String password;
    @Value("${spring.redis.database:}")
    private String database;

    /**
     * Redis模块构造函数
     * 初始化Redis模块，设置动态数据源管理器并调用父类构造函数初始化连接工厂
     *
     * @param magicDynamicRedisDataSource 动态Redis数据源管理器
     * @param connectionFactory           Redis连接工厂
     */
    public RedisModule(MagicDynamicRedisDataSource magicDynamicRedisDataSource, RedisConnectionFactory connectionFactory) {
        super(connectionFactory);
        this.magicDynamicRedisDataSource = magicDynamicRedisDataSource;
    }

    /**
     * 获取Redis模板对象
     * 通过反射方式从父类获取redisTemplate字段的值，使用缓存机制提高性能
     * 通过双重检查锁定模式确保线程安全并避免重复的反射操作
     *
     * @return StringRedisTemplate实例，如果获取失败则返回null
     */
    public StringRedisTemplate getRedisTemplate() {
        try {
            // 缓存Field对象以提高性能
            if (redisTemplateField == null) {
                synchronized (RedisModule.class) {
                    if (redisTemplateField == null) {
                        Field field = RedisModule.class.getSuperclass().getDeclaredField("redisTemplate");
                        field.setAccessible(true);
                        redisTemplateField = field;
                    }
                }
            }
            return (StringRedisTemplate) redisTemplateField.get(this);
        } catch (NoSuchFieldException e) {
            log.error("找不到redisTemplate字段", e);
        } catch (IllegalAccessException e) {
            log.error("无法访问redisTemplate字段", e);
        } catch (Exception e) {
            log.error("获取redisTemplate字段时发生未知异常", e);
        }
        return null;
    }

    public MagicDynamicRedisDataSource getMagicDynamicRedisDataSource() {
        return this.magicDynamicRedisDataSource;
    }

    /**
     * 创建并配置一个RedisModule实例。
     *
     * @param key 用于标识数据源的键，用于获取对应的Redis连接信息。
     * @return 配置完成的RedisModule实例。
     */
    private RedisModule createRedisModule(String key) {
        // 初始化RedisModule实例，并传入动态数据源和连接工厂
        RedisModule redisModule = new RedisModule(this.magicDynamicRedisDataSource, this.magicDynamicRedisDataSource.getRedisConnectionFactory(key));

        // 获取指定key对应的数据源信息
        RedisDataSourceInfo redisDataSourceInfo = this.magicDynamicRedisDataSource.getRedisDataSourceInfo(key);

        // 如果数据源信息存在，则设置RedisModule的相关属性
        if (redisDataSourceInfo != null) {
            redisModule.setHost(redisDataSourceInfo.getHostname());
            redisModule.setPort(redisDataSourceInfo.getPort());
            redisModule.setUsername(redisDataSourceInfo.getUsername());
            redisModule.setPassword(redisDataSourceInfo.getPassword());
            redisModule.setDatabase(redisDataSourceInfo.getDatabase());
        }

        // 返回配置好的RedisModule实例
        return redisModule;
    }

    /**
     * 获取动态Redis模块实例
     * 根据脚本上下文中的数据源配置获取对应的Redis连接工厂，并创建新的RedisModule实例
     *
     * @param context MagicScript脚本上下文，包含数据源配置信息
     * @return 包含指定连接工厂的RedisModule实例，如果未指定数据源则返回当前实例
     */
    @Override
    public RedisModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) return this;
        return createRedisModule(dataSourceKey);
    }

    /**
     * 获取指定key的动态Redis模块实例
     * 根据key从动态数据源中获取对应的Redis连接工厂，并创建新的RedisModule实例
     *
     * @param key Redis连接工厂的唯一标识符
     * @return 包含指定连接工厂的RedisModule实例
     */
    @Override
    public RedisModule getDynamicAttribute(String key) {
        return createRedisModule(key);
    }


    // Getter 和 Setter 方法
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUsername() {
        return usermame; // 注意字段名拼写错误，应为 username
    }

    public void setUsername(String username) {
        this.usermame = username; // 注意字段名拼写错误，应为 username
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;

    }
}
