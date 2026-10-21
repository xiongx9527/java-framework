package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.options;

import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.helper.RedisHelper;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.template.DynamicRedisTemplate;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

/**
 * 操作redis db默认实现
 *
 * @author Mr_wenpan@163.com 2021/09/06 13:58
 */
public class DefaultOptionsRedisDb extends AbstractOptionsRedisDb<String, String> {

    /**
     * 构造函数，创建DefaultOptionsRedisDb实例
     *
     * @param redisHelper Redis助手对象，用于提供Redis操作功能
     */
    public DefaultOptionsRedisDb(RedisHelper redisHelper) {
        super(redisHelper);
    }

    /**
     * 获取操作Redis数据库0的模板
     * @return RedisTemplate<String, String> 操作数据库0的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbZero() {
        return commonOpsDb(0);
    }

    /**
     * 获取操作Redis数据库1的模板
     * @return RedisTemplate<String, String> 操作数据库1的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbOne() {
        return commonOpsDb(1);
    }

    /**
     * 获取操作Redis数据库2的模板
     * @return RedisTemplate<String, String> 操作数据库2的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbTwo() {
        return commonOpsDb(2);
    }

    /**
     * 获取操作Redis数据库3的模板
     * @return RedisTemplate<String, String> 操作数据库3的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbThree() {
        return commonOpsDb(3);
    }

    /**
     * 获取操作Redis数据库4的模板
     * @return RedisTemplate<String, String> 操作数据库4的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbFour() {
        return commonOpsDb(4);
    }

    /**
     * 获取操作Redis数据库5的模板
     * @return RedisTemplate<String, String> 操作数据库5的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbFive() {
        return commonOpsDb(5);
    }

    /**
     * 获取操作Redis数据库6的模板
     * @return RedisTemplate<String, String> 操作数据库6的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbSix() {
        return commonOpsDb(6);
    }

    /**
     * 获取操作Redis数据库7的模板
     * @return RedisTemplate<String, String> 操作数据库7的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbSeven() {
        return commonOpsDb(7);
    }

    /**
     * 获取操作Redis数据库8的模板
     * @return RedisTemplate<String, String> 操作数据库8的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbEight() {
        return commonOpsDb(8);
    }

    /**
     * 获取操作Redis数据库9的模板
     * @return RedisTemplate<String, String> 操作数据库9的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbNine() {
        return commonOpsDb(9);
    }

    /**
     * 获取操作Redis数据库10的模板
     * @return RedisTemplate<String, String> 操作数据库10的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbTen() {
        return commonOpsDb(10);
    }

    /**
     * 获取操作Redis数据库11的模板
     * @return RedisTemplate<String, String> 操作数据库11的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbEleven() {
        return commonOpsDb(11);
    }

    /**
     * 获取操作Redis数据库12的模板
     * @return RedisTemplate<String, String> 操作数据库12的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbTwelve() {
        return commonOpsDb(12);
    }

    /**
     * 获取操作Redis数据库13的模板
     * @return RedisTemplate<String, String> 操作数据库13的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbThirteen() {
        return commonOpsDb(13);
    }

    /**
     * 获取操作Redis数据库14的模板
     * @return RedisTemplate<String, String> 操作数据库14的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbFourteen() {
        return commonOpsDb(14);
    }

    /**
     * 获取操作Redis数据库15的模板
     * @return RedisTemplate<String, String> 操作数据库15的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsDbFifteen() {
        return commonOpsDb(15);
    }

    /**
     * 获取操作指定Redis数据库的模板
     * @param db 数据库索引
     * @return RedisTemplate<String, String> 操作指定数据库的Redis模板
     */
    @Override
    public RedisTemplate<String, String> opsOtherDb(int db) {
        return commonOpsDb(db);
    }

    /**
     * 操作db公用方法
     */
    private RedisTemplate<String, String> commonOpsDb(int db) {
        // 获取到该redisHelper对应的Redis数据源操作的redisTemplates
        Map<Object, RedisTemplate<String, String>> redisTemplates = redisHelper.getRedisTemplates();
        // 静态redisHelper不能切换db
        if (redisTemplates == null) {
            throw new RuntimeException(I18nMessageResolver.resolveKey("fw.boot.redis.staticHelperNoDynamicDb"));
        }

        // 获取到该RedisHelper的redisTemplate(一定有，在创建redisHelper的时候就赋值了)
        DynamicRedisTemplate<String, String> dynamicRedisTemplate = (DynamicRedisTemplate<String, String>) redisHelper.getRedisTemplate();
        RedisTemplate<String, String> redisTemplate = redisTemplates.get(db);

        // 双重检查，这里直接使用synchronized锁，因为创建redisTemplate不会很频繁，一般整个生命周期只有几次，不会有性能问题
        if (null == redisTemplate) {
            synchronized (DynamicRedisTemplate.class) {
                if (null == redisTemplates.get(db)) {
                    // 创建到该db的RedisTemplate并缓存起来
                    RedisTemplate<String, String> redisTemplateOnMissing = dynamicRedisTemplate.createRedisTemplateOnMissing(db);
                    redisTemplates.put(db, redisTemplateOnMissing);
                }
            }
        }

        return redisTemplates.get(db);
    }

}
