package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.starter.MagicAPIDtpConfig;
import com.xiongx9527.fw.plus.common.util.SpringUtils;

import java.util.Map;

public interface BaseService extends MagicBaseService {

    /**
     * 验证数据源信息实体的字段有效性
     * @param entity 数据源信息实体对象，包含需要验证的各个字段
     */
    default void validateField(DtpDataSourceInfo entity) {
        // 验证必填字段：确保名称、密钥、主机和协议都不为空
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getKey(), "key");

        // 验证其它配置是否为有效的JSON格式
        // 如果配置内容不为空，则进行JSON格式校验
        validateJSON(entity.getOther(),"其它配置");
    }

    /**
     * 获取动态线程池执行器配置
     *
     * @return 返回MagicAPIDtpConfig配置对象，包含线程池的相关配置信息
     */
    default Map<String,Object> getDtpExecutorConfig() {
        return SpringUtils.getBean(MagicAPIDtpConfig.class);
    }

    /**
     * 获取核心线程池大小配置值
     *
     * @return 返回核心线程池大小，如果配置不存在则返回默认值5
     */
    default int getCorePoolSize() {
        return getConfigValueAsInt(getDtpExecutorConfig(), "corePoolSize",5) ;
    }

    /**
     * 获取线程池最大线程数配置值
     *
     * @return 返回线程池最大线程数，如果配置不存在则返回默认值15
     */
    default int getMaximumPoolSize() {
        return getConfigValueAsInt(getDtpExecutorConfig(), "maximumPoolSize",15) ;
    }

    /**
     * 获取线程池保持存活的时间配置值
     *
     * @return 返回线程池中空闲线程的保持存活时间，单位为秒。如果配置不存在或解析失败，则返回默认值0
     */
    default int getKeepAliveTime() {
        return getConfigValueAsInt(getDtpExecutorConfig(), "keepAliveTime",0) ;
    }

    /**
     * 获取队列容量配置值
     *
     * @return 队列容量，默认值为20
     */
    default int getQueueCapacity() {
        return getConfigValueAsInt(getDtpExecutorConfig(), "queueCapacity",20) ;
    }



}
