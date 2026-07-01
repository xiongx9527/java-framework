package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.common;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo;

public interface BaseService extends MagicBaseService {
    /**
     * 验证S3数据源配置信息的必填字段
     *
     * @param entity S3数据源配置信息实体
     * @throws IllegalArgumentException 当任何必填字段为空时抛出此异常
     */
    default void validateField(S3DataSourceInfo entity) {
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getKey(), "key");
        validateRequiredField(entity.getPlatform(), "platform");
        validateRequiredField(entity.getAccessKey(), "accessKey");
        validateRequiredField(entity.getSecretKey(), "secretKey");
        validateRequiredField(entity.getEndPoint(), "endPoint");
        validateRequiredField(entity.getBucketName(), "bucketName");
    }


}
