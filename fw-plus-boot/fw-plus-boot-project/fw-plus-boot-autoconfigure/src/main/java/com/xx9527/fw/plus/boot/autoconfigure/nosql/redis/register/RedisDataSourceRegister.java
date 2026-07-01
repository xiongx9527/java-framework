package com.xx9527.fw.plus.boot.autoconfigure.nosql.redis.register;

import com.xx9527.fw.plus.boot.autoconfigure.nosql.redis.helper.RedisHelper;
import com.xx9527.fw.plus.nosql.redis.constant.DynamicRedisConstants;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源注册
 *
 * @author Mr_wenpan@163.com 2021/09/06 10:30
 */
public class RedisDataSourceRegister {

    /**
     * 多数据源redisTemplate注册（不包括默认数据源）
     */
    private final static Map<String, RedisTemplate<String, String>> REDIS_TEMPLATE_REGISTER = new ConcurrentHashMap<>();

    /**
     * 多数据源redisHelper注册（不包括默认数据源）
     */
    private final static Map<String, RedisHelper> REDIS_HELPER_REGISTER = new ConcurrentHashMap<>();

    /**
     * Redis数据源注册器构造函数
     * 用于初始化Redis数据源注册器实例
     */
    public RedisDataSourceRegister() {

    }

    /**
     * 注册RedisTemplate
     */
    public static void registerRedisTemplate(String name, RedisTemplate<String, String> redisTemplate) {
        if (redisTemplate == null || name == null) {
            return;
        }
        REDIS_TEMPLATE_REGISTER.put(name + DynamicRedisConstants.MultiSource.REDIS_TEMPLATE, redisTemplate);
    }

    /**
     * 注册RedisHelper
     */
    public static void registerRedisHelper(String name, RedisHelper redisHelper) {
        if (redisHelper == null || name == null) {
            return;
        }
        REDIS_HELPER_REGISTER.put(name + DynamicRedisConstants.MultiSource.REDIS_HELPER, redisHelper);
    }

    /**
     * 获取指定数据源的RedisTemplate
     */
    public static RedisTemplate<String, String> getRedisTemplate(String name) {
        return REDIS_TEMPLATE_REGISTER.get(name + DynamicRedisConstants.MultiSource.REDIS_TEMPLATE);
    }

    /**
     * 获取指定数据源的RedisHelper
     */
    public static RedisHelper getRedisHelper(String name) {
        return REDIS_HELPER_REGISTER.get(name + DynamicRedisConstants.MultiSource.REDIS_HELPER);
    }

    /**
     * 获取多数据源的RedisTemplate注册器
     */
    public static Map<String, RedisTemplate<String, String>> getRedisTemplateRegister() {
        return REDIS_TEMPLATE_REGISTER;
    }

    /**
     * 获取多数据源的RedisHelper注册器
     */
    public static Map<String, RedisHelper> getRedisHelperRegister() {
        return REDIS_HELPER_REGISTER;
    }

}
