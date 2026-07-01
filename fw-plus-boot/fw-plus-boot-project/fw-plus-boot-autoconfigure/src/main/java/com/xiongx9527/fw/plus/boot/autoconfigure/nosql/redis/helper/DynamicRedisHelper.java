package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.helper;

import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.template.DynamicRedisTemplate;
import org.springframework.data.redis.core.*;

import java.util.Map;

/**
 * Redis操作工具类，集成封装一些常用方法，支持动态切换DB
 *
 * @author Mr_wenpan@163.com 2021/8/7 2:54 下午
 */
public class DynamicRedisHelper extends RedisHelper {

    /**
     * 动态redisTemplate
     */
    private final DynamicRedisTemplate<String, String> redisTemplate;

    /**
     * 构造函数，初始化DynamicRedisHelper实例
     *
     * @param redisTemplate 动态Redis模板对象，用于执行Redis操作
     */
    public DynamicRedisHelper(DynamicRedisTemplate<String, String> redisTemplate) {
        super(redisTemplate);
        this.redisTemplate = redisTemplate;
    }

    /**
     * <p>
     * 获取RedisTemplate对象
     * </p>
     * redisTemplate
     */
    @Override
    public RedisTemplate<String, String> getRedisTemplate() {
        return redisTemplate;
    }

    /**
     * 获取该redis数据源对应的多个RedisTemplate
     */
    @Override
    public Map<Object, RedisTemplate<String, String>> getRedisTemplates() {
        return redisTemplate.getRedisTemplates();
    }

    /**
     * 更改当前线程 RedisTemplate database
     *
     * @param database set current redis database
     */
    @Override
    public void setCurrentDatabase(int database) {
        RedisDbThreadLocalHelper.set(database);
    }

    /**
     * 清除当前数据库连接的本地线程变量
     * 用于重置Redis数据库连接上下文，确保后续操作使用正确的数据库实例
     */
    @Override
    public void clearCurrentDatabase() {
        RedisDbThreadLocalHelper.clear();
    }

    /**
     * 获取Redis值操作对象
     * 提供对Redis中String类型数据的基本操作功能
     *
     * @return ValueOperations<String, String> 值操作接口实例
     */
    @Override
    protected ValueOperations<String, String> getValueOperations() {
        return getRedisTemplate().opsForValue();
    }

    /**
     * 获取Redis哈希操作对象
     * 提供对Redis中Hash类型数据的操作功能
     *
     * @return HashOperations<String, String, String> 哈希操作接口实例
     */
    @Override
    protected HashOperations<String, String, String> getHashOperations() {
        return getRedisTemplate().opsForHash();
    }

    /**
     * 获取Redis列表操作对象
     * 提供对Redis中List类型数据的操作功能
     *
     * @return ListOperations<String, String> 列表操作接口实例
     */
    @Override
    protected ListOperations<String, String> getListOperations() {
        return getRedisTemplate().opsForList();
    }

    /**
     * 获取Redis集合操作对象
     * 提供对Redis中Set类型数据的操作功能
     *
     * @return SetOperations<String, String> 集合操作接口实例
     */
    @Override
    protected SetOperations<String, String> getSetOperations() {
        return getRedisTemplate().opsForSet();
    }

    /**
     * 获取Redis有序集合操作对象
     * 提供对Redis中ZSet类型数据的操作功能
     *
     * @return ZSetOperations<String, String> 有序集合操作接口实例
     */
    @Override
    protected ZSetOperations<String, String> getZSetOperations() {
        return getRedisTemplate().opsForZSet();
    }

}
