package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.client;

import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.helper.RedisHelper;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.options.AbstractOptionsRedisDb;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.register.RedisDataSourceRegister;
import com.xiongx9527.fw.plus.boot.common.CommonAutoConfiguration;
import com.xiongx9527.fw.plus.nosql.redis.constant.DynamicRedisConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import static com.xiongx9527.fw.plus.nosql.redis.constant.DynamicRedisConstants.MultiSource.DEFAULT_SOURCE;

/**
 * Redis多数据源操作客户端
 *
 * @author Mr_wenpan@163.com 2021/09/06 11:10
 */
@Configuration
public class RedisMultiSourceClient extends CommonAutoConfiguration {

    /**
     * 操作指定数据源的默认db
     *
     * @param datasource 数据源名称
     */
    public RedisTemplate<String, String> opsDefaultDb(String datasource) {
        // 获取该数据源的默认db对应的redisTemplate
        RedisTemplate<String, String> redisTemplate = RedisDataSourceRegister.getRedisTemplate(
                datasource + DynamicRedisConstants.MultiSource.REDIS_TEMPLATE);
        if (null == redisTemplate) {
            throw new RuntimeException("没有该数据源，请确认传入的redis数据源名称是否正确.");
        }
        return redisTemplate;
    }

    /**
     * 使用默认数据源，操作0号数据库
     *
     * @return RedisTemplate<String, String> 0号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbZeroWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbZero();
    }

    /**
     * 使用默认数据源，操作1号数据库
     *
     * @return RedisTemplate<String, String> 1号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbOneWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbOne();
    }

    /**
     * 使用默认数据源，操作2号数据库
     *
     * @return RedisTemplate<String, String> 2号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbTwoWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbTwo();
    }

    /**
     * 使用默认数据源，操作3号数据库
     *
     * @return RedisTemplate<String, String> 3号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbThreeWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbThree();
    }

    /**
     * 使用默认数据源，操作4号数据库
     *
     * @return RedisTemplate<String, String> 4号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbFourWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbFour();
    }

    /**
     * 使用默认数据源，操作5号数据库
     *
     * @return RedisTemplate<String, String> 5号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbFiveWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbFive();
    }

    /**
     * 使用默认数据源，操作6号数据库
     *
     * @return RedisTemplate<String, String> 6号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbSixWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbSix();
    }

    /**
     * 使用默认数据源，操作7号数据库
     *
     * @return RedisTemplate<String, String> 7号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbSevenWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbSeven();
    }

    /**
     * 使用默认数据源，操作8号数据库
     *
     * @return RedisTemplate<String, String> 8号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbEightWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbEight();
    }

    /**
     * 使用默认数据源，操作9号数据库
     *
     * @return RedisTemplate<String, String> 9号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbNineWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbNine();
    }

    /**
     * 使用默认数据源，操作10号数据库
     *
     * @return RedisTemplate<String, String> 10号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbTenWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbTen();
    }

    /**
     * 使用默认数据源，操作11号数据库
     *
     * @return RedisTemplate<String, String> 11号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbElevenWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbEleven();
    }

    /**
     * 使用默认数据源，操作12号数据库
     *
     * @return RedisTemplate<String, String> 12号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbTwelveWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbTwelve();
    }

    /**
     * 使用默认数据源，操作13号数据库
     *
     * @return RedisTemplate<String, String> 13号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbThirteenWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbThirteen();
    }

    /**
     * 使用默认数据源，操作14号数据库
     *
     * @return RedisTemplate<String, String> 14号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbFourteenWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbFourteen();
    }

    /**
     * 使用默认数据源，操作15号数据库
     *
     * @return RedisTemplate<String, String> 15号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbFifteenWithDefaultSource() {
        return commonOpsDb(DEFAULT_SOURCE).opsDbFifteen();
    }

    /**
     * 使用默认数据源，操作0~15号库以外的db
     */
    public RedisTemplate<String, String> opsOtherDbWithDefaultSource(int db) {
        return commonOpsDb(DEFAULT_SOURCE).opsOtherDb(db);
    }

    /**
     * 操作指定数据源的0号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 0号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbZero(String datasource) {
        return commonOpsDb(datasource).opsDbZero();
    }

    /**
     * 操作指定数据源的1号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 1号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbOne(String datasource) {
        return commonOpsDb(datasource).opsDbOne();
    }

    /**
     * 操作指定数据源的2号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 2号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbTwo(String datasource) {
        return commonOpsDb(datasource).opsDbTwo();
    }

    /**
     * 操作指定数据源的3号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 3号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbThree(String datasource) {
        return commonOpsDb(datasource).opsDbThree();
    }

    /**
     * 操作指定数据源的4号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 4号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbFour(String datasource) {
        return commonOpsDb(datasource).opsDbFour();
    }

    /**
     * 操作指定数据源的5号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 5号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbFive(String datasource) {
        return commonOpsDb(datasource).opsDbFive();
    }

    /**
     * 操作指定数据源的6号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 6号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbSix(String datasource) {
        return commonOpsDb(datasource).opsDbSix();
    }

    /**
     * 操作指定数据源的7号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 7号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbSeven(String datasource) {
        return commonOpsDb(datasource).opsDbSeven();
    }

    /**
     * 操作指定数据源的8号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 8号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbEight(String datasource) {
        return commonOpsDb(datasource).opsDbEight();
    }

    /**
     * 操作指定数据源的9号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 9号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbNine(String datasource) {
        return commonOpsDb(datasource).opsDbNine();
    }

    /**
     * 操作指定数据源的10号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 10号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbTen(String datasource) {
        return commonOpsDb(datasource).opsDbTen();
    }

    /**
     * 操作指定数据源的11号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 11号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbEleven(String datasource) {
        return commonOpsDb(datasource).opsDbEleven();
    }

    /**
     * 操作指定数据源的12号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 12号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbTwelve(String datasource) {
        return commonOpsDb(datasource).opsDbTwelve();
    }

    /**
     * 操作指定数据源的13号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 13号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbThirteen(String datasource) {
        return commonOpsDb(datasource).opsDbThirteen();
    }

    /**
     * 操作指定数据源的14号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 14号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbFourteen(String datasource) {
        return commonOpsDb(datasource).opsDbFourteen();
    }

    /**
     * 操作指定数据源的15号数据库
     *
     * @param datasource 数据源名称
     * @return RedisTemplate<String, String> 15号数据库对应的RedisTemplate实例
     */
    public RedisTemplate<String, String> opsDbFifteen(String datasource) {
        return commonOpsDb(datasource).opsDbFifteen();
    }

    /**
     * 操作0~15号库以外的db
     */
    public RedisTemplate<String, String> opsOtherDb(String datasource, int db) {
        return commonOpsDb(datasource).opsOtherDb(db);
    }

    private AbstractOptionsRedisDb<String, String> commonOpsDb(String datasource) {
        // 获取该数据源对应的redisHelper
        RedisHelper redisHelper = RedisDataSourceRegister.getRedisHelper(datasource + DynamicRedisConstants.MultiSource.REDIS_HELPER);
        if (redisHelper == null) {
            throw new RuntimeException("没有该数据源，请确认传入的redis数据源名称是否正确.");
        }
        // 获取指定db的redisTemplate
        return redisHelper.opsDb();
    }

}
