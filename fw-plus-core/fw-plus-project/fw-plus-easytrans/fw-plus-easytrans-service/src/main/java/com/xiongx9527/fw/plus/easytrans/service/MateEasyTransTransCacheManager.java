package com.xiongx9527.fw.plus.easytrans.service;

import org.dromara.cache.service.RedisCacheService;
import org.dromara.cache.service.TransCacheManager;
import org.dromara.common.utils.ConverterUtils;
import org.dromara.common.utils.JsonUtils;
import com.xiongx9527.fw.plus.easytrans.annotation.TransType;
import org.dromara.trans.service.impl.RpcTransService;
import org.dromara.trans.service.impl.SimpleTransService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * MateEasyTransTransCacheManager 类
 *
 * 这是一个继承自 TransCacheManager 的服务类，用于处理传输缓存相关的业务逻辑。
 *
 * 继承关系：
 * - 父类：TransCacheManager
 *
 * 主要功能：
 * - 提供传输缓存的基础实现
 * - 可以扩展父类的功能来满足特定的业务需求
 */
public class MateEasyTransTransCacheManager extends TransCacheManager {

    /** RPC事务服务实例，用于处理分布式事务的远程调用 */
    private final MateEasyTransRpcTransService rpcTransService;

    /** 简单事务服务实例，用于处理本地事务操作 */
    private final MateEasyTransSimpleTransService simpleTransService;

    /** Redis缓存服务实例，用于数据缓存操作，泛型参数为String类型 */
    private final RedisCacheService<String> redisCacheService;


    /**
     * 事务缓存管理器构造函数
     *
     * @param rpcTransService RPC事务服务实例，用于处理远程调用相关的事务操作
     * @param simpleTransService 简单事务服务实例，用于处理本地事务操作
     * @param redisCacheService Redis缓存服务实例，用于缓存事务相关数据，泛型类型为String
     */
    public MateEasyTransTransCacheManager(MateEasyTransRpcTransService rpcTransService, MateEasyTransSimpleTransService simpleTransService, RedisCacheService<String> redisCacheService) {
        this.rpcTransService = rpcTransService;
        this.simpleTransService = simpleTransService;
        this.redisCacheService = redisCacheService;
    }

    /**
     * 清理缓存
     *
     * 该方法旨在清理三种不同服务（simpleTransService, rpcTransService, redisCacheService）中的缓存信息
     * 它根据目标类和主键来定位需要清理的缓存对象
     *
     * @param targetClass 目标类，表示需要清理缓存的对象类型
     * @param pkey        主键，用于标识特定缓存对象的键
     */
    @Override
    public void clearCache(Class targetClass, Object pkey) {
        // 清理simpleTransService中的缓存
        if (Objects.nonNull(simpleTransService)) {
            simpleTransService.clearGlobalCache(pkey, targetClass.getName(), TransType.SIMPLE);
        }
        // 清理rpcTransService中的缓存
        if (Objects.nonNull(rpcTransService)) {
            rpcTransService.clearGlobalCache(pkey, targetClass.getName(), TransType.RPC);
        }
        // 通过redisCacheService发送消息来清理缓存
        if (Objects.nonNull(redisCacheService)) {
            Map<String, String> body = new HashMap<>();
            body.put("messageType", "clear");
            body.put("target", targetClass.getName());
            body.put("pkey", ConverterUtils.toString(pkey));
            // 发送消息清理simple类型的缓存
            body.put("transType", TransType.SIMPLE);
            redisCacheService.convertAndSend("trans", JsonUtils.map2json(body));
            // 发送消息清理RPC类型的缓存
            body.put("transType", TransType.RPC);
            redisCacheService.convertAndSend("trans", JsonUtils.map2json(body));
        }
    }

    /**
     * 配置RPC 翻译的缓存
     *
     * @param targetClassName 目标类
     * @param cacheSett       缓存配置
     */
    @Override
    public void setRpcTransCache(String targetClassName, SimpleTransService.TransCacheSett cacheSett) {
        rpcTransService.setTransCache(targetClassName, cacheSett);
    }

    /**
     * 配置唯一键缓存
     *
     * @param targetClassName 目标类名
     * @param uniqueField     唯一键字段
     */
    @Override
    public void setUniqueFieldCache(String targetClassName, String uniqueField) {
        simpleTransService.setUniqueFieldCache(targetClassName, uniqueField);
    }
}
